import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-balance-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './balance-widget.component.html',
  styleUrl: './balance-widget.component.css'
})
export class BalanceWidgetComponent {
  @Input() accountName: string = '';
  @Input() portfolioValue: number = 0;
  @Input() availableForTrading: number = 0;
  @Output() depositClicked = new EventEmitter<void>();
  @Output() withdrawClicked = new EventEmitter<void>();
  @Output() transferClicked = new EventEmitter<void>();

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
