import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';

@Component({
    selector: 'app-history',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './history.component.html',
    styleUrl: './history.component.scss'
})
export class HistoryComponent {
    readonly game = inject(GameService);

    formatDate(iso: string): string {
        const d = new Date(iso);
        return d.toLocaleDateString('fr-FR', {
            day: '2-digit', month: '2-digit',
            hour: '2-digit', minute: '2-digit'
        });
    }
}
