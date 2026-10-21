import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountResponse } from '../core/api/api.models';

export interface TransferSummary {
  sourceAccount: AccountResponse;
  destinationAccount: AccountResponse;
  amount: number;
}

@Component({
  selector: 'app-transfer-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transfer-modal.component.html',
  styleUrl: './transfer-modal.component.css'
})
export class TransferModalComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() accounts: AccountResponse[] = [];
  @Input() sourceAccountId = '';
  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<TransferSummary>();

  destinationAccountId = '';
  transferAmount: number | null = null;
  isProcessing = false;
  private defaultSourceAccountId = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sourceAccountId']) {
      this.defaultSourceAccountId = this.sourceAccountId;
    }
  }

  get sourceAccount(): AccountResponse | null {
    return this.accounts.find(account => account.accountId === this.sourceAccountId) || null;
  }

  get destinationAccount(): AccountResponse | null {
    return this.accounts.find(account => account.accountId === this.destinationAccountId) || null;
  }

  get otherAccounts(): AccountResponse[] {
    return this.accounts.filter(account => account.accountId !== this.sourceAccountId && account.status !== 'CLOSED');
  }

  get openAccounts(): AccountResponse[] {
    return this.accounts.filter(account => account.status !== 'CLOSED');
  }

  get amountIsTooLarge(): boolean {
    return Number(this.transferAmount) > Number(this.sourceAccount?.balance || 0);
  }

  get canSubmit(): boolean {
    return !!this.sourceAccount && !!this.destinationAccount && Number.isFinite(Number(this.transferAmount)) && Number(this.transferAmount) > 0 && !this.amountIsTooLarge && !this.isProcessing;
  }

  closeModal(): void {
    this.close.emit();
    this.resetForm();
  }

  onSourceChanged(accountId: string): void {
    this.sourceAccountId = accountId;
    if (this.destinationAccountId === accountId) this.destinationAccountId = '';
  }

  resetForm(): void {
    this.sourceAccountId = this.defaultSourceAccountId;
    this.destinationAccountId = '';
    this.transferAmount = null;
    this.isProcessing = false;
  }

  submitTransfer(): void {
    if (!this.canSubmit || !this.sourceAccount || !this.destinationAccount) return;
    this.isProcessing = true;
    const summary: TransferSummary = {
      sourceAccount: this.sourceAccount,
      destinationAccount: this.destinationAccount,
      amount: Number(this.transferAmount)
    };
    this.submit.emit(summary);
    this.closeModal();
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}
