package model

import characters.DndCharacter

case class FightState(
  player: DndCharacter,
  playerHP: Int,
  villain: DndCharacter,
  villainHP: Int,
  round: Int,
  log: List[String]
)
