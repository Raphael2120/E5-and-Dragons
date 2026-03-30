package http

import actions.{CardinalDirection, NextAction}
import cats.effect.IO
import errors.Death
import history.GameHistoryEntry
import io.circe.Json
import io.circe.syntax.*
import items.ItemType
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.typelevel.ci.CIStringSyntax

import Codecs.given

class GameRoutes(sessions: GameSessionService, historyService: GameHistoryService):

  // Cost in gold for NPC healing
  private val NPC_HEAL_COST   = 10
  private val NPC_HEAL_AMOUNT = 25

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    // ── Health check ────────────────────────────────────────────────────────
    case GET -> Root / "health" =>
      Ok(Json.obj("status" -> Json.fromString("ok")))

    // ── New game ────────────────────────────────────────────────────────────
    case POST -> Root / "api" / "game" / "new" =>
      sessions.create().flatMap { id =>
        sessions.get(id).flatMap {
          case None          => InternalServerError("Session not found")
          case Some(session) =>
            val state = session.currentState
            Ok(Json.obj(
              "sessionId"      -> Json.fromString(id),
              "state"          -> state.asJson,
              "welcomeMessage" -> Json.fromString(
                s"🏰 Bienvenue dans E5 & Dragons ! Carte ${state.width}x${state.height} | ⚔️ Ennemis: ${state.villains.size} | 💰 NPC soigne ${NPC_HEAL_AMOUNT}HP pour ${NPC_HEAL_COST} 💰"
              )
            ))
        }
      }

    // ── Reset ───────────────────────────────────────────────────────────────
    case req @ POST -> Root / "api" / "game" / "reset" =>
      withSession(req) { session =>
        val id = req.headers.get(ci"X-Session-Id").map(_.head.value).getOrElse("")
        sessions.delete(id) >> sessions.create().flatMap { newId =>
          sessions.get(newId).flatMap {
            case None     => InternalServerError("Session not found")
            case Some(s)  =>
              val state = s.currentState
              Ok(Json.obj(
                "sessionId"      -> Json.fromString(newId),
                "state"          -> state.asJson,
                "welcomeMessage" -> Json.fromString("🔄 Nouvelle partie ! Bonne chance !")
              ))
          }
        }
      }

    // ── Get state ───────────────────────────────────────────────────────────
    case req @ GET -> Root / "api" / "game" / "state" =>
      withSession(req) { session =>
        Ok(Json.obj("state" -> session.currentState.asJson))
      }

    // ── Move ────────────────────────────────────────────────────────────────
    case req @ POST -> Root / "api" / "game" / "move" =>
      withSession(req) { session =>
        req.as[Json].flatMap { body =>
          val dirStr = body.hcursor.downField("direction").as[CardinalDirection]
          dirStr match {
            case Left(_)    => BadRequest("Invalid direction. Use NORTH, SOUTH, EAST, WEST")
            case Right(dir) =>
              session.mutex.lock.surround(IO.blocking {
                val action = session.movementEngine.move(dir)
                val logs = action match {
                  case NextAction.LOOT =>
                    val s        = session.movementEngine.getCurrentState
                    val healedHp = s.player.hp + 10
                    val healed   = s.copy(player = s.player.copy(hp = healedHp))
                    session.movementEngine.updateState(healed)
                    session.dataStorage.saveMapState(healed)
                    session.rendering.renderMapState(healed)
                    List(s"⭐ Or ramassé ! Total : ${healed.player.gold} 💰  |  ❤️ HP +10 (total : $healedHp)")

                  case NextAction.TALK =>
                    List(s"🧙 Le PNJ vous propose ses marchandises magiques.")
                  case NextAction.ITEM =>
                    val s       = session.movementEngine.getCurrentState
                    val itemOpt = s.itemPositions.get(s.playerPos)
                    itemOpt match {
                      case None => List.empty
                      case Some(item) =>
                        val (updatedPlayer, logMsg) = item match {
                          case ItemType.ATK_POTION =>
                            val p = s.player.copy(bonusAtk = s.player.bonusAtk + 3)
                            (p, s"⚔️ Potion d'attaque ! ATK +3 (total bonus : ${p.bonusAtk})")
                          case ItemType.DEF_POTION =>
                            val p = s.player.copy(bonusDef = s.player.bonusDef + 2)
                            (p, s"🛡️ Potion de défense ! DEF +2 (CA effective : ${s.player.armorClass + p.bonusDef})")
                        }
                        val afterItem = s.copy(
                          player        = updatedPlayer,
                          itemPositions = s.itemPositions - s.playerPos
                        )
                        session.movementEngine.updateState(afterItem)
                        session.dataStorage.saveMapState(afterItem)
                        session.rendering.renderMapState(afterItem)
                        List(logMsg)
                    }

                  case _ => List.empty
                }
                Json.obj(
                  "state"       -> session.currentState.asJson,
                  "nextAction"  -> Json.fromString(action.toString),
                  "logs"        -> Json.fromValues(logs.map(Json.fromString)),
                  "fightResult" -> Json.Null
                )
              }).flatMap(json => Ok(json))
          }
        }
      }

    // ── Buy ─────────────────────────────────────────────────────────────
    case req @ POST -> Root / "api" / "game" / "buy" =>
      withSession(req) { session =>
        req.as[Json].flatMap { body =>
          val itemOpt = body.hcursor.downField("item").as[String].toOption
          session.mutex.lock.surround(IO.blocking {
            val state = session.movementEngine.getCurrentState
            
            // Check if player is on an NPC cell
            if !state.npcPositions.contains(state.playerPos) then
              Json.obj(
                "state"       -> state.asJson,
                "nextAction"  -> Json.fromString("MOVE"),
                "logs"        -> Json.fromValues(List(Json.fromString("Il n'y a pas de vendeur ici."))),
                "fightResult" -> Json.Null
              )
            else
              itemOpt match {
                case Some("HP") =>
                  if state.player.gold >= 10 then
                    val p = state.player.copy(hp = state.player.hp + 25, gold = state.player.gold - 10)
                    val s = state.copy(player = p)
                    session.movementEngine.updateState(s)
                    session.dataStorage.saveMapState(s)
                    session.rendering.renderMapState(s)
                    Json.obj(
                      "state"       -> s.asJson,
                      "nextAction"  -> Json.fromString("TALK"),
                      "logs"        -> Json.fromValues(List(Json.fromString(s"❤️ +25 HP achetés pour 10 💰. Restant: ${p.gold} 💰"))),
                      "fightResult" -> Json.Null
                    )
                  else
                    Json.obj("state" -> state.asJson, "nextAction" -> Json.fromString("TALK"), "logs" -> Json.fromValues(List(Json.fromString("Pas assez d'or pour acheter des HP."))), "fightResult" -> Json.Null)
                
                case Some("ATK") =>
                  if state.player.gold >= 15 then
                    val p = state.player.copy(bonusAtk = state.player.bonusAtk + 1, gold = state.player.gold - 15)
                    val s = state.copy(player = p)
                    session.movementEngine.updateState(s)
                    session.dataStorage.saveMapState(s)
                    session.rendering.renderMapState(s)
                    Json.obj(
                      "state"       -> s.asJson,
                      "nextAction"  -> Json.fromString("TALK"),
                      "logs"        -> Json.fromValues(List(Json.fromString(s"⚔️ +1 ATK acheté pour 15 💰."))),
                      "fightResult" -> Json.Null
                    )
                  else
                    Json.obj("state" -> state.asJson, "nextAction" -> Json.fromString("TALK"), "logs" -> Json.fromValues(List(Json.fromString("Pas assez d'or pour acheter de l'attaque."))), "fightResult" -> Json.Null)

                case Some("DEF") =>
                  if state.player.gold >= 15 then
                    val p = state.player.copy(bonusDef = state.player.bonusDef + 1, gold = state.player.gold - 15)
                    val s = state.copy(player = p)
                    session.movementEngine.updateState(s)
                    session.dataStorage.saveMapState(s)
                    session.rendering.renderMapState(s)
                    Json.obj(
                      "state"       -> s.asJson,
                      "nextAction"  -> Json.fromString("TALK"),
                      "logs"        -> Json.fromValues(List(Json.fromString(s"🛡️ +1 DEF acheté pour 15 💰."))),
                      "fightResult" -> Json.Null
                    )
                  else
                    Json.obj("state" -> state.asJson, "nextAction" -> Json.fromString("TALK"), "logs" -> Json.fromValues(List(Json.fromString("Pas assez d'or pour acheter de la défense."))), "fightResult" -> Json.Null)

                case _ =>
                  Json.obj("state" -> state.asJson, "nextAction" -> Json.fromString("TALK"), "logs" -> Json.fromValues(List(Json.fromString("Article inconnu."))), "fightResult" -> Json.Null)
              }
          }).flatMap(json => Ok(json))
        }
      }

    // ── Fight ────────────────────────────────────────────────────────────────
    case req @ POST -> Root / "api" / "game" / "fight" =>
      withSession(req) { session =>
        session.mutex.lock.surround(IO.blocking {
          val state = session.movementEngine.getCurrentState
          state.villains.get(state.playerPos) match {
            case None =>
              (Json.obj(
                "state"       -> state.asJson,
                "nextAction"  -> Json.fromString("MOVE"),
                "logs"        -> Json.fromValues(List(Json.fromString("Pas d'ennemi ici."))),
                "fightResult" -> Json.Null
              ), None: Option[GameHistoryEntry])

            case Some(villain) =>
              session.rendering.clearFightState()
              session.fightingEngine.fight(state.player, villain) match {
                case Left(_: Death) =>
                  val fs = session.rendering.lastFightState
                  (Json.obj(
                    "state"       -> state.asJson,
                    "nextAction"  -> Json.fromString("DEAD"),
                    "logs"        -> Json.fromValues(List(Json.fromString("💀 Vous êtes mort. Game Over."))),
                    "fightResult" -> Json.obj(
                      "won"            -> Json.fromBoolean(false),
                      "combatLog"      -> Json.fromValues(fs.map(_.log).getOrElse(List.empty).map(Json.fromString)),
                      "finalPlayerHp"  -> Json.fromInt(0),
                      "finalVillainHp" -> Json.fromInt(fs.map(_.villainHP).getOrElse(0))
                    )
                  ), Some(GameHistoryEntry.now("DEFEAT", state.player.gold, 0, state.villains.size)))

                case Right(updatedPlayer) =>
                  val fs         = session.rendering.lastFightState
                  val healedHp   = updatedPlayer.hp + 15
                  val afterFight = state.copy(
                    player   = updatedPlayer.copy(hp = healedHp),
                    villains = state.villains - state.playerPos
                  )
                  session.movementEngine.updateState(afterFight)
                  session.dataStorage.saveMapState(afterFight)
                  session.rendering.renderMapState(afterFight)
                  val isVictory = afterFight.villains.isEmpty
                  val entry = if isVictory then
                    Some(GameHistoryEntry.now("VICTORY", afterFight.player.gold, healedHp, 0))
                  else None
                  val villainLabel = villain.dndRace.toString + " " + villain.dndClass.className
                  (Json.obj(
                    "state"       -> afterFight.asJson,
                    "nextAction"  -> Json.fromString(if isVictory then "VICTORY" else "FIGHT_WON"),
                    "logs"        -> Json.fromValues(List(
                      Json.fromString(s"⚔️ Victoire contre $villainLabel ! +${villain.gold} 💰  |  ❤️ Repos HP +15 (total : $healedHp)")
                    )),
                    "fightResult" -> Json.obj(
                      "won"            -> Json.fromBoolean(true),
                      "combatLog"      -> Json.fromValues(fs.map(_.log).getOrElse(List.empty).map(Json.fromString)),
                      "finalPlayerHp"  -> Json.fromInt(healedHp),
                      "finalVillainHp" -> Json.fromInt(0)
                    )
                  ), entry)
              }
          }
        }).flatMap { case (json, maybeEntry) =>
          maybeEntry.fold(IO.unit)(historyService.add) >> Ok(json)
        }
      }

    // ── History ──────────────────────────────────────────────────────────────
    case GET -> Root / "api" / "game" / "history" =>
      historyService.getAll.flatMap { entries =>
        Ok(Json.fromValues(entries.map(_.asJson)))
      }
  }

  private def sessionId(req: Request[IO]): Option[String] =
    req.headers.get(ci"X-Session-Id").map(_.head.value)

  private def withSession(req: Request[IO])(f: GameSession => IO[Response[IO]]): IO[Response[IO]] =
    sessionId(req) match {
      case None     => BadRequest("Missing X-Session-Id header")
      case Some(id) =>
        sessions.get(id).flatMap {
          case None          => NotFound(s"Session $id not found. Start a new game first.")
          case Some(session) => f(session)
        }
    }
