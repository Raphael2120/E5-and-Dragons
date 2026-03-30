package domain

import actions.{CardinalDirection, NextAction}
import in.ForMovingCharacter
import model.DndMapState
import out.{ExplorationDataPortOut, ExplorationRenderingPortOut}

class MovementEngine(
  private var currentState: DndMapState,
  dataPortOut: ExplorationDataPortOut,
  renderingPortOut: ExplorationRenderingPortOut
) extends ForMovingCharacter:

  def getCurrentState: DndMapState = currentState

  def updateState(state: DndMapState): Unit =
    currentState = state

  override def move(cardinalDirection: CardinalDirection): NextAction =
    val (x, y) = currentState.playerPos
    val (nx, ny) = cardinalDirection match
      case CardinalDirection.NORTH => (x, y - 1)
      case CardinalDirection.SOUTH => (x, y + 1)
      case CardinalDirection.EAST  => (x + 1, y)
      case CardinalDirection.WEST  => (x - 1, y)

    if nx < 0 || nx >= currentState.width || ny < 0 || ny >= currentState.height then
      renderingPortOut.renderMapState(currentState)
      return NextAction.MOVE

    val newPos = (nx, ny)
    val movedState = currentState.copy(
      playerPos         = newPos,
      playerOrientation = cardinalDirection
    )

    val action =
      if currentState.villains.contains(newPos)    then NextAction.FIGHT
      else if currentState.npcPositions.contains(newPos) then NextAction.TALK
      else if currentState.goldPieces.contains(newPos)   then NextAction.LOOT
      else NextAction.MOVE

    val updatedState =
      if action == NextAction.LOOT then
        val amount = currentState.goldPieces(newPos)
        movedState.copy(
          goldPieces = movedState.goldPieces - newPos,
          player     = movedState.player.copy(gold = movedState.player.gold + amount)
        )
      else movedState

    currentState = updatedState
    dataPortOut.saveMapState(updatedState)
    renderingPortOut.renderMapState(updatedState)
    action
