import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Observable, throwError } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthResponse } from './api/api.models';
import { ApiService } from './api/api.service';
import { clearAuthSession, getAccessToken, getAccountId as readAccountId, getClientDisplayName, getRefreshToken, storeAuthSession } from '../auth-storage';

/**
 * Handles the client session stored for the current browser tab.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private apiService: ApiService, private router?: Router) {}

  /**
   * Store client info in sessionStorage after login/registration.
   */
  storeClientSession(response: AuthResponse): void {
    storeAuthSession(response);
  }

  refreshSession(): Observable<AuthResponse> {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      this.forceLogout();
      return throwError(() => new Error('Refresh token is missing'));
    }

    return this.apiService.refreshSession(refreshToken).pipe(
      tap((response) => this.storeClientSession(response))
    );
  }

  getClientName(): string {
    return getClientDisplayName() || 'User';
  }

  getClientId(): string {
    return sessionStorage.getItem('clientId') || '';
  }

  getAccountId(): string {
    return readAccountId() || '';
  }

  setAccountId(accountId: string): void {
    sessionStorage.setItem('accountId', accountId);
  }

  logout(tokenToRevoke?: string | null): void {
    const token = tokenToRevoke ?? getAccessToken() ?? getRefreshToken();
    this.apiService.logout(token)
      .pipe(
        finalize(() => {
          this.finishLogout();
        })
      )
      .subscribe({
        error: (error) => {
          console.error('Failed to revoke session during sign out:', error);
        }
      });
  }

  forceLogout(): void {
    this.finishLogout();
  }

  private finishLogout(): void {
    clearAuthSession();
    if (this.router) {
      void this.router.navigate(['/login'], { replaceUrl: true });
      return;
    }
    window.location.href = '/login';
  }
}
