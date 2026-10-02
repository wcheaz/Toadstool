import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderConfirmationComponent } from '../order-confirmation/order-confirmation.component';
import { OrderSuccessComponent } from '../order-success/order-success.component';

interface Asset {
  symbol: string;
  name: string;
  price: number;
  change: number;
  holdings: number;
}

@Component({
  selector: 'app-trading-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, OrderConfirmationComponent, OrderSuccessComponent],
  templateUrl: './trading-modal.component.html',
  styleUrl: './trading-modal.component.css'
})
export class TradingModalComponent {
  @Input() isOpen: boolean = false;
  @Input() selectedAsset: Asset | null = null;
  @Output() close = new EventEmitter<void>();

  orderType: 'market' | 'limit' = 'market';
  orderSide: 'buy' | 'sell' = 'buy';
  quantity: number = 50;
  limitPrice: number = 0;
  duration: 'day' | 'gtc' = 'day';
  agreedToTerms: boolean = false;
  isPreviewOpen: boolean = false;
  isSuccessOpen: boolean = false;

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

  closeModal() {
    this.close.emit();
  }

  placeOrder() {
    if (!this.agreedToTerms) {
      alert('Please agree to market conditions before placing an order');
      return;
    }
    this.isSuccessOpen = true;
  }

  previewOrder() {
    this.isPreviewOpen = true;
  }

  closePreview() {
    this.isPreviewOpen = false;
  }

  editOrder() {
    this.isPreviewOpen = false;
  }

  confirmOrder() {
    this.isPreviewOpen = false;
    this.isSuccessOpen = true;
  }

  onSuccessClose() {
    this.isSuccessOpen = false;
    this.closeModal();
  }

  onReturnToDashboard() {
    this.onSuccessClose();
  }

  onViewActivity() {
    // Navigate to activity or show activity view
    console.log('View activity clicked');
    this.onSuccessClose();
  }
}
