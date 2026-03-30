package http

import cats.effect.{IO, Ref}
import java.util.UUID

class GameSessionService(sessions: Ref[IO, Map[String, GameSession]]):

  def create(): IO[String] =
    val id = UUID.randomUUID().toString
    GameSession.create().flatMap: session =>
      sessions.update(_ + (id -> session)).as(id)

  def get(id: String): IO[Option[GameSession]] =
    sessions.get.map(_.get(id))

  def delete(id: String): IO[Unit] =
    sessions.update(_ - id)

object GameSessionService:
  def create(): IO[GameSessionService] =
    Ref.of[IO, Map[String, GameSession]](Map.empty).map(GameSessionService(_))
