export interface Position {
  x: number;
  y: number;
}

export interface DndClass {
  name: string;
  level: number;
}

export interface DndCharacter {
  race: string;
  dndClass: DndClass;
  armorClass: number;
  hp: number;
  gold: number;
}

export interface VillainEntry {
  pos: Position;
  villain: DndCharacter;
}

export interface GoldEntry {
  pos: Position;
  amount: number;
}

export type Orientation = 'NORTH' | 'SOUTH' | 'EAST' | 'WEST';
export type Direction = 'NORTH' | 'SOUTH' | 'EAST' | 'WEST';
export type NextAction = 'FIGHT' | 'MOVE' | 'LOOT' | 'TALK' | 'DEAD' | 'FIGHT_WON';

export interface GameState {
  width: number;
  height: number;
  playerPos: Position;
  playerOrientation: Orientation;
  player: DndCharacter;
  villains: VillainEntry[];
  npcPositions: Position[];
  goldPieces: GoldEntry[];
}

export interface FightResult {
  won: boolean;
  combatLog: string[];
  finalPlayerHp: number;
  finalVillainHp: number;
}

export interface NewGameResponse {
  sessionId: string;
  state: GameState;
  welcomeMessage: string;
}

export interface ActionResponse {
  state: GameState;
  nextAction: NextAction;
  logs: string[];
  fightResult: FightResult | null;
}

export interface GameHistoryEntry {
  date: string;
  result: 'VICTORY' | 'DEFEAT';
  gold: number;
  finalHp: number;
  villainCount: number;
}
