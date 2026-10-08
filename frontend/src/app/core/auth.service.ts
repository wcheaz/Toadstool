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
    localStorage.setItem('clientId', response.clientId);
    localStorage.setItem('clientEmail', response.email);
    localStorage.setItem('clientName', response.displayName);
    localStorage.setItem('accountId', response.accountId);
    localStorage.setItem('accessToken', response.accessToken);
  }

  getClientName(): string {
    return localStorage.getItem('clientName') || 'User';
  }

  getAccountId(): string {
    return localStorage.getItem('accountId') || '';
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
