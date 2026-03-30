package history

import java.time.Instant

case class GameHistoryEntry(
  date:         String,   // ISO-8601
  result:       String,   // "VICTORY" | "DEFEAT"
  gold:         Int,
  finalHp:      Int,
  villainCount: Int       // villains left on map at end
)

object GameHistoryEntry:
  def now(result: String, gold: Int, finalHp: Int, villainCount: Int): GameHistoryEntry =
    GameHistoryEntry(Instant.now().toString, result, gold, finalHp, villainCount)
