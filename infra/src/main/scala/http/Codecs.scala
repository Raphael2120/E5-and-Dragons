package http

import actions.CardinalDirection
import characters.{DndCharacter, DndClass, DndRace}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.*
import model.{DndMapState, FightState}

object Codecs:

  // ── Primitive types ───────────────────────────────────────────────────────

  given Encoder[(Int, Int)] = Encoder.instance((x, y) =>
    Json.obj("x" -> Json.fromInt(x), "y" -> Json.fromInt(y)))

  given Encoder[CardinalDirection] =
    Encoder.encodeString.contramap(_.toString)

  given Decoder[CardinalDirection] =
    Decoder.decodeString.emap:
      case "NORTH" => Right(CardinalDirection.NORTH)
      case "SOUTH" => Right(CardinalDirection.SOUTH)
      case "EAST"  => Right(CardinalDirection.EAST)
      case "WEST"  => Right(CardinalDirection.WEST)
      case s       => Left(s"Unknown direction: $s")

  given Encoder[DndRace] =
    Encoder.encodeString.contramap(_.toString)

  given Encoder[DndClass] = Encoder.instance:
    case DndClass.PALADIN(lvl) =>
      Json.obj("name" -> Json.fromString("PALADIN"), "level" -> Json.fromInt(lvl))

  given Encoder[DndCharacter] = Encoder.instance: c =>
    Json.obj(
      "race"        -> Encoder[DndRace].apply(c.dndRace),
      "dndClass"    -> Encoder[DndClass].apply(c.dndClass),
      "armorClass"  -> Json.fromInt(c.armorClass),
      "hp"          -> Json.fromInt(c.hp),
      "gold"        -> Json.fromInt(c.gold)
    )

  given Encoder[DndMapState] = Encoder.instance: s =>
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
      "npcPositions" -> Json.fromValues(s.npcPositions.map(Encoder[(Int, Int)].apply)),
      "goldPieces"   -> Json.fromValues(
        s.goldPieces.map { case (pos, amt) =>
          Json.obj("pos" -> Encoder[(Int, Int)].apply(pos), "amount" -> Json.fromInt(amt))
        }
      )
    )

  given Encoder[model.FightState] = Encoder.instance: fs =>
    Json.obj(
      "playerHP"  -> Json.fromInt(fs.playerHP),
      "villainHP" -> Json.fromInt(fs.villainHP),
      "round"     -> Json.fromInt(fs.round),
      "log"       -> Json.fromValues(fs.log.map(Json.fromString))
    )
