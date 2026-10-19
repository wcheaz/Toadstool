import { of, firstValueFrom } from 'rxjs';
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { ApiService } from './api/api.service';
import { AuthResponse } from './api/api.models';

describe('AuthService', () => {
  let apiService: Pick<ApiService, 'refreshSession' | 'logout'>;
  let authService: AuthService;

  beforeEach(() => {
    apiService = {
      refreshSession: vi.fn(),
      logout: vi.fn()
    };
    authService = new AuthService(apiService as ApiService);
    window.sessionStorage.clear();
    window.sessionStorage.setItem('refreshToken', 'existing-refresh-token');
  });

  afterEach(() => {
    window.sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('stores the refreshed refresh token returned by the server', async () => {
    const response: AuthResponse = {
      accessToken: 'new-access-token',
      expiresIn: 900,
      refreshToken: 'new-refresh-token',
      refreshTokenExpiresIn: 900,
      clientId: 'client-1',
      accountId: 'account-1',
      displayName: 'Client User',
      username: 'clientuser',
      email: 'client@example.com',
      role: 'CLIENT'
    };

    vi.mocked(apiService.refreshSession).mockReturnValue(of(response));

    const result = await firstValueFrom(authService.refreshSession());

    expect(apiService.refreshSession).toHaveBeenCalledWith('existing-refresh-token');
    expect(result).toEqual(response);
    expect(window.sessionStorage.getItem('refreshToken')).toBe('new-refresh-token');
    expect(window.sessionStorage.getItem('accessToken')).toBe('new-access-token');
  });
});
