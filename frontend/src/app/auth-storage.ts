import { AuthResponse } from './core/api/api.models';

const storageKeys = {
  accessToken: 'accessToken',
  clientId: 'clientId',
  accountId: 'accountId',
  clientEmail: 'clientEmail',
  clientName: 'clientName',
  username: 'username',
  role: 'role'
} as const;

function sessionStorageOrNull(): Storage | null {
  return typeof window === 'undefined' ? null : window.sessionStorage;
}

function localStorageOrNull(): Storage | null {
  return typeof window === 'undefined' ? null : window.localStorage;
}

function readSessionItem(key: string): string | null {
  return sessionStorageOrNull()?.getItem(key) ?? null;
}

export function storeAuthSession(response: AuthResponse): void {
  clearAuthSession();
  const storage = sessionStorageOrNull();
  if (!storage) {
    return;
  }
  storage.setItem(storageKeys.accessToken, response.accessToken);
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
}

export function clearAuthSession(): void {
  const storages = [sessionStorageOrNull(), localStorageOrNull()];
  storages.forEach((storage) => {
    if (!storage) {
      return;
    }
    Object.values(storageKeys).forEach((key) => storage.removeItem(key));
  });
}

export function getAccessToken(): string | null {
  return readSessionItem(storageKeys.accessToken);
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
