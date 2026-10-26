import { AuthResponse } from './core/api/api.models';

const storageKeys = {
  accessToken: 'accessToken',
  accessTokenExpiresAt: 'accessTokenExpiresAt',
  refreshToken: 'refreshToken',
  refreshTokenExpiresAt: 'refreshTokenExpiresAt',
  clientId: 'clientId',
  accountId: 'accountId',
  clientEmail: 'clientEmail',
  clientName: 'clientName',
  username: 'username',
  role: 'role'
} as const;

const authSessionChangedEventName = 'auth-session-changed';

function sessionStorageOrNull(): Storage | null {
  return typeof window === 'undefined' ? null : window.sessionStorage;
}

function localStorageOrNull(): Storage | null {
  return typeof window === 'undefined' ? null : window.localStorage;
}

function notifyAuthSessionChanged(): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.dispatchEvent(new Event(authSessionChangedEventName));
}

function readStoredItem(key: string): string | null {
  const sessionValue = sessionStorageOrNull()?.getItem(key);
  if (sessionValue) {
    return sessionValue;
  }
  return localStorageOrNull()?.getItem(key) ?? null;
}

function readSessionItem(key: string): string | null {
  return sessionStorageOrNull()?.getItem(key) ?? null;
}

function requireText(value: string | null | undefined, fieldName: string): string {
  if (!value || !value.trim()) {
    throw new Error(`Auth response is missing ${fieldName}`);
  }
  return value;
}

function requirePositiveNumber(value: number, fieldName: string): number {
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`Auth response is missing valid ${fieldName}`);
  }
  return value;
}

export function storeAuthSession(response: AuthResponse): void {
  const accessToken = requireText(response.accessToken, 'access token');
  const expiresIn = requirePositiveNumber(response.expiresIn, 'access token lifetime');
  const refreshToken = response.refreshToken;
  const refreshTokenExpiresIn = response.refreshTokenExpiresIn;

  const storage = sessionStorageOrNull();
  if (!storage) {
    return;
  }
  Object.values(storageKeys).forEach((key) => storage.removeItem(key));
  storage.setItem(storageKeys.accessToken, accessToken);
  storage.setItem(storageKeys.accessTokenExpiresAt, String(Date.now() + (expiresIn * 1000)));

  const hasRefreshToken = typeof refreshToken === 'string' && refreshToken.trim().length > 0;
  const hasRefreshExpiry = Number.isFinite(refreshTokenExpiresIn) && refreshTokenExpiresIn > 0;
  if (hasRefreshToken && hasRefreshExpiry) {
    storage.setItem(storageKeys.refreshToken, refreshToken);
    storage.setItem(storageKeys.refreshTokenExpiresAt, String(Date.now() + (refreshTokenExpiresIn * 1000)));
  } else {
    storage.removeItem(storageKeys.refreshToken);
    storage.removeItem(storageKeys.refreshTokenExpiresAt);
  }
  if (response.clientId) {
    storage.setItem(storageKeys.clientId, response.clientId);
  }
  if (response.accountId) {
    storage.setItem(storageKeys.accountId, response.accountId);
  }
  storage.setItem(storageKeys.clientEmail, response.email);
  storage.setItem(storageKeys.clientName, response.displayName);
  storage.setItem(storageKeys.username, response.username);
  storage.setItem(storageKeys.role, response.role);
  notifyAuthSessionChanged();
}

export function clearAuthSession(): void {
  const storages = [sessionStorageOrNull(), localStorageOrNull()];
  storages.forEach((storage) => {
    if (!storage) {
      return;
    }
    Object.values(storageKeys).forEach((key) => storage.removeItem(key));
  });
  notifyAuthSessionChanged();
}

export function getAccessToken(): string | null {
  return readSessionItem(storageKeys.accessToken);
}

export function getRefreshToken(): string | null {
  return readStoredItem(storageKeys.refreshToken);
}

export function getAccessTokenExpiresAt(): number | null {
  const expiresAt = readStoredItem(storageKeys.accessTokenExpiresAt);
  if (!expiresAt) {
    return null;
  }

  const parsedExpiresAt = Number(expiresAt);
  return Number.isFinite(parsedExpiresAt) ? parsedExpiresAt : null;
}

export function getRefreshTokenExpiresAt(): number | null {
  const expiresAt = readStoredItem(storageKeys.refreshTokenExpiresAt);
  if (!expiresAt) {
    return null;
  }

  const parsedExpiresAt = Number(expiresAt);
  return Number.isFinite(parsedExpiresAt) ? parsedExpiresAt : null;
}

export function getClientDisplayName(): string | null {
  return readSessionItem(storageKeys.clientName) ?? readSessionItem(storageKeys.username);
}

export function getClientEmail(): string | null {
  return readSessionItem(storageKeys.clientEmail);
}

export function getAccountId(): string | null {
  return readSessionItem(storageKeys.accountId);
}

export function isAuthenticated(): boolean {
  return !!getAccessToken();
}
