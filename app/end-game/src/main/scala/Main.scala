import actions.NextAction
import data.MutableCollectionDataStorageAdapter
import domain.{FightingEngine, MapManager, MovementEngine}
import errors.Death
import randomness.MachineDefaultRandomnessAdapter
import rendering.{GameUI, SwingRenderingAdapter}

import scala.io.Source
import scala.swing.SimpleSwingApplication

object Main extends SimpleSwingApplication:
  private val gameUI    = GameUI()
  private val rendering = SwingRenderingAdapter(gameUI)

  def top =
    startGame()
    gameUI

  private def loadMapLines(): List[String] =
    Option(Thread.currentThread().getContextClassLoader.getResourceAsStream("e5-dungeon.dndmap"))
      .map(Source.fromInputStream(_).getLines().toList)
      .getOrElse:
        System.err.println("Cannot find e5-dungeon.dndmap on classpath")
        sys.exit(1)

  private def startGame(): Unit =
    // ── Infrastructure adapters (fresh state each game) ─────────────────────
    val randomness  = MachineDefaultRandomnessAdapter()
    val dataStorage = MutableCollectionDataStorageAdapter()

    // ── Parse map ────────────────────────────────────────────────────────────
    val mapManager = MapManager(dataStorage)
    mapManager.validateAndStoreMap(loadMapLines()) match
      case Left(err) =>
        System.err.println(s"Map error: ${err.getMessage}")
        sys.exit(1)
      case Right(_) => ()

    val initialState = dataStorage.getMapState.getOrElse:
      System.err.println("Map state not found after parsing")
      sys.exit(1)

    // ── Domain engines ───────────────────────────────────────────────────────
    val maxHp          = initialState.player.hp
    val movementEngine = MovementEngine(initialState, dataStorage, rendering)
    val fightingEngine = FightingEngine(randomness, rendering, dataStorage)

    // ── Combat helper ────────────────────────────────────────────────────────
    def runCombat(villain: model.DndMapState => Option[characters.DndCharacter]): Unit =
      val state = movementEngine.getCurrentState
      villain(state).foreach: v =>
        rendering.clearFightState()
        fightingEngine.fight(state.player, v) match
          case Left(_: Death) =>
            rendering.lastFightState.foreach: fs =>
              gameUI.showCombatDialog(fs.log, won = false, fs.playerHP, fs.villainHP)
            gameUI.appendLog("\u2620 Vous etes mort. Game Over.")
            if gameUI.askPlayAgain() then
              javax.swing.SwingUtilities.invokeLater(() => startGame())
            else
              sys.exit(0)

          case Right(updatedPlayer) =>
            rendering.lastFightState.foreach: fs =>
              gameUI.showCombatDialog(fs.log, won = true, fs.playerHP, fs.villainHP)
            val healedHp    = (updatedPlayer.hp + 15).min(maxHp)
            val healedPlayer = updatedPlayer.copy(hp = healedHp)
            val afterFight = state.copy(
              player   = healedPlayer,
              villains = state.villains - state.playerPos
            )
            movementEngine.updateState(afterFight)
            dataStorage.saveMapState(afterFight)
            rendering.renderMapState(afterFight)
            gameUI.appendLog(s"\u2694 Victoire ! Or ramasse : ${v.gold}  |  \u2665 Repos : HP +15 ($healedHp/$maxHp)")

    // ── Callbacks ────────────────────────────────────────────────────────────
    gameUI.onMove = dir =>
      movementEngine.move(dir) match
        case NextAction.FIGHT => runCombat(s => s.villains.get(s.playerPos))
        case NextAction.LOOT  =>
          val s        = movementEngine.getCurrentState
          val healedHp = (s.player.hp + 10).min(maxHp)
          val healed   = s.copy(player = s.player.copy(hp = healedHp))
          movementEngine.updateState(healed)
          dataStorage.saveMapState(healed)
          rendering.renderMapState(healed)
          gameUI.appendLog(s"\u2605 Or ramasse ! Total : ${healed.player.gold}  |  \u2665 Soin : HP +10 ($healedHp/$maxHp)")
        case NextAction.TALK  =>
          gameUI.appendLog("? Un PNJ vous observe en silence...")
        case NextAction.MOVE  => ()

    gameUI.onFight = () =>
      val state = movementEngine.getCurrentState
      if state.villains.contains(state.playerPos) then
        runCombat(s => s.villains.get(s.playerPos))
      else
        gameUI.appendLog("Pas d'ennemi ici.")

    // ── Initial render ───────────────────────────────────────────────────────
    gameUI.resetForNewGame()
    rendering.renderMapState(initialState)
    gameUI.appendLog(s"Bienvenue dans E5 & Dragons !")
    gameUI.appendLog(s"Carte ${initialState.width}x${initialState.height} | Ennemis: ${initialState.villains.size} | Or: ${initialState.goldPieces.size} tas")
    gameUI.appendLog("Touches : fleches ou WASD = deplacer  |  F = combat")
