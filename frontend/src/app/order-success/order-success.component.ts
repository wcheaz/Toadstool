import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Asset {
  instrumentId: string;
  symbol: string;
  name: string;
  price: number;
  change: number;
  holdings: number;
}

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-success.component.html',
  styleUrl: './order-success.component.css'
})
export class OrderSuccessComponent {
  @Input() isOpen: boolean = false;
  @Input() orderType: 'buy' | 'sell' = 'buy';
  @Input() selectedAsset: Asset | null = null;
  @Input() quantity: number = 50;
  @Input() filledPrice: number = 142.12;
  @Output() returnToDashboard = new EventEmitter<void>();
  @Output() viewActivity = new EventEmitter<void>();

  currentBalance: number = 142384.50;
  estimatedValue: number = 7106.00;
  estimatedFees: number = 0.00;
  orderTotal: number = 7106.00;
  updatedBuyingPower: number = 135278.50;

  // Generate order confirmation number
  get confirmationNumber(): string {
    return 'TT-1024-NE-' + Math.floor(Math.random() * 900000 + 100000);
  }

  get currentTime(): string {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    return `${hours}:${minutes}:${seconds}`;
  }

  get currentDate(): string {
    const now = new Date();
    return now.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  onReturnToDashboard() {
    this.returnToDashboard.emit();
  }

  onViewActivity() {
    this.viewActivity.emit();
  }
}
