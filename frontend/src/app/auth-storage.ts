import { AuthResponse } from './api.service';

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

function readStoredItem(key: string): string | null {
  const sessionValue = sessionStorageOrNull()?.getItem(key);
  if (sessionValue) {
    return sessionValue;
  }
  return localStorageOrNull()?.getItem(key) ?? null;
}

export function storeAuthSession(response: AuthResponse): void {
  clearAuthSession();
  const storage = sessionStorageOrNull();
  if (!storage) {
    return;
  }
  storage.setItem(storageKeys.accessToken, response.accessToken);
  storage.setItem(storageKeys.clientId, response.clientId);
  storage.setItem(storageKeys.accountId, response.accountId);
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
  return readStoredItem(storageKeys.accessToken);
}

export function getClientDisplayName(): string | null {
  return readStoredItem(storageKeys.clientName) ?? readStoredItem(storageKeys.username);
}

export function getAccountId(): string | null {
  return readStoredItem(storageKeys.accountId);
}

export function isAuthenticated(): boolean {
  return !!getAccessToken();
}
