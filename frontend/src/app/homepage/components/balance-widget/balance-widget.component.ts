import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-balance-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './balance-widget.component.html',
  styleUrl: './balance-widget.component.css'
})
export class BalanceWidgetComponent {
  // TODO: Remove - mock balance data
  currentBalance: number = 142384.50;
  // TODO: Remove - mock balance data
  availableForTrading: number = 12450.00;

  isBalanceVisible: boolean = true;

  toggleBalanceVisibility() {
    this.isBalanceVisible = !this.isBalanceVisible;
  }

  deposit() {
    alert('Deposit functionality coming soon');
  }

  withdraw() {
    alert('Withdraw functionality coming soon');
  }

  transfer() {
    alert('Transfer functionality coming soon');
  }
}
