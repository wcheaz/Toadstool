import { Injectable } from '@angular/core';
import { AuthResponse } from './api/api.models';
import { clearAuthSession, getAccountId, getClientDisplayName, storeAuthSession } from '../auth-storage';

/**
 * Handles the client session stored for the current browser tab.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  /**
   * Store client info in sessionStorage after login/registration.
   */
  storeClientSession(response: AuthResponse): void {
    storeAuthSession(response);
  }

  getClientName(): string {
    return getClientDisplayName() || 'User';
  }

  getClientId(): string {
    return sessionStorage.getItem('clientId') || '';
  }

  getAccountId(): string {
    return getAccountId() || '';
  }

  setAccountId(accountId: string): void {
    sessionStorage.setItem('accountId', accountId);
  }

  logout(): void {
    clearAuthSession();
    window.location.replace('/login');
  }
}
