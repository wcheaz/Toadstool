import { Injectable } from '@angular/core';
import { AuthResponse } from './api/api.models';
import { ApiService } from './api/api.service';

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
    this.apiService.logout().subscribe({
      next: () => this.finishLogout(),
      error: (error) => {
        console.error('Failed to revoke session during sign out:', error);
        this.finishLogout();
      }
    });
  }

  private finishLogout(): void {
    localStorage.removeItem('clientName');
    localStorage.removeItem('clientId');
    localStorage.removeItem('clientEmail');
    localStorage.removeItem('accountId');
    localStorage.removeItem('accessToken');
    window.location.href = '/login';
  }
}
