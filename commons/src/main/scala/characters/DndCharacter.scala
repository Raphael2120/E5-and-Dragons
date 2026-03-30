package characters

final case class DndCharacter(
  dndRace:   DndRace,
  dndClass:  DndClass,
  shout:     String,
  armorClass: Int,
  hp:        Int,
  gold:      Int,
  bonusAtk:  Int = 0,
  bonusDef:  Int = 0
)
