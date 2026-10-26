import { vi } from 'vitest';
import { Subject } from 'rxjs';
import { ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { App } from './app';
import { ApiService } from './core/api/api.service';
import { AuthService } from './core/auth.service';

describe('App', () => {
  let authService: Pick<AuthService, 'getClientName' | 'logout' | 'forceLogout' | 'refreshSession'>;
  let apiService: Pick<ApiService, 'validateToken' | 'refreshSession'>;
  let changeDetectorRef: Pick<ChangeDetectorRef, 'detectChanges'>;
  let app: App;

  beforeEach(() => {
    window.sessionStorage.clear();
    authService = {
      getClientName: vi.fn().mockReturnValue('Frontend User'),
      logout: vi.fn(),
      forceLogout: vi.fn(),
      refreshSession: vi.fn()
    };
    apiService = {
      validateToken: vi.fn(),
      refreshSession: vi.fn()
    };
    changeDetectorRef = {
      detectChanges: vi.fn()
    };
    app = new App(
      authService as AuthService,
      apiService as ApiService,
      { url: '/homepage' } as Router,
      changeDetectorRef as ChangeDetectorRef
    );
  });

  afterEach(() => {
    window.sessionStorage.clear();
  });

  it('should create the app', () => {
    expect(app).toBeTruthy();
  });

  it('should revoke the access token when choosing No in the session modal', () => {
    window.sessionStorage.setItem('accessToken', 'access-token-123');
    app.isSessionRenewalModalOpen = true;

    app.signOutNow();

    expect(app.isSessionRenewalModalOpen).toBe(false);
    expect(authService.logout).toHaveBeenCalledWith('access-token-123');
  });

  it('should close the session modal immediately when choosing Yes', () => {
    const refreshResponse = new Subject<any>();
    window.sessionStorage.setItem('accessToken', 'access-token-123');
    window.sessionStorage.setItem('refreshToken', 'refresh-token-123');
    window.sessionStorage.setItem('refreshTokenExpiresAt', String(Date.now() + (20 * 60 * 1000)));
    vi.mocked(authService.refreshSession).mockReturnValue(refreshResponse.asObservable());

    app.isSessionRenewalModalOpen = true;

    app.staySignedIn();

    expect(app.isSessionRenewalModalOpen).toBe(false);
    expect((app as any).isRefreshingSession).toBe(true);

    refreshResponse.next({
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
      expiresIn: 900,
      refreshTokenExpiresIn: 900,
      email: 'user@example.com',
      displayName: 'Frontend User',
      username: 'frontend.user',
      role: 'USER'
    });
    refreshResponse.complete();

    expect((app as any).isRefreshingSession).toBe(false);
  });

  it('should reopen the renewal popup if the warning window was missed', () => {
    const expiresAt = Date.now() + (4 * 60 * 1000);
    window.sessionStorage.setItem('accessToken', 'access-token-123');
    window.sessionStorage.setItem('refreshToken', 'refresh-token-123');
    window.sessionStorage.setItem('refreshTokenExpiresAt', String(expiresAt));
    (app as any).trackedRefreshToken = 'refresh-token-123';
    (app as any).trackedRefreshTokenExpiresAt = expiresAt;
    (app as any).sessionWarningTimeoutId = null;
    (app as any).sessionExpiryTimeoutId = null;

    (app as any).syncSessionTimers();

    expect(app.isSessionRenewalModalOpen).toBe(true);
    expect(app.sessionRenewalMessage).toContain('another 15 minutes');
  });

  it('should force logout when the tracked session has already expired', () => {
    const expiresAt = Date.now() - 1000;
    window.sessionStorage.setItem('accessToken', 'access-token-123');
    window.sessionStorage.setItem('refreshToken', 'refresh-token-123');
    window.sessionStorage.setItem('refreshTokenExpiresAt', String(expiresAt));
    (app as any).trackedRefreshToken = 'refresh-token-123';
    (app as any).trackedRefreshTokenExpiresAt = expiresAt;
    (app as any).sessionWarningTimeoutId = null;
    (app as any).sessionExpiryTimeoutId = null;

    (app as any).syncSessionTimers();

    expect(authService.forceLogout).toHaveBeenCalled();
    expect(app.isSessionRenewalModalOpen).toBe(false);
  });

  it('should schedule the renewal popup from the refresh token expiry', () => {
    const expiresAt = Date.now() + (6 * 60 * 1000);
    window.sessionStorage.setItem('accessToken', 'access-token-123');
    window.sessionStorage.setItem('refreshToken', 'refresh-token-123');
    window.sessionStorage.setItem('refreshTokenExpiresAt', String(expiresAt));

    (app as any).syncSessionTimers();

    expect(app.isSessionRenewalModalOpen).toBe(false);
    expect((app as any).trackedRefreshToken).toBe('refresh-token-123');
    expect((app as any).trackedRefreshTokenExpiresAt).toBe(expiresAt);
  });
});
