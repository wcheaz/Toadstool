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
  @Output() orderPreviewed = new EventEmitter<OrderTicketValue>();

  orderType: 'market' | 'limit' = 'market';
  orderSide: 'buy' | 'sell' = 'buy';
  quantity: number = 50;
  limitPrice: number = 0;
  duration: 'day' | 'gtc' = 'day';
  agreedToTerms: boolean = false;

  get estimatedValue(): number {
    if (!this.selectedAsset) return 0;
    return this.quantity * this.selectedAsset.price;
  }

  get estimatedFees(): number {
    return 0; // Placeholder for fee calculation
  }

  get orderTotal(): number {
    return this.estimatedValue + this.estimatedFees;
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
    this.previewOrder();
  }

  previewOrder() {
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
