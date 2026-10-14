import { Injectable } from '@angular/core';
import { AuthResponse } from './api/api.models';
import { ApiService } from './api/api.service';
import { clearAuthSession, getAccessToken, getAccountId as readAccountId, getClientDisplayName, storeAuthSession } from '../auth-storage';

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

  getClientName(): string {
    return getClientDisplayName() || 'User';
  }

  getAccountId(): string {
    return readAccountId() || '';
  }

  logout(): void {
    this.apiService.logout(getAccessToken()).subscribe({
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
