package http

import actions.CardinalDirection
import characters.{DndCharacter, DndClass, DndRace}
import history.GameHistoryEntry
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.*
import items.ItemType
import model.{DndMapState, FightState}

object Codecs:

  // ── Primitive types ───────────────────────────────────────────────────────

  given Encoder[(Int, Int)] = Encoder.instance((x, y) =>
    Json.obj("x" -> Json.fromInt(x), "y" -> Json.fromInt(y)))

  given Encoder[CardinalDirection] =
    Encoder.encodeString.contramap(_.toString)

  given Decoder[CardinalDirection] =
    Decoder.decodeString.emap {
      case "NORTH" => Right(CardinalDirection.NORTH)
      case "SOUTH" => Right(CardinalDirection.SOUTH)
      case "EAST"  => Right(CardinalDirection.EAST)
      case "WEST"  => Right(CardinalDirection.WEST)
      case s       => Left(s"Unknown direction: $s")
    }

  given Encoder[DndRace] =
    Encoder.encodeString.contramap(_.toString)

  given Encoder[DndClass] = Encoder.instance { cls =>
    Json.obj(
      "name"  -> Json.fromString(cls.className),
      "level" -> Json.fromInt(cls.level)
    )
  }

  given Encoder[ItemType] = Encoder.instance {
    case ItemType.ATK_POTION => Json.fromString("ATK_POTION")
    case ItemType.DEF_POTION => Json.fromString("DEF_POTION")
  }

  given Encoder[DndCharacter] = Encoder.instance { c =>
    Json.obj(
      "race"       -> Encoder[DndRace].apply(c.dndRace),
      "dndClass"   -> Encoder[DndClass].apply(c.dndClass),
      "armorClass" -> Json.fromInt(c.armorClass),
      "hp"         -> Json.fromInt(c.hp),
      "gold"       -> Json.fromInt(c.gold),
      "bonusAtk"   -> Json.fromInt(c.bonusAtk),
      "bonusDef"   -> Json.fromInt(c.bonusDef)
    )
  }

  given Encoder[DndMapState] = Encoder.instance { s =>
    Json.obj(
      "width"             -> Json.fromInt(s.width),
      "height"            -> Json.fromInt(s.height),
      "playerPos"         -> Encoder[(Int, Int)].apply(s.playerPos),
      "playerOrientation" -> Encoder[CardinalDirection].apply(s.playerOrientation),
      "player"            -> Encoder[DndCharacter].apply(s.player),
      "villains"          -> Json.fromValues(
        s.villains.map { case (pos, v) =>
          Json.obj("pos" -> Encoder[(Int, Int)].apply(pos), "villain" -> Encoder[DndCharacter].apply(v))
        }
      ),
      "npcPositions"  -> Json.fromValues(s.npcPositions.map(Encoder[(Int, Int)].apply)),
      "goldPieces"    -> Json.fromValues(
        s.goldPieces.map { case (pos, amt) =>
          Json.obj("pos" -> Encoder[(Int, Int)].apply(pos), "amount" -> Json.fromInt(amt))
        }
      ),
      "itemPositions" -> Json.fromValues(
        s.itemPositions.map { case (pos, item) =>
          Json.obj("pos" -> Encoder[(Int, Int)].apply(pos), "itemType" -> Encoder[ItemType].apply(item))
        }
      )
    )
  }

  given Encoder[model.FightState] = Encoder.instance { fs =>
    Json.obj(
      "playerHP"  -> Json.fromInt(fs.playerHP),
      "villainHP" -> Json.fromInt(fs.villainHP),
      "round"     -> Json.fromInt(fs.round),
      "log"       -> Json.fromValues(fs.log.map(Json.fromString))
    )
  }

  given Encoder[GameHistoryEntry] = Encoder.instance { e =>
    Json.obj(
      "date"         -> Json.fromString(e.date),
      "result"       -> Json.fromString(e.result),
      "gold"         -> Json.fromInt(e.gold),
      "finalHp"      -> Json.fromInt(e.finalHp),
      "villainCount" -> Json.fromInt(e.villainCount)
    )
  }
