package rendering

import model.{DndMapState, FightState}
import out.{ExplorationRenderingPortOut, FightRenderingPortOut}

class SwingRenderingAdapter(gameUI: GameUI) extends FightRenderingPortOut, ExplorationRenderingPortOut:

  // Last fight state cached so Main can retrieve the full log after combat.
  private var _lastFightState: Option[FightState] = None

  def lastFightState: Option[FightState]  = _lastFightState
  def clearFightState(): Unit             = _lastFightState = None

  override def renderMapState(dndMap: DndMapState): Unit =
    gameUI.updateMap(dndMap)

  override def renderFightState(fightState: FightState): Unit =
    _lastFightState = Some(fightState)
    gameUI.updateFight(fightState)
