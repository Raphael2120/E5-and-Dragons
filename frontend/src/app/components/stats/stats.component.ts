import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';
import { HistoryComponent } from '../history/history.component';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule, HistoryComponent],
  templateUrl: './stats.component.html',
  styleUrl: './stats.component.scss'
})
export class StatsComponent {
  readonly game = inject(GameService);

  get hpPercent(): number {
    const s = this.game.state();
    if (!s) return 0;
    // Relative indicator: bar fills more as HP grows but never reaches 100%
    const hp = Math.max(0, s.player.hp);
    return Math.round((hp / (hp + 50)) * 100);
  }

  get hpColor(): string {
    const p = this.hpPercent;
    if (p > 60) return '#4caf50';
    if (p > 30) return '#ff9800';
    return '#f44336';
  }
}
