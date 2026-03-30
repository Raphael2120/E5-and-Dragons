package rendering

import model.{DndMapState, FightState}
import out.{ExplorationRenderingPortOut, FightRenderingPortOut}

/** In-memory rendering adapter used by the HTTP server.
 *  Accumulates the last known state so routes can read it after domain calls.
 *  Access must be serialized externally (see GameSession.mutex).
 */
class RefRenderingAdapter extends FightRenderingPortOut, ExplorationRenderingPortOut:

  private var _lastMapState:   Option[DndMapState] = None
  private var _lastFightState: Option[FightState]  = None

  def lastMapState:   Option[DndMapState] = _lastMapState
  def lastFightState: Option[FightState]  = _lastFightState

  def clearFightState(): Unit = _lastFightState = None

  override def renderMapState(state: DndMapState): Unit  = _lastMapState  = Some(state)
  override def renderFightState(state: FightState): Unit = _lastFightState = Some(state)
