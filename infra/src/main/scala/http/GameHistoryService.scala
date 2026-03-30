package http

import cats.effect.{IO, Ref}
import history.GameHistoryEntry

/** Thread-safe in-memory game history store (server lifetime). */
class GameHistoryService(store: Ref[IO, List[GameHistoryEntry]]):

  def add(entry: GameHistoryEntry): IO[Unit] =
    store.update(entry :: _)

  def getAll: IO[List[GameHistoryEntry]] =
    store.get.map(_.reverse) // chronological order

object GameHistoryService:
  def create(): IO[GameHistoryService] =
    Ref.of[IO, List[GameHistoryEntry]](List.empty).map(GameHistoryService(_))
