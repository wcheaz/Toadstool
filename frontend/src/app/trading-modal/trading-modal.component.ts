import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, OrderResponse } from '../api.service';
import { OrderConfirmationComponent } from '../order-confirmation/order-confirmation.component';
import { OrderSuccessComponent } from '../order-success/order-success.component';
import { ErrorModalComponent } from '../error-modal/error-modal.component';

interface Asset {
  instrumentId: string;
  symbol: string;
  name: string;
  price: number;
  change: number;
  holdings: number;
}

@Component({
  selector: 'app-trading-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, OrderConfirmationComponent, OrderSuccessComponent, ErrorModalComponent],
  templateUrl: './trading-modal.component.html',
  styleUrl: './trading-modal.component.css'
})
export class TradingModalComponent {
  @Input() isOpen: boolean = false;
  @Input() selectedAsset: Asset | null = null;
  @Input() accountId: string = '';
  @Output() close = new EventEmitter<void>();

  orderType: 'market' | 'limit' = 'market';
  orderSide: 'buy' | 'sell' = 'buy';
  quantity: number = 50;
  limitPrice: number = 0;
  duration: 'day' | 'gtc' = 'day';
  agreedToTerms: boolean = false;
  isPreviewOpen: boolean = false;
  isSuccessOpen: boolean = false;
  isErrorOpen: boolean = false;
  errorMessage: string = '';
  errorDetails: string = '';
  isSubmitting: boolean = false;

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

  constructor(private apiService: ApiService) {}

  closeModal() {
    this.close.emit();
  }

  placeOrder() {
    if (!this.agreedToTerms) {
      alert('Please agree to market conditions before placing an order');
      return;
    }
    this.previewOrder();
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
    if (!this.agreedToTerms) {
      this.errorMessage = 'Please agree to market conditions before placing an order';
      this.isErrorOpen = true;
      return;
    }

    if (!this.selectedAsset) {
      this.errorMessage = 'No asset selected';
      this.isErrorOpen = true;
      return;
    }

    if (!this.accountId) {
      this.errorMessage = 'Account ID not found. Please log in again.';
      this.isErrorOpen = true;
      return;
    }

    // TODO (W3-8): Fix confirmation modal display - should show before order submission
    // Currently the modal closes immediately. Need to investigate why it's not displaying.
    // Close the preview modal immediately
    this.isPreviewOpen = false;
    this.isSubmitting = true;

    // Call the backend to place the order
    this.apiService.placeOrder(
      this.accountId,
      this.selectedAsset.instrumentId,
      this.orderSide.toUpperCase(),
      this.quantity.toString()
    ).subscribe({
      next: (response: OrderResponse) => {
        this.isSubmitting = false;
        console.log('Order placed successfully:', response);
        this.isSuccessOpen = true;
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('Order placement failed:', error);
        
        // Extract error message
        if (error.error && error.error.message) {
          this.errorMessage = error.error.message;
        } else if (error.status === 422) {
          this.errorMessage = 'Order validation failed. Please check your inputs.';
        } else if (error.status === 409) {
          this.errorMessage = 'This order already exists. Please try again with different details.';
        } else if (error.status === 404) {
          this.errorMessage = 'Account or instrument not found.';
        } else if (error.status >= 500) {
          this.errorMessage = 'Server error. Please try again later.';
        } else {
          this.errorMessage = 'Failed to place order. Please try again.';
        }

        this.errorDetails = error.status ? `Error ${error.status}` : 'Network error';
        this.isErrorOpen = true;
      }
    });
  }

  closeErrorModal() {
    this.isErrorOpen = false;
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
