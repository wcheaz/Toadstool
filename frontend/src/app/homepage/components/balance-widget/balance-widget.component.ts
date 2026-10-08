import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-balance-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './balance-widget.component.html',
  styleUrl: './balance-widget.component.css'
})
export class BalanceWidgetComponent {
  @Output() depositClicked = new EventEmitter<void>();
  @Output() withdrawClicked = new EventEmitter<void>();
  @Output() transferClicked = new EventEmitter<void>();

  // TODO: Remove - mock balance data
  currentBalance: number = 142384.50;
  // TODO: Remove - mock balance data
  availableForTrading: number = 12450.00;

  isBalanceVisible: boolean = true;

  toggleBalanceVisibility() {
    this.isBalanceVisible = !this.isBalanceVisible;
  }

  deposit() {
    this.depositClicked.emit();
  }

  withdraw() {
    this.withdrawClicked.emit();
  }

  transfer() {
    this.transferClicked.emit();
  }
}
