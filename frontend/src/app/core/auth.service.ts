import { Injectable } from '@angular/core';
import { LoginResponse } from './api/api.models';

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
  storeClientSession(response: LoginResponse): void {
    localStorage.setItem('clientId', response.clientId);
    localStorage.setItem('clientEmail', response.email);
    localStorage.setItem('clientName', response.displayName);
    localStorage.setItem('clientStatus', response.status);
  }

  getClientName(): string {
    return localStorage.getItem('clientName') || 'User';
  }

  getAccountId(): string {
    return localStorage.getItem('accountId') || '';
  }

  logout(): void {
    localStorage.removeItem('clientName');
    window.location.href = '/login';
  }
}
