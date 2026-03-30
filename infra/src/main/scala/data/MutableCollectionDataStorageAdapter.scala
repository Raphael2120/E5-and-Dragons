package data

import characters.DndCharacter
import model.DndMapState
import out.{CombatDataPortOut, ExplorationDataPortOut}

class MutableCollectionDataStorageAdapter extends ExplorationDataPortOut, CombatDataPortOut:
  private var _mapState: Option[DndMapState]                     = None
  private var _characterState: Option[(DndCharacter, DndCharacter)] = None

  override def saveMapState(dndMap: DndMapState): Unit =
    _mapState = Some(dndMap)

  override def saveCharacterState(dndCharacter: DndCharacter, villain: DndCharacter): Unit =
    _characterState = Some((dndCharacter, villain))

  def getMapState: Option[DndMapState]                         = _mapState
  def getCharacterState: Option[(DndCharacter, DndCharacter)]  = _characterState
