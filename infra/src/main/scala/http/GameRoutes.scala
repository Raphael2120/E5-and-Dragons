package http

import actions.{CardinalDirection, NextAction}
import cats.effect.IO
import errors.Death
import io.circe.Json
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.typelevel.ci.CIStringSyntax

import Codecs.given

class GameRoutes(sessions: GameSessionService):

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    // ── Health check ────────────────────────────────────────────────────────
    case GET -> Root / "health" =>
      Ok(Json.obj("status" -> Json.fromString("ok")))

    // ── New game ────────────────────────────────────────────────────────────
    case POST -> Root / "api" / "game" / "new" =>
      sessions.create().flatMap: id =>
        sessions.get(id).flatMap:
          case None          => InternalServerError("Session not found")
          case Some(session) =>
            val state = session.currentState
            Ok(Json.obj(
              "sessionId"      -> Json.fromString(id),
              "state"          -> state.asJson,
              "welcomeMessage" -> Json.fromString(
                s"🏰 Bienvenue dans E5 & Dragons ! Carte ${state.width}x${state.height} | ⚔️ Ennemis: ${state.villains.size} | ⭐ Or: ${state.goldPieces.size} tas"
              )
            ))

    // ── Reset (play again) ──────────────────────────────────────────────────
    case req @ POST -> Root / "api" / "game" / "reset" =>
      withSession(req): session =>
        val id = req.headers.get(ci"X-Session-Id").map(_.head.value).getOrElse("")
        sessions.delete(id) >> sessions.create().flatMap: newId =>
          sessions.get(newId).flatMap:
            case None     => InternalServerError("Session not found")
            case Some(s)  =>
              val state = s.currentState
              Ok(Json.obj(
                "sessionId"      -> Json.fromString(newId),
                "state"          -> state.asJson,
                "welcomeMessage" -> Json.fromString("🔄 Nouvelle partie ! Bonne chance !")
              ))

    // ── Get state ───────────────────────────────────────────────────────────
    case req @ GET -> Root / "api" / "game" / "state" =>
      withSession(req): session =>
        Ok(Json.obj("state" -> session.currentState.asJson))

    // ── Move ────────────────────────────────────────────────────────────────
    case req @ POST -> Root / "api" / "game" / "move" =>
      withSession(req): session =>
        req.as[Json].flatMap: body =>
          val dirStr = body.hcursor.downField("direction").as[CardinalDirection]
          dirStr match
            case Left(_)    => BadRequest("Invalid direction. Use NORTH, SOUTH, EAST, WEST")
            case Right(dir) =>
              session.mutex.lock.surround(IO.blocking {
                val action = session.movementEngine.move(dir)
                val state  = session.currentState
                val logs   = action match
                  case NextAction.LOOT =>
                    val s        = session.movementEngine.getCurrentState
                    val healedHp = (s.player.hp + 10).min(session.maxHp)
                    val healed   = s.copy(player = s.player.copy(hp = healedHp))
                    session.movementEngine.updateState(healed)
                    session.dataStorage.saveMapState(healed)
                    session.rendering.renderMapState(healed)
                    List(s"⭐ Or ramassé ! Total : ${healed.player.gold}  |  ❤️ Soin : HP +10 ($healedHp/${session.maxHp})")
                  case NextAction.TALK => List("❓ Un PNJ vous observe en silence...")
                  case _               => List.empty

                Json.obj(
                  "state"      -> session.currentState.asJson,
                  "nextAction" -> Json.fromString(action.toString),
                  "logs"       -> Json.fromValues(logs.map(Json.fromString)),
                  "fightResult"-> Json.Null
                )
              }).flatMap(Ok(_))

    // ── Fight ────────────────────────────────────────────────────────────────
    case req @ POST -> Root / "api" / "game" / "fight" =>
      withSession(req): session =>
        session.mutex.lock.surround(IO.blocking {
          val state = session.movementEngine.getCurrentState
          state.villains.get(state.playerPos) match
            case None => Json.obj(
              "state"       -> state.asJson,
              "nextAction"  -> Json.fromString("MOVE"),
              "logs"        -> Json.fromValues(List(Json.fromString("Pas d'ennemi ici."))),
              "fightResult" -> Json.Null
            )
            case Some(villain) =>
              session.rendering.clearFightState()
              session.fightingEngine.fight(state.player, villain) match
                case Left(_: Death) =>
                  val fs = session.rendering.lastFightState
                  Json.obj(
                    "state"      -> state.asJson,
                    "nextAction" -> Json.fromString("DEAD"),
                    "logs"       -> Json.fromValues(List(Json.fromString("💀 Vous êtes mort. Game Over."))),
                    "fightResult"-> Json.obj(
                      "won"          -> Json.fromBoolean(false),
                      "combatLog"    -> Json.fromValues(fs.map(_.log).getOrElse(List.empty).map(Json.fromString)),
                      "finalPlayerHp"-> Json.fromInt(0),
                      "finalVillainHp"-> Json.fromInt(fs.map(_.villainHP).getOrElse(0))
                    )
                  )

                case Right(updatedPlayer) =>
                  val fs         = session.rendering.lastFightState
                  val healedHp   = (updatedPlayer.hp + 15).min(session.maxHp)
                  val afterFight = state.copy(
                    player   = updatedPlayer.copy(hp = healedHp),
                    villains = state.villains - state.playerPos
                  )
                  session.movementEngine.updateState(afterFight)
                  session.dataStorage.saveMapState(afterFight)
                  session.rendering.renderMapState(afterFight)
                  Json.obj(
                    "state"      -> afterFight.asJson,
                    "nextAction" -> Json.fromString("FIGHT_WON"),
                    "logs"       -> Json.fromValues(List(
                      Json.fromString(s"⚔️ Victoire ! Or ramassé : ${villain.gold}  |  ❤️ Repos : HP +15 ($healedHp/${session.maxHp})")
                    )),
                    "fightResult"-> Json.obj(
                      "won"           -> Json.fromBoolean(true),
                      "combatLog"     -> Json.fromValues(fs.map(_.log).getOrElse(List.empty).map(Json.fromString)),
                      "finalPlayerHp" -> Json.fromInt(healedHp),
                      "finalVillainHp"-> Json.fromInt(0)
                    )
                  )
        }).flatMap(Ok(_))
  }

  // ── Helpers ────────────────────────────────────────────────────────────────
  private def sessionId(req: Request[IO]): Option[String] =
    req.headers.get(ci"X-Session-Id").map(_.head.value)

  private def withSession(req: Request[IO])(f: GameSession => IO[Response[IO]]): IO[Response[IO]] =
    sessionId(req) match
      case None     => BadRequest("Missing X-Session-Id header")
      case Some(id) =>
        sessions.get(id).flatMap:
          case None          => NotFound(s"Session $id not found. Start a new game first.")
          case Some(session) => f(session)
