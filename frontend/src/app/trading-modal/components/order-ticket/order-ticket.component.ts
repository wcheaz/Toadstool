import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Asset } from '../../../shared/models/asset.model';

/**
 * Snapshot of the order ticket form, emitted when the user previews
 * or places an order. Consumed by the trading modal to run the order flow.
 */
export interface OrderTicketValue {
  orderType: 'market' | 'limit';
  orderSide: 'buy' | 'sell';
  quantity: number;
  limitPrice: number;
  duration: 'day' | 'gtc';
  agreedToTerms: boolean;
}

@Component({
  selector: 'app-order-ticket',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-ticket.component.html',
  styleUrl: './order-ticket.component.css'
})
export class OrderTicketComponent {
  @Input() selectedAsset: Asset | null = null;
  @Input() accountBalance: number | null = null;
  @Output() orderPreviewed = new EventEmitter<OrderTicketValue>();

  orderType: 'market' | 'limit' = 'market';
  orderSide: 'buy' | 'sell' = 'buy';
  quantity: number = 50;
  limitPrice: number = 0;
  duration: 'day' | 'gtc' = 'day';
  agreedToTerms: boolean = false;

  get estimatedValue(): number {
    const price = this.orderType === 'limit' ? this.limitPrice : this.selectedAsset?.price;
    if (price === undefined) return 0;
    return this.quantity * price;
  }

  get estimatedFees(): number {
    return 0; // Placeholder for fee calculation
  }

  get orderTotal(): number {
    return this.estimatedValue + this.estimatedFees;
  }

  get validationError(): string {
    if (!this.selectedAsset) return 'Select an asset before placing an order.';
    if (!Number.isFinite(this.quantity) || this.quantity <= 0) {
      return 'Quantity must be greater than zero.';
    }
    if (!Number.isFinite(this.selectedAsset.price) || this.selectedAsset.price <= 0) {
      return 'The selected asset does not have a valid market price.';
    }
    if (this.orderType === 'limit' && (!Number.isFinite(this.limitPrice) || this.limitPrice <= 0)) {
      return 'Limit price must be greater than zero.';
    }
    if (this.orderSide === 'sell' && this.quantity > this.selectedAsset.holdings) {
      return `You can sell at most ${this.selectedAsset.holdings} shares.`;
    }
    if (this.orderSide === 'buy' && this.accountBalance !== null &&
        (!Number.isFinite(this.accountBalance) || this.orderTotal > this.accountBalance)) {
      return `This order exceeds your available balance of $${this.accountBalance.toFixed(2)}.`;
    }
    return '';
  }

  get isFormValid(): boolean {
    return this.validationError === '';
  }

  get isSellQuantityValid(): boolean {
    return !!this.selectedAsset &&
      Number.isFinite(this.quantity) &&
      this.quantity > 0 &&
      this.quantity <= this.selectedAsset.holdings;
  }

  get estFillPrice(): string {
    if (!this.selectedAsset) return '$0.00';
    if (this.orderType === 'limit') {
      return `$${this.limitPrice.toFixed(2)}`;
    }
    return `$${this.selectedAsset.price.toFixed(2)}`;
  }

  placeOrder() {
    if (!this.agreedToTerms) {
      alert('Please agree to market conditions before placing an order');
      return;
    }
    if (!this.isFormValid) return;
    this.previewOrder();
  }

  previewOrder() {
    if (!this.isFormValid) return;
    this.orderPreviewed.emit({
      orderType: this.orderType,
      orderSide: this.orderSide,
      quantity: this.quantity,
      limitPrice: this.limitPrice,
      duration: this.duration,
      agreedToTerms: this.agreedToTerms
    });
  }
}
