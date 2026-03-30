import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stats.component.html',
  styleUrl: './stats.component.scss'
})
export class StatsComponent {
  readonly game = inject(GameService);

  get hpPercent(): number {
    const s = this.game.state();
    if (!s) return 0;
    return Math.max(0, Math.round((s.player.hp / this.game.maxHp()) * 100));
  }

  get hpColor(): string {
    const p = this.hpPercent;
    if (p > 60) return '#4caf50';
    if (p > 30) return '#ff9800';
    return '#f44336';
  }
}
