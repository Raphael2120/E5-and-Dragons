import { Component, inject, AfterViewChecked, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game.service';

@Component({
  selector: 'app-log',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './log.component.html',
  styleUrl: './log.component.scss'
})
export class LogComponent implements AfterViewChecked {
  @ViewChild('logEnd') logEnd!: ElementRef;
  readonly game = inject(GameService);

  ngAfterViewChecked(): void {
    this.logEnd?.nativeElement.scrollIntoView({ behavior: 'smooth' });
  }
}
