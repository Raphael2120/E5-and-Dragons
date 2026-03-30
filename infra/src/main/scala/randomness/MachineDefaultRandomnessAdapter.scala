package randomness

import out.RandomnessPortOut
import rolls.Die

class MachineDefaultRandomnessAdapter extends RandomnessPortOut:
  private val rng = scala.util.Random()

  override def getRandom(die: Die): Int =
    val sides = die match
      case Die.D20 => 20
      case Die.D6  => 6
    rng.between(1, sides + 1)
