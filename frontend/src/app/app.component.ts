import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from './services/game.service';
import { MapComponent } from './components/map/map.component';
import { StatsComponent } from './components/stats/stats.component';
import { LogComponent } from './components/log/log.component';
import { CombatDialogComponent } from './components/combat-dialog/combat-dialog.component';
import { ShopDialogComponent } from './components/shop-dialog/shop-dialog.component';
import { VictoryDialogComponent } from './components/victory-dialog/victory-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, MapComponent, StatsComponent, LogComponent, CombatDialogComponent, ShopDialogComponent, VictoryDialogComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  readonly game = inject(GameService);

  ngOnInit(): void {
    // Restore session state on page load; also load history
    this.game.restoreSession().subscribe();
    this.game.loadHistory().subscribe();
  }

  startGame(): void {
    this.game.newGame().subscribe(() => {
      // Refresh history after new game (session reset clears it server-side potentially)
    });
  }
}
