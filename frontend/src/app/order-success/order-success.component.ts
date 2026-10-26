import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Asset } from '../shared/models/asset.model';
import { OrderResponse } from '../core/api/api.models';

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
  @Input() filledPrice: number = 0;
  /** The filled order; its id is the confirmation number. */
  @Input() order: OrderResponse | null = null;
  /** Fee charged on the fill; null until the fill has loaded. */
  @Input() fee: number | null = null;
  /** When the fill executed; null until the fill has loaded. */
  @Input() executedAt: Date | null = null;
  /** Cash before this order; null when unknown. */
  @Input() balanceBefore: number | null = null;
  @Output() returnToDashboard = new EventEmitter<void>();
  @Output() viewActivity = new EventEmitter<void>();

  get filledValue(): number {
    return this.quantity * this.filledPrice;
  }

  /** Cash paid for a buy (value plus fee) or received for a sell (value minus fee). */
  get orderTotal(): number {
    const fee = this.fee ?? 0;
    return this.orderType === 'buy' ? this.filledValue + fee : this.filledValue - fee;
  }

  /** Cash after the fill; null until the fee is known, so it never shows a guess. */
  get updatedBuyingPower(): number | null {
    if (this.balanceBefore === null || this.fee === null) return null;
    return this.orderType === 'buy' ? this.balanceBefore - this.orderTotal : this.balanceBefore + this.orderTotal;
  }

  get confirmationNumber(): string {
    return this.order ? this.order.orderId.slice(0, 8).toUpperCase() : '—';
  }

  get executedDateTime(): string {
    if (!this.executedAt) return '—';
    return this.executedAt.toLocaleString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
  }

  get isLimitOrder(): boolean {
    return this.order?.orderType === 'LIMIT';
  }

  onReturnToDashboard() {
    this.returnToDashboard.emit();
  }

  onViewActivity() {
    this.viewActivity.emit();
  }
}
