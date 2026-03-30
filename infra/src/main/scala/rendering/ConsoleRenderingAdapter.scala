package rendering

import model.{DndMapState, FightState}
import out.{ExplorationRenderingPortOut, FightRenderingPortOut}

class ConsoleRenderingAdapter extends FightRenderingPortOut, ExplorationRenderingPortOut:

  override def renderMapState(dndMap: DndMapState): Unit =
    println(s"\n=== Dungeon (${dndMap.width}x${dndMap.height}) ===")
    for y <- 0 until dndMap.height do
      for x <- 0 until dndMap.width do
        val pos  = (x, y)
        val cell =
          if dndMap.playerPos == pos           then "C"
          else if dndMap.villains.contains(pos)    then "V"
          else if dndMap.npcPositions.contains(pos) then "N"
          else if dndMap.goldPieces.contains(pos)   then "G"
          else "."
        print(s"[$cell]")
      println()
    println(s"Position: ${dndMap.playerPos}  Facing: ${dndMap.playerOrientation}  Gold: ${dndMap.player.gold}")

  override def renderFightState(fightState: FightState): Unit =
    println(s"Round ${fightState.round} — Your HP: ${fightState.playerHP} | Villain HP: ${fightState.villainHP}")
    fightState.log.lastOption.foreach(l => println(s"  $l"))
