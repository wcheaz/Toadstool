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
  selector: 'app-order-confirmation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-confirmation.component.html',
  styleUrl: './order-confirmation.component.css'
})
export class OrderConfirmationComponent {
  @Input() isOpen: boolean = false;
  @Input() orderType: 'buy' | 'sell' = 'buy';
  @Input() selectedAsset: Asset | null = null;
  @Input() quantity: number = 50;
  @Input() limitPrice: number = 0;
  @Input() duration: 'day' | 'gtc' = 'day';
  @Input() orderSide: 'market' | 'limit' = 'market';
  @Output() confirm = new EventEmitter<void>();
  @Output() editOrder = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  currentBalance: number = 142384.50;
  
  get estimatedValue(): number {
    if (!this.selectedAsset) return 0;
    return this.quantity * this.selectedAsset.price;
  }

  get estimatedFees(): number {
    return 0;
  }

  get orderTotal(): number {
    return this.estimatedValue + this.estimatedFees;
  }

  get buyingPowerRemaining(): number {
    return this.currentBalance - this.orderTotal;
  }

  get estExecutionPrice(): string {
    if (!this.selectedAsset) return '$0.00';
    return `$${this.selectedAsset.price.toFixed(2)}`;
  }

  get durationLabel(): string {
    return this.duration === 'day' ? 'Day' : 'GTC (Good Till Cancel)';
  }

  confirmOrder() {
    this.confirm.emit();
  }

  editOrderClick() {
    this.editOrder.emit();
  }

  closeModal() {
    this.close.emit();
  }
}
