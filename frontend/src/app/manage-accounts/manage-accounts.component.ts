import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AccountResponse } from '../core/api/api.models';
import { ApiService } from '../core/api/api.service';
import { AccountStateService } from '../core/account-state.service';
import { WithdrawalModalComponent, WithdrawalSummary } from '../withdrawal-modal/withdrawal-modal.component';

@Component({
  selector: 'app-manage-accounts',
  standalone: true,
  imports: [CommonModule, FormsModule, WithdrawalModalComponent],
  templateUrl: './manage-accounts.component.html',
  styleUrl: './manage-accounts.component.css'
})
export class ManageAccountsComponent implements OnInit, OnDestroy {
  accounts: AccountResponse[] = [];
  selectedAccount: AccountResponse | null = null;
  accountName = '';
  transferDestination = '';
  transferAmount: number | null = null;
  isBusy = false;
  notice = '';
  noticeType: 'success' | 'error' | 'info' = 'info';
  isLiquidationOpen = false;
  isWithdrawalOpen = false;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private accountState: AccountStateService,
    private apiService: ApiService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.accountState.accounts$.pipe(takeUntil(this.destroy$)).subscribe(accounts => {
      this.accounts = accounts;
      const updatedSelectedAccount = this.selectedAccount
        ? accounts.find(account => account.accountId === this.selectedAccount?.accountId)
        : null;
      if (updatedSelectedAccount) {
        this.selectedAccount = updatedSelectedAccount;
        this.accountName = updatedSelectedAccount.accountName;
      } else {
        this.selectAccount(accounts[0] || null);
      }
      this.changeDetectorRef.detectChanges();
    });
    this.accountState.selectedAccount$.pipe(takeUntil(this.destroy$)).subscribe(account => {
      this.selectAccount(account);
      this.changeDetectorRef.detectChanges();
    });
    this.accountState.loadAccounts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get otherAccounts(): AccountResponse[] {
    return this.accounts.filter(account => account.accountId !== this.selectedAccount?.accountId && account.status !== 'CLOSED');
  }

  get canClose(): boolean {
    return !!this.selectedAccount && Number(this.selectedAccount.balance) === 0 && this.selectedAccount.status !== 'CLOSED';
  }

  selectAccount(account: AccountResponse | null): void {
    this.selectedAccount = account;
    this.accountName = account?.accountName || '';
    this.transferDestination = '';
  }

  saveAccountName(): void {
    if (!this.selectedAccount || !this.accountName.trim() || this.isBusy) return;
    const updatedName = this.accountName.trim();
    this.isBusy = true;
    this.apiService.updateAccount(this.selectedAccount.accountId, { accountName: updatedName }).subscribe({
      next: account => {
        this.accountState.updateAccount({ ...account, accountName: updatedName });
        this.showNotice('Account name updated.', 'success');
        this.isBusy = false;
      },
      error: () => { this.showNotice('We could not update that account name.', 'error'); this.isBusy = false; }
    });
  }

  transferFunds(): void {
    const amount = Number(this.transferAmount);
    const destination = this.accounts.find(account => account.accountId === this.transferDestination);
    if (!this.selectedAccount || !destination || amount <= 0 || amount > Number(this.selectedAccount.balance) || this.isBusy) return;
    this.isBusy = true;
    this.apiService.postCashEvent(this.selectedAccount.accountId, { eventType: 'WITHDRAWAL', amount }).subscribe({
      next: source => this.apiService.postCashEvent(destination.accountId, { eventType: 'DEPOSIT', amount }).subscribe({
        next: updatedDestination => {
          this.accountState.updateAccount(source);
          this.accountState.updateAccount(updatedDestination);
          this.transferAmount = null;
          this.showNotice(`Transferred ${this.money(amount)} to ${destination.accountName}.`, 'success');
          this.isBusy = false;
        },
        error: () => { this.showNotice('The destination account could not receive the funds.', 'error'); this.isBusy = false; }
      }),
      error: () => { this.showNotice('Transfer declined. Check the available balance.', 'error'); this.isBusy = false; }
    });
  }

  withdrawFunds(): void {
    if (!this.selectedAccount || this.isBusy || Number(this.selectedAccount.balance) <= 0) return;
    this.isWithdrawalOpen = true;
  }

  closeWithdrawal(): void {
    this.isWithdrawalOpen = false;
  }

  submitWithdrawal(summary: WithdrawalSummary): void {
    if (!this.selectedAccount || this.isBusy) return;
    const amount = summary.amount;
    this.isBusy = true;
    this.apiService.postCashEvent(this.selectedAccount.accountId, { eventType: 'WITHDRAWAL', amount }).subscribe({
      next: account => { this.accountState.updateAccount(account); this.showNotice(`Withdrawal of ${this.money(amount)} submitted.`, 'success'); this.isBusy = false; },
      error: () => { this.showNotice('The withdrawal could not be submitted.', 'error'); this.isBusy = false; }
    });
  }

  closeAccount(): void {
    if (!this.selectedAccount || !this.canClose || this.isBusy) return;
    this.isBusy = true;
    this.apiService.updateAccount(this.selectedAccount.accountId, { status: 'CLOSED' }).subscribe({
      next: account => { this.accountState.removeAccount(account.accountId); this.showNotice('Account closed and removed from your account list.', 'success'); this.isBusy = false; },
      error: error => { this.showNotice(error.status === 422 ? 'Move or withdraw the remaining balance before closing this account.' : 'We could not close this account.', 'error'); this.isBusy = false; }
    });
  }

  showLiquidation(): void { this.isLiquidationOpen = true; }
  closeLiquidation(): void { this.isLiquidationOpen = false; }

  private showNotice(message: string, type: 'success' | 'error' | 'info'): void {
    this.notice = message;
    this.noticeType = type;
  }

  private money(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}