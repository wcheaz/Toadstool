import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AccountResponse } from './api/api.models';
import { ApiService } from './api/api.service';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class AccountStateService {
  private readonly accountsSubject = new BehaviorSubject<AccountResponse[]>([]);
  private readonly selectedAccountSubject = new BehaviorSubject<AccountResponse | null>(null);
  private hasLoaded = false;

  readonly accounts$ = this.accountsSubject.asObservable();
  readonly selectedAccount$ = this.selectedAccountSubject.asObservable();

  constructor(private apiService: ApiService, private authService: AuthService) {}

  loadAccounts(): void {
    if (this.hasLoaded) return;
    const clientId = this.authService.getClientId();
    if (!clientId) return;
    this.hasLoaded = true;

    this.apiService.getAccountsByClientId(clientId).subscribe({
      next: (response) => {
        const accounts = response.items ?? [];
        this.accountsSubject.next(accounts);
        const selected = accounts.find(account => account.accountId === this.authService.getAccountId())
          || accounts[0]
          || null;
        if (selected) {
          this.selectAccount(selected);
        } else {
          this.selectedAccountSubject.next(null);
        }
      },
      error: (error) => {
        this.hasLoaded = false;
        console.error('Failed to load accounts:', error);
      }
    });
  }

  selectAccount(account: AccountResponse): void {
    this.authService.setAccountId(account.accountId);
    this.selectedAccountSubject.next(account);
  }

  createAccount(accountName: string) {
    const clientId = this.authService.getClientId();
    if (!clientId) return null;

    return this.apiService.createAccount(clientId, accountName);
  }

  addAccount(account: AccountResponse): void {
    this.accountsSubject.next([...this.accountsSubject.value, account]);
    this.selectAccount(account);
  }

  updateAccount(account: AccountResponse): void {
    this.accountsSubject.next(this.accountsSubject.value.map(existing =>
      existing.accountId === account.accountId ? account : existing));
    this.selectAccount(account);
  }

  removeAccount(accountId: string): void {
    const remaining = this.accountsSubject.value.filter(account => account.accountId !== accountId);
    this.accountsSubject.next(remaining);
    const nextAccount = remaining[0] || null;
    if (nextAccount) this.selectAccount(nextAccount);
  }
}