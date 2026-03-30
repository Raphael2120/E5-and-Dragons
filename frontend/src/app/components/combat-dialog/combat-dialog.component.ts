import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';

@Component({
  selector: 'app-combat-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './combat-dialog.component.html',
  styleUrl: './combat-dialog.component.scss'
})
export class CombatDialogComponent {
  readonly game = inject(GameService);

  dismiss(): void {
    this.game.dismissFightResult();
  }

  playAgain(): void {
    this.game.reset().subscribe();
  }
}
