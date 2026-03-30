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

    // Effective ACs: bonusDef increases AC
    val playerEffectiveAC  = character.armorClass + character.bonusDef
    val villainEffectiveAC = villain.armorClass + villain.bonusDef

    def rollDice(die: Die, times: Int): Int =
      (1 to times).map(_ => randomnessPortOut.getRandom(die)).sum

    def doAttack(attackerName: String, attacker: DndCharacter, targetAC: Int, isPlayer: Boolean): Int =
      val hitRoll = randomnessPortOut.getRandom(Die.D20)
      if hitRoll >= targetAC then
        val action   = attacker.dndClass.action
        val dmg      = rollDice(action.diceRoll, action.diceAmount)
        val bonusDmg = attacker.dndClass.bonusAction
          .map(ba => rollDice(ba.diceRoll, ba.diceAmount))
          .getOrElse(0)
        // bonusAtk adds flat damage for the player only
        val atkBonus = if isPlayer then attacker.bonusAtk else 0
        val total    = dmg + bonusDmg + atkBonus
        log = log :+ s"$attackerName frappe pour $total dégâts (jet $hitRoll vs CA $targetAC)"
        total
      else
        log = log :+ s"$attackerName rate ! (jet $hitRoll vs CA $targetAC)"
        0

    val playerInit  = randomnessPortOut.getRandom(Die.D20)
    val villainInit = randomnessPortOut.getRandom(Die.D20)
    val playerFirst = playerInit >= villainInit
    log = log :+ s"Initiative — Vous: $playerInit | Ennemi: $villainInit → ${if playerFirst then "Vous commencez !" else "L'ennemi commence !"}"

    while playerHP > 0 && villainHP > 0 && round <= 100 do
      log = log :+ s"── Round $round ──"

      if playerFirst then
        villainHP -= doAttack("Vous", character, villainEffectiveAC, isPlayer = true)
        if villainHP > 0 then
          playerHP -= doAttack("Ennemi", villain, playerEffectiveAC, isPlayer = false)
      else
        playerHP -= doAttack("Ennemi", villain, playerEffectiveAC, isPlayer = false)
        if playerHP > 0 then
          villainHP -= doAttack("Vous", character, villainEffectiveAC, isPlayer = true)

      val state = FightState(character, playerHP.max(0), villain, villainHP.max(0), round, log)
      renderingPortOut.renderFightState(state)
      dataPortOut.saveCharacterState(character.copy(hp = playerHP.max(0)), villain.copy(hp = villainHP.max(0)))
      round += 1

    if playerHP <= 0 then Left(Death())
    else Right(character.copy(hp = playerHP, gold = character.gold + villain.gold))
