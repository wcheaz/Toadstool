import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Asset } from '../shared/models/asset.model';

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
  @Input() orderSide: 'market' | 'limit' = 'market';
  /** The order ticket's "Available to trade after"; null while the balance is unknown. */
  @Input() buyingPowerRemaining: number | null = null;
  @Output() confirm = new EventEmitter<void>();
  @Output() editOrder = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  /** Price the estimate uses: the limit for limit orders, otherwise the live quote. */
  get estimatePrice(): number {
    if (this.orderSide === 'limit' && this.limitPrice > 0) return this.limitPrice;
    return this.selectedAsset?.price ?? 0;
  }

  get estimatedValue(): number {
    return this.quantity * this.estimatePrice;
  }

  get estimatedFees(): number {
    return 0;
  }

  get orderTotal(): number {
    return this.estimatedValue + this.estimatedFees;
  }

  get estExecutionPrice(): string {
    if (!this.selectedAsset) return '$0.00';
    return `$${this.estimatePrice.toFixed(2)}`;
  }

  get priceSource(): string {
    return this.orderSide === 'limit' ? 'Your limit price' : 'Live quote';
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
