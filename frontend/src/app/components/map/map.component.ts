import { Component, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';
import { Direction, GameState, Position } from '../../models/game.models';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss'
})
export class MapComponent {
  readonly game = inject(GameService);

  get state(): GameState | null { return this.game.state(); }

  get grid(): { x: number; y: number }[][] {
    const s = this.state;
    if (!s) return [];
    return Array.from({ length: s.height }, (_, y) =>
      Array.from({ length: s.width }, (_, x) => ({ x, y }))
    );
  }

  cellType(x: number, y: number): 'player' | 'villain' | 'npc' | 'gold' | 'item' | 'empty' {
    const s = this.state;
    if (!s) return 'empty';
    if (s.playerPos.x === x && s.playerPos.y === y) return 'player';
    if (s.villains.some(v => v.pos.x === x && v.pos.y === y)) return 'villain';
    if (s.npcPositions.some(p => p.x === x && p.y === y)) return 'npc';
    if (s.goldPieces.some(g => g.pos.x === x && g.pos.y === y)) return 'gold';
    if (s.itemPositions.some(i => i.pos.x === x && i.pos.y === y)) return 'item';
    return 'empty';
  }

  goldAmount(x: number, y: number): number {
    return this.state?.goldPieces.find(g => g.pos.x === x && g.pos.y === y)?.amount ?? 0;
  }

  itemEmoji(x: number, y: number): string {
    const item = this.state?.itemPositions.find(i => i.pos.x === x && i.pos.y === y);
    if (!item) return '?';
    return item.itemType === 'ATK_POTION' ? '⚔️' : '🛡️';
  }

  itemLabel(x: number, y: number): string {
    const item = this.state?.itemPositions.find(i => i.pos.x === x && i.pos.y === y);
    if (!item) return '';
    return item.itemType === 'ATK_POTION' ? 'ATK' : 'DEF';
  }

  /** Returns the emoji for the villain at (x,y) based on race+class */
  villainEmoji(x: number, y: number): string {
    const entry = this.state?.villains.find(v => v.pos.x === x && v.pos.y === y);
    if (!entry) return '☠️';
    const { race, dndClass } = entry.villain;
    if (dndClass.name === 'BOSS') return '🐉';
    if (race === 'ORC') return '👹';
    if (race === 'UNDEAD') return '💀';
    return '🧙'; // HUMAN/PALADIN
  }

  villainLabel(x: number, y: number): string {
    const entry = this.state?.villains.find(v => v.pos.x === x && v.pos.y === y);
    if (!entry) return 'ENE';
    const { dndClass } = entry.villain;
    if (dndClass.name === 'BOSS') return 'BOSS';
    if (dndClass.name === 'WARRIOR') return 'GUE';
    if (dndClass.name === 'ARCHER') return 'ARC';
    return 'MAG';
  }

  orientationArrow(): string {
    switch (this.state?.playerOrientation) {
      case 'NORTH': return '↑';
      case 'SOUTH': return '↓';
      case 'EAST': return '→';
      case 'WEST': return '←';
      default: return '';
    }
  }

  onMove(dir: Direction): void {
    if (this.game.isGameOver()) return;
    this.game.move(dir).subscribe();
  }

  onFight(): void {
    if (this.game.isGameOver()) return;
    this.game.fight().subscribe();
  }

  @HostListener('window:keydown', ['$event'])
  onKey(e: KeyboardEvent): void {
    if (this.game.isGameOver() || !this.game.hasSession()) return;
    switch (e.key) {
      case 'ArrowUp': case 'w': case 'W': this.onMove('NORTH'); break;
      case 'ArrowDown': case 's': case 'S': this.onMove('SOUTH'); break;
      case 'ArrowRight': case 'd': case 'D': this.onMove('EAST'); break;
      case 'ArrowLeft': case 'a': case 'A': this.onMove('WEST'); break;
      case 'f': case 'F': this.onFight(); break;
    }
  }
}
