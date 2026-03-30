import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap, catchError, of, map } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ActionResponse, Direction, FightResult,
  GameState, NewGameResponse, GameHistoryEntry
} from '../models/game.models';

@Injectable({ providedIn: 'root' })
export class GameService {
  private sessionId = signal<string | null>(
    typeof localStorage !== 'undefined' ? localStorage.getItem('e5-session-id') : null
  );

  readonly state = signal<GameState | null>(null);
  readonly log = signal<string[]>([]);
  readonly fightResult = signal<FightResult | null>(null);
  readonly isGameOver = signal<boolean>(false);
  readonly history = signal<GameHistoryEntry[]>([]);
  readonly isShopOpen = signal<boolean>(false);
  readonly isVictory = signal<boolean>(false);

  readonly hasSession = computed(() => this.sessionId() !== null);

  constructor(private http: HttpClient) { }

  /** Called on app init — try to restore state from an existing session. */
  restoreSession(): Observable<void> {
    if (!this.sessionId()) return of(undefined);
    return this.http.get<{ state: GameState }>(
      `${environment.apiUrl}/game/state`,
      { headers: this.headers() }
    ).pipe(
      tap(r => this.state.set(r.state)),
      map(() => undefined),
      catchError(() => {
        // Session expired / server restarted → clear stale session
        this.clearSession();
        return of(undefined);
      })
    );
  }

  clearSession(): void {
    this.sessionId.set(null);
    if (typeof localStorage !== 'undefined') localStorage.removeItem('e5-session-id');
    this.state.set(null);
    this.log.set([]);
    this.fightResult.set(null);
    this.isGameOver.set(false);
    this.isShopOpen.set(false);
    this.isVictory.set(false);
  }

  newGame(): Observable<NewGameResponse> {
    return this.http.post<NewGameResponse>(`${environment.apiUrl}/game/new`, {}).pipe(
      tap(r => {
        this.saveSession(r.sessionId);
        this.state.set(r.state);
        this.log.set([r.welcomeMessage]);
        this.fightResult.set(null);
        this.isGameOver.set(false);
        this.isShopOpen.set(false);
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
        this.log.set([r.welcomeMessage]);
        this.fightResult.set(null);
        this.isGameOver.set(false);
        this.isShopOpen.set(false);
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

  loadHistory(): Observable<GameHistoryEntry[]> {
    return this.http.get<GameHistoryEntry[]>(`${environment.apiUrl}/game/history`).pipe(
      tap(entries => this.history.set(entries))
    );
  }

  buyItem(item: string): Observable<ActionResponse> {
    return this.http.post<ActionResponse>(
      `${environment.apiUrl}/game/buy`,
      { item },
      { headers: this.headers() }
    ).pipe(
      tap(r => {
        this.handleAction(r);
      })
    );
  }

  closeShop(): void {
    this.isShopOpen.set(false);
  }

  private handleAction(r: ActionResponse): void {
    this.state.set(r.state);
    if (r.logs.length) {
      this.log.update(l => [...l, ...r.logs]);
    }
    if (r.fightResult) {
      this.fightResult.set(r.fightResult);
    }
    if (r.nextAction === 'DEAD' || r.nextAction === 'FIGHT_WON' || r.nextAction === 'VICTORY') {
      if (r.nextAction === 'DEAD' || r.nextAction === 'VICTORY') this.isGameOver.set(true);
      if (r.nextAction === 'VICTORY') this.isVictory.set(true);
      // Refresh history from backend after game ends
      this.loadHistory().subscribe();
    }
    if (r.nextAction === 'TALK') {
      this.isShopOpen.set(true);
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
