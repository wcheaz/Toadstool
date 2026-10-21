import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface BankAccount {
  id: string;
  accountNumber: string;
  bankName: string;
  accountType: string;
}

export interface WithdrawalSummary {
  amount: number;
  account: BankAccount;
}

@Component({
  selector: 'app-withdrawal-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './withdrawal-modal.component.html',
  styleUrl: './withdrawal-modal.component.css'
})
export class WithdrawalModalComponent {
  @Input() isOpen = false;
  @Input() availableBalance = 0;
  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<WithdrawalSummary>();

  withdrawalAmount = '';
  selectedAccountId = '';
  agreeToTerms = false;
  isProcessing = false;
  currentStep = 1;

  bankAccounts: BankAccount[] = [
    { id: '1', accountNumber: '****1234', bankName: 'Chase Bank', accountType: 'Checking' },
    { id: '2', accountNumber: '****5678', bankName: 'Bank of America', accountType: 'Savings' },
    { id: '3', accountNumber: '****9012', bankName: 'Wells Fargo', accountType: 'Checking' }
  ];

  get amount(): number {
    return Number(this.withdrawalAmount);
  }

  get hasInsufficientFunds(): boolean {
    return this.amount > this.availableBalance;
  }

  get selectedAccount(): BankAccount | null {
    return this.bankAccounts.find(account => account.id === this.selectedAccountId) || null;
  }

  canProceedToStep2(): boolean {
    return Number.isFinite(this.amount) && this.amount > 0 && !this.hasInsufficientFunds;
  }

  canProceedToStep3(): boolean {
    return this.canProceedToStep2() && !!this.selectedAccount;
  }

  nextStep(): void {
    if (this.currentStep === 1 && this.canProceedToStep2()) this.currentStep = 2;
    else if (this.currentStep === 2 && this.canProceedToStep3()) this.currentStep = 3;
  }

  previousStep(): void {
    if (this.currentStep > 1) this.currentStep--;
  }

  closeModal(): void {
    this.close.emit();
    this.resetForm();
  }

  resetForm(): void {
    this.withdrawalAmount = '';
    this.selectedAccountId = '';
    this.agreeToTerms = false;
    this.isProcessing = false;
    this.currentStep = 1;
  }

  submitWithdrawal(): void {
    if (!this.agreeToTerms || !this.canProceedToStep3() || !this.selectedAccount) return;

    this.isProcessing = true;
    this.submit.emit({ amount: this.amount, account: this.selectedAccount });
    this.closeModal();
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}
