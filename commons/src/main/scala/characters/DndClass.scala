package characters

import actions.CombatAction
import rolls.Die.*

enum DndClass:
  case PALADIN(lvl: Int) extends DndClass   // 🧙 Sorcier  — 2D6 + bonus D6 (lvl>3)
  case WARRIOR(lvl: Int) extends DndClass   // 👹 Guerrier  — 2D8
  case ARCHER(lvl: Int)  extends DndClass   // 💀 Archère   — 1D4
  case BOSS(lvl: Int)    extends DndClass   // 🐉 Boss      — 3D10 + D6

  def action: CombatAction =
    this match
      case DndClass.PALADIN(_) => CombatAction(2, D6)
      case DndClass.WARRIOR(_) => CombatAction(2, D8)
      case DndClass.ARCHER(_)  => CombatAction(1, D4)
      case DndClass.BOSS(_)    => CombatAction(3, D10)

  def bonusAction: Option[CombatAction] =
    this match
      case DndClass.PALADIN(lvl) => if lvl > 3 then Some(CombatAction(1, D6)) else None
      case DndClass.WARRIOR(lvl) => if lvl > 5 then Some(CombatAction(1, D6)) else None
      case DndClass.ARCHER(_)    => None
      case DndClass.BOSS(_)      => Some(CombatAction(1, D6))

  def className: String = this match
    case DndClass.PALADIN(_) => "PALADIN"
    case DndClass.WARRIOR(_) => "WARRIOR"
    case DndClass.ARCHER(_)  => "ARCHER"
    case DndClass.BOSS(_)    => "BOSS"

  def level: Int = this match
    case DndClass.PALADIN(l) => l
    case DndClass.WARRIOR(l) => l
    case DndClass.ARCHER(l)  => l
    case DndClass.BOSS(l)    => l
