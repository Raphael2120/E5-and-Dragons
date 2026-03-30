import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from './services/game.service';
import { MapComponent } from './components/map/map.component';
import { StatsComponent } from './components/stats/stats.component';
import { LogComponent } from './components/log/log.component';
import { CombatDialogComponent } from './components/combat-dialog/combat-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, MapComponent, StatsComponent, LogComponent, CombatDialogComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  readonly game = inject(GameService);

  startGame(): void {
    this.game.newGame().subscribe();
  }
}
