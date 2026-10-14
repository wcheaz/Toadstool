import { Injectable } from '@angular/core';
import { AuthResponse } from './api/api.models';

/**
 * Handles the client session stored in localStorage.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  /**
   * Store client info in localStorage after login/registration
   */
  storeClientSession(response: AuthResponse): void {
    if (response.clientId) {
      localStorage.setItem('clientId', response.clientId);
    } else {
      localStorage.removeItem('clientId');
    }
    localStorage.setItem('clientEmail', response.email);
    localStorage.setItem('clientName', response.displayName);
    if (response.accountId) {
      localStorage.setItem('accountId', response.accountId);
    } else {
      localStorage.removeItem('accountId');
    }
    localStorage.setItem('accessToken', response.accessToken);
  }

  getClientName(): string {
    return localStorage.getItem('clientName') || 'User';
  }

  getClientId(): string {
    return localStorage.getItem('clientId') || '';
  }

  getAccountId(): string {
    return localStorage.getItem('accountId') || '';
  }

  setAccountId(accountId: string): void {
    localStorage.setItem('accountId', accountId);
  }

  logout(): void {
    localStorage.removeItem('clientName');
    localStorage.removeItem('clientId');
    localStorage.removeItem('clientEmail');
    localStorage.removeItem('accountId');
    localStorage.removeItem('accessToken');
    window.location.href = '/login';
  }
}
