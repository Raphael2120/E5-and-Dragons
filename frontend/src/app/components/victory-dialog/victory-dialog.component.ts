import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';

@Component({
    selector: 'app-victory-dialog',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './victory-dialog.component.html',
    styleUrl: './victory-dialog.component.scss'
})
export class VictoryDialogComponent {
    readonly game = inject(GameService);

    get isVisible(): boolean {
        return this.game.isVictory();
    }

    playAgain(): void {
        this.game.reset().subscribe();
    }

    close(): void {
        this.game.isVictory.set(false);
    }
}
