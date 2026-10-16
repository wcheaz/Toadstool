import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthResponse } from './api/api.models';
import { ApiService } from './api/api.service';
import { clearAuthSession, getAccountId, getClientDisplayName, storeAuthSession } from '../auth-storage';

/**
 * Handles the client session stored for the current browser tab.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private apiService: ApiService, private router: Router) {}

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
    this.apiService.logout()
      .pipe(
        finalize(() => {
          clearAuthSession();
          this.router.navigate(['/login'], { replaceUrl: true });
        })
      )
      .subscribe({
        error: (error) => {
          console.error('Server logout failed; completing local logout:', error);
        }
      });
  }
}
