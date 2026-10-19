import { Injectable } from '@angular/core';
import { AuthResponse } from './api/api.models';
import { ApiService } from './api/api.service';
import { clearAuthSession, getAccessToken, getAccountId as readAccountId, getClientDisplayName, getRefreshToken, storeAuthSession } from '../auth-storage';
import { Observable, throwError } from 'rxjs';
import { tap } from 'rxjs/operators';

/**
 * Handles the client session stored in localStorage.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private apiService: ApiService) {}

  /**
   * Store client info in localStorage after login/registration
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

  getAccountId(): string {
    return readAccountId() || '';
  }

  logout(tokenToRevoke?: string | null): void {
    const token = tokenToRevoke ?? getAccessToken() ?? getRefreshToken();
    this.apiService.logout(token).subscribe({
      next: () => this.finishLogout(),
      error: (error) => {
        console.error('Failed to revoke session during sign out:', error);
        this.finishLogout();
      }
    });
  }

  forceLogout(): void {
    this.finishLogout();
  }

  private finishLogout(): void {
    clearAuthSession();
    window.location.href = '/login';
  }
}
