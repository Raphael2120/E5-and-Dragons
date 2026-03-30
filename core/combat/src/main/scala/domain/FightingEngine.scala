package domain

import characters.DndCharacter
import errors.Death
import in.{ForFighting, NewCharacterState}
import model.FightState
import out.{CombatDataPortOut, FightRenderingPortOut, RandomnessPortOut}
import rolls.Die

class FightingEngine(
  randomnessPortOut: RandomnessPortOut,
  renderingPortOut: FightRenderingPortOut,
  dataPortOut: CombatDataPortOut
) extends ForFighting:

  override def fight(character: DndCharacter, villain: DndCharacter): Either[Death, NewCharacterState] =
    var playerHP  = character.hp
    var villainHP = villain.hp
    var round     = 1
    var log       = List.empty[String]

    def rollDice(die: Die, times: Int): Int =
      (1 to times).map(_ => randomnessPortOut.getRandom(die)).sum

    def doAttack(attackerName: String, attacker: DndCharacter, targetAC: Int): Int =
      val hitRoll = randomnessPortOut.getRandom(Die.D20)
      if hitRoll >= targetAC then
        val action     = attacker.dndClass.action
        val dmg        = rollDice(action.diceRoll, action.diceAmount)
        val bonusDmg   = attacker.dndClass.bonusAction
          .map(ba => rollDice(ba.diceRoll, ba.diceAmount))
          .getOrElse(0)
        val total = dmg + bonusDmg
        log = log :+ s"$attackerName hits for $total dmg (rolled $hitRoll vs AC $targetAC)"
        total
      else
        log = log :+ s"$attackerName misses! (rolled $hitRoll vs AC $targetAC)"
        0

    val playerInit  = randomnessPortOut.getRandom(Die.D20)
    val villainInit = randomnessPortOut.getRandom(Die.D20)
    val playerFirst = playerInit >= villainInit
    log = log :+ s"Initiative — You: $playerInit | Villain: $villainInit → ${if playerFirst then "You go first!" else "Villain goes first!"}"

    while playerHP > 0 && villainHP > 0 && round <= 100 do
      log = log :+ s"── Round $round ──"

      if playerFirst then
        villainHP -= doAttack("You", character, villain.armorClass)
        if villainHP > 0 then
          playerHP -= doAttack("Villain", villain, character.armorClass)
      else
        playerHP -= doAttack("Villain", villain, character.armorClass)
        if playerHP > 0 then
          villainHP -= doAttack("You", character, villain.armorClass)

      val state = FightState(character, playerHP.max(0), villain, villainHP.max(0), round, log)
      renderingPortOut.renderFightState(state)
      dataPortOut.saveCharacterState(character.copy(hp = playerHP.max(0)), villain.copy(hp = villainHP.max(0)))
      round += 1

    if playerHP <= 0 then Left(Death())
    else Right(character.copy(hp = playerHP, gold = character.gold + villain.gold))
