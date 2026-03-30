import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';
import { ShopItem } from '../../models/game.models';

@Component({
    selector: 'app-shop-dialog',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './shop-dialog.component.html',
    styleUrl: './shop-dialog.component.scss'
})
export class ShopDialogComponent {
    readonly game = inject(GameService);

    get show(): boolean {
        return this.game.isShopOpen();
    }

    get gold(): number {
        return this.game.state()?.player.gold ?? 0;
    }

    buy(item: ShopItem): void {
        this.game.buyItem(item).subscribe();
    }

    close(): void {
        this.game.closeShop();
    }
}
