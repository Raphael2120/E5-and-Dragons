import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ActionResponse, Direction, FightResult,
  GameState, NewGameResponse
} from '../models/game.models';

@Injectable({ providedIn: 'root' })
export class GameService {
  private sessionId = signal<string | null>(
    typeof localStorage !== 'undefined' ? localStorage.getItem('e5-session-id') : null
  );

  readonly state      = signal<GameState | null>(null);
  readonly log        = signal<string[]>([]);
  readonly fightResult = signal<FightResult | null>(null);
  readonly isGameOver = signal<boolean>(false);
  readonly maxHp      = signal<number>(45);

  readonly hasSession = computed(() => this.sessionId() !== null);

  constructor(private http: HttpClient) {}

  newGame(): Observable<NewGameResponse> {
    return this.http.post<NewGameResponse>(`${environment.apiUrl}/game/new`, {}).pipe(
      tap(r => {
        this.saveSession(r.sessionId);
        this.state.set(r.state);
        this.maxHp.set(r.state.player.hp);
        this.log.set([r.welcomeMessage]);
        this.fightResult.set(null);
        this.isGameOver.set(false);
      })
    );
  }

  reset(): Observable<NewGameResponse> {
    return this.http.post<NewGameResponse>(
      `${environment.apiUrl}/game/reset`, {},
      { headers: this.headers() }
    ).pipe(
      tap(r => {
        this.saveSession(r.sessionId);
        this.state.set(r.state);
        this.maxHp.set(r.state.player.hp);
        this.log.set([r.welcomeMessage]);
        this.fightResult.set(null);
        this.isGameOver.set(false);
      })
    );
  }

  move(direction: Direction): Observable<ActionResponse> {
    return this.http.post<ActionResponse>(
      `${environment.apiUrl}/game/move`,
      { direction },
      { headers: this.headers() }
    ).pipe(tap(r => this.handleAction(r)));
  }

  fight(): Observable<ActionResponse> {
    return this.http.post<ActionResponse>(
      `${environment.apiUrl}/game/fight`, {},
      { headers: this.headers() }
    ).pipe(tap(r => this.handleAction(r)));
  }

  private handleAction(r: ActionResponse): void {
    this.state.set(r.state);
    if (r.logs.length) {
      this.log.update(l => [...l, ...r.logs]);
    }
    if (r.fightResult) {
      this.fightResult.set(r.fightResult);
    }
    if (r.nextAction === 'DEAD') {
      this.isGameOver.set(true);
    }
  }

  dismissFightResult(): void {
    this.fightResult.set(null);
  }

  private saveSession(id: string): void {
    this.sessionId.set(id);
    localStorage.setItem('e5-session-id', id);
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ 'X-Session-Id': this.sessionId() ?? '' });
  }
}
