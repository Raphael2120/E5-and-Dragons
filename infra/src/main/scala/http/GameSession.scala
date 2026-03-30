package http

import cats.effect.{IO, Ref}
import cats.effect.std.Mutex
import characters.DndCharacter
import data.MutableCollectionDataStorageAdapter
import domain.{FightingEngine, MapManager, MovementEngine}
import errors.Death
import model.DndMapState
import randomness.MachineDefaultRandomnessAdapter
import rendering.RefRenderingAdapter

import scala.io.Source

/** All state belonging to a single game session. Access must be serialized via mutex. */
case class GameSession(
  mutex:          Mutex[IO],
  rendering:      RefRenderingAdapter,
  dataStorage:    MutableCollectionDataStorageAdapter,
  movementEngine: MovementEngine,
  fightingEngine: FightingEngine,
  maxHp:          Int
):
  def currentState: DndMapState =
    movementEngine.getCurrentState

object GameSession:
  private def loadMapLines(): List[String] =
    Option(Thread.currentThread().getContextClassLoader.getResourceAsStream("e5-dungeon.dndmap"))
      .map(Source.fromInputStream(_).getLines().toList)
      .getOrElse(throw RuntimeException("Cannot find e5-dungeon.dndmap on classpath"))

  def create(): IO[GameSession] =
    for
      mutex <- Mutex[IO]
      session <- IO.blocking {
        val rendering   = RefRenderingAdapter()
        val dataStorage = MutableCollectionDataStorageAdapter()
        val randomness  = MachineDefaultRandomnessAdapter()

        val mapManager = MapManager(dataStorage)
        mapManager.validateAndStoreMap(loadMapLines()) match
          case Left(err) => throw RuntimeException(s"Map error: ${err.getMessage}")
          case Right(_)  => ()

        val initialState = dataStorage.getMapState
          .getOrElse(throw RuntimeException("Map state not found after parsing"))

        val movementEngine = MovementEngine(initialState, dataStorage, rendering)
        val fightingEngine = FightingEngine(randomness, rendering, dataStorage)

        GameSession(mutex, rendering, dataStorage, movementEngine, fightingEngine, initialState.player.hp)
      }
    yield session
