import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface BankAccount {
  id: string;
  accountNumber: string;
  bankName: string;
  accountType: string;
  lastFour: string;
}

interface DepositSummary {
  amount: number;
  account: BankAccount | null;
  fee: number;
  total: number;
  estimatedTime: string;
}

@Component({
  selector: 'app-deposit-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './deposit-modal.component.html',
  styleUrl: './deposit-modal.component.css'
})
export class DepositModalComponent {
  @Input() isOpen: boolean = false;
  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<DepositSummary>();

  depositAmount: string = '';
  selectedAccountId: string = '';
  agreeToTerms: boolean = false;
  isProcessing: boolean = false;
  currentStep: number = 1; // 1: Enter Amount, 2: Select Account, 3: Review

  bankAccounts: BankAccount[] = [
    {
      id: '1',
      accountNumber: '****1234',
      bankName: 'Chase Bank',
      accountType: 'Checking',
      lastFour: '1234'
    },
    {
      id: '2',
      accountNumber: '****5678',
      bankName: 'Bank of America',
      accountType: 'Savings',
      lastFour: '5678'
    },
    {
      id: '3',
      accountNumber: '****9012',
      bankName: 'Wells Fargo',
      accountType: 'Checking',
      lastFour: '9012'
    }
  ];

  depositFeePercentage: number = 0.5; // 0.5% fee

  closeModal() {
    this.close.emit();
    this.resetForm();
  }

  resetForm() {
    this.depositAmount = '';
    this.selectedAccountId = '';
    this.agreeToTerms = false;
    this.currentStep = 1;
  }

  getDepositFee(): number {
    const amount = parseFloat(this.depositAmount) || 0;
    return amount * (this.depositFeePercentage / 100);
  }

  getTotalDeposit(): number {
    const amount = parseFloat(this.depositAmount) || 0;
    return amount + this.getDepositFee();
  }

  getSelectedAccount(): BankAccount | null {
    return this.bankAccounts.find(acc => acc.id === this.selectedAccountId) || null;
  }

  canProceedToStep2(): boolean {
    return (parseFloat(this.depositAmount) || 0) >= 100 && (parseFloat(this.depositAmount) || 0) <= 100000;
  }

  canProceedToStep3(): boolean {
    return this.selectedAccountId !== '' && this.canProceedToStep2();
  }

  nextStep() {
    if (this.currentStep === 1 && this.canProceedToStep2()) {
      this.currentStep = 2;
    } else if (this.currentStep === 2 && this.canProceedToStep3()) {
      this.currentStep = 3;
    }
  }

  previousStep() {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  submitDeposit() {
    if (!this.agreeToTerms || !this.canProceedToStep3()) return;

    this.isProcessing = true;

    const summary: DepositSummary = {
      amount: parseFloat(this.depositAmount),
      account: this.getSelectedAccount(),
      fee: this.getDepositFee(),
      total: this.getTotalDeposit(),
      estimatedTime: '1-2 business days'
    };

    setTimeout(() => {
      this.submit.emit(summary);
      this.closeModal();
      this.isProcessing = false;
    }, 1500);
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value);
  }

  parseFloatSafe(value: string): number {
    return parseFloat(value) || 0;
  }
}
