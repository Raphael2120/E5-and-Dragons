package model

import actions.CardinalDirection
import characters.DndCharacter
import items.ItemType

case class DndMapState(
  width:             Int,
  height:            Int,
  playerPos:         (Int, Int),
  playerOrientation: CardinalDirection,
  player:            DndCharacter,
  villains:          Map[(Int, Int), DndCharacter],
  npcPositions:      List[(Int, Int)],
  goldPieces:        Map[(Int, Int), Int],
  itemPositions:     Map[(Int, Int), ItemType]
)
