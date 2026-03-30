package domain

import actions.CardinalDirection
import characters.{DndCharacter, DndClass, DndRace}
import errors.MapError
import errors.MapError.IllegalMapFormat
import in.ForValidatingMap
import model.DndMapState
import out.ExplorationDataPortOut

class MapManager(dataPortOut: ExplorationDataPortOut) extends ForValidatingMap:
  override def validateAndStoreMap(dataLines: List[String]): Either[MapError, Unit] =
    val lines = dataLines.filterNot(l => l.trim.startsWith("--") || l.isBlank)

    def parts(line: String): Array[String] = line.split("-").map(_.trim)

    def parseOrientation(s: String): Either[MapError, CardinalDirection] = s match
      case "N" => Right(CardinalDirection.NORTH)
      case "S" => Right(CardinalDirection.SOUTH)
      case "E" => Right(CardinalDirection.EAST)
      case "W" => Right(CardinalDirection.WEST)
      case _   => Left(IllegalMapFormat())

    def parseRace(s: String): Either[MapError, DndRace] = s match
      case "HUMAN" => Right(DndRace.HUMAN)
      case _       => Left(IllegalMapFormat())

    def parseDndClass(s: String, lvl: Int): Either[MapError, DndClass] = s match
      case "PALADIN" => Right(DndClass.PALADIN(lvl))
      case _         => Left(IllegalMapFormat())

    def parseInt(s: Option[String]): Either[MapError, Int] =
      s.flatMap(_.toIntOption).toRight(IllegalMapFormat())

    def lift(arr: Array[String], idx: Int): Either[MapError, String] =
      arr.lift(idx).toRight(IllegalMapFormat())

    for
      mapLine     <- lines.find(_.startsWith("M")).toRight(IllegalMapFormat())
      mp          = parts(mapLine)
      width       <- parseInt(mp.lift(1))
      height      <- parseInt(mp.lift(2))

      playerLine  <- lines.find(_.startsWith("C")).toRight(IllegalMapFormat())
      pp          = parts(playerLine)
      px          <- parseInt(pp.lift(1))
      py          <- parseInt(pp.lift(2))
      pLvl        <- parseInt(pp.lift(3))
      pRaceStr    <- lift(pp, 4)
      pRace       <- parseRace(pRaceStr)
      pClassStr   <- lift(pp, 5)
      pClass      <- parseDndClass(pClassStr, pLvl)
      pAC         <- parseInt(pp.lift(6))
      pHP         <- parseInt(pp.lift(7))
      pOrientStr  <- lift(pp, 8)
      pOrient     <- parseOrientation(pOrientStr)

      player = DndCharacter(pRace, pClass, "", pAC, pHP, 0)

      npcPositions = lines.filter(_.startsWith("NPC")).flatMap: l =>
        val p = parts(l)
        for
          x <- p.lift(1).flatMap(_.toIntOption)
          y <- p.lift(2).flatMap(_.toIntOption)
        yield (x, y)

      villains = lines.filter(_.startsWith("PC")).flatMap: l =>
        val p = parts(l)
        (for
          vx   <- p.lift(1).flatMap(_.toIntOption)
          vy   <- p.lift(2).flatMap(_.toIntOption)
          vLvl <- p.lift(3).flatMap(_.toIntOption)
          race <- p.lift(4).flatMap:
            case "HUMAN" => Some(DndRace.HUMAN)
            case _       => None
          cls  <- p.lift(5).flatMap:
            case "PALADIN" => Some(DndClass.PALADIN(vLvl))
            case _         => None
          ac   <- p.lift(6).flatMap(_.toIntOption)
          hp   <- p.lift(7).flatMap(_.toIntOption)
        yield ((vx, vy), DndCharacter(race, cls, "I will defeat you!", ac, hp, 0)))

      goldPieces = lines.filter(_.startsWith("GP")).flatMap: l =>
        val p = parts(l)
        for
          gx     <- p.lift(1).flatMap(_.toIntOption)
          gy     <- p.lift(2).flatMap(_.toIntOption)
          amount <- p.lift(3).flatMap(_.toIntOption)
        yield ((gx, gy), amount)

      state = DndMapState(
        width        = width,
        height       = height,
        playerPos    = (px, py),
        playerOrientation = pOrient,
        player       = player,
        villains     = villains.toMap,
        npcPositions = npcPositions,
        goldPieces   = goldPieces.toMap
      )
      _ = dataPortOut.saveMapState(state)
    yield ()
