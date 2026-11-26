import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';

import { Announcer } from '../shared/announcer/announcer';
import { AuthResponse, Credentials, Registration, User } from './auth.model';

export const AUTH_URL = '/api/v1/auth';

/**
 * Holds the signed-in session. The access token lives only in memory (never localStorage), so a page reload
 * restores the session through the HttpOnly refresh cookie instead.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly announcer = inject(Announcer);

  private readonly _user = signal<User | null>(null);
  private readonly _accessToken = signal<string | null>(null);
  private refreshInFlight: Promise<string | null> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  accessToken(): string | null {
    return this._accessToken();
  }

  /** Called once at startup: silently restores a session if the refresh cookie is still valid. */
  async restoreSession(): Promise<void> {
    await this.refresh();
  }

  async login(credentials: Credentials): Promise<User> {
    return this.startSession(this.http.post<AuthResponse>(`${AUTH_URL}/login`, credentials));
  }

  async register(registration: Registration): Promise<User> {
    return this.startSession(this.http.post<AuthResponse>(`${AUTH_URL}/register`, registration));
  }

  /** Swaps the refresh cookie for a new access token. Concurrent callers share one request. */
  refresh(): Promise<string | null> {
    this.refreshInFlight ??= firstValueFrom(
      this.http.post<AuthResponse>(`${AUTH_URL}/refresh`, null),
    )
      .then((response) => {
        this.applySession(response);
        return response.accessToken;
      })
      .catch(() => {
        this.clear();
        return null;
      })
      .finally(() => (this.refreshInFlight = null));
    return this.refreshInFlight;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>(`${AUTH_URL}/logout`, null));
    } catch {
      // Signing out locally still matters if the server can't be reached.
    }
    this.clear();
    this.announcer.announce('Signed out');
    await this.router.navigateByUrl('/login');
  }

  /** The refresh token was rejected while using the app: send the user to sign in, then back here. */
  async sessionEnded(): Promise<void> {
    if (!this._accessToken() && this.router.url.startsWith('/login')) return;
    this.clear();
    this.announcer.announce('Your session has ended. Sign in again.');
    await this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
  }

  /** Used after a password change, which ends every other session and issues a new one. */
  applySession(response: AuthResponse): void {
    this._accessToken.set(response.accessToken);
    this._user.set(response.user);
  }

  updateUser(user: User): void {
    this._user.set(user);
  }

  private async startSession(request: Observable<AuthResponse>): Promise<User> {
    const response = await firstValueFrom(request);
    this.applySession(response);
    return response.user;
  }

  private clear(): void {
    this._accessToken.set(null);
    this._user.set(null);
  }
}
