import { Component, Input, Output, EventEmitter, OnChanges, OnDestroy, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { EMPTY, Subscription, timer } from 'rxjs';
import { catchError, filter, switchMap, take, timeout } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { ApiService } from '../core/api/api.service';
import { CandleResponse, DepthLevel, OrderResponse } from '../core/api/api.models';
import { toDate } from '../admin-dashboard/admin-format';
import { Asset } from '../shared/models/asset.model';
import { ChartComponent } from '../shared/components/chart/chart.component';
import { ErrorModalComponent } from '../shared/components/error-modal/error-modal.component';
import { OrderConfirmationComponent } from '../order-confirmation/order-confirmation.component';
import { OrderSuccessComponent } from '../order-success/order-success.component';
import { OrderTicketComponent, OrderTicketValue } from './components/order-ticket/order-ticket.component';
import { MarketDepthComponent } from './components/market-depth/market-depth.component';

/** How the ticket follows server-side execution of a submitted order. */
const ORDER_STATUS_FIRST_CHECK_MS = 500;
const ORDER_STATUS_INTERVAL_MS = 1000;
const ORDER_STATUS_TIMEOUT_MS = 30000;

@Component({
  selector: 'app-trading-modal',
  standalone: true,
  imports: [
    CommonModule,
    ChartComponent,
    OrderTicketComponent,
    MarketDepthComponent,
    OrderConfirmationComponent,
    OrderSuccessComponent,
    ErrorModalComponent
  ],
  templateUrl: './trading-modal.component.html',
  styleUrl: './trading-modal.component.css'
})
export class TradingModalComponent implements OnChanges, OnDestroy {
  @Input() isOpen: boolean = false;
  @Input() selectedAsset: Asset | null = null;
  @Input() accountId: string = '';
  @Input() accountBalance: number | null = null;
  @Input() candleData: CandleResponse[] = [];
  @Input() instrumentId: string = '';
  @Input() apiError: string = '';
  @Output() close = new EventEmitter<void>();
  /** Emitted once an order is accepted (and filled, for market orders) so the parent can refresh holdings and cash. */
  @Output() orderCompleted = new EventEmitter<void>();

  selectedTimeframe: string = '3M';
  loadingCandles: boolean = true;

  depthLevels: DepthLevel[] = [];

  timeframes = [
    { label: '1D', days: 1 },
    { label: '1W', days: 7 },
    { label: '1M', days: 30 },
    { label: '3M', days: 90 },
    { label: '1Y', days: 365 }
  ];

  isPreviewOpen: boolean = false;
  isSuccessOpen: boolean = false;
  isErrorOpen: boolean = false;
  errorMessage: string = '';
  errorDetails: string = '';
  /** True from submission until the server reports the order FILLED or REJECTED. */
  isSubmitting: boolean = false;
  /** Price the server actually executed the last order at. */
  filledPrice: number = 0;
  /** The filled order and its fill, shown in the success popup; fee and time arrive with the fill. */
  filledOrder: OrderResponse | null = null;
  filledFee: number | null = null;
  filledAt: Date | null = null;
  /** Cash before the last submitted order, so the success popup can show cash after it. */
  balanceBeforeOrder: number | null = null;
  private orderStatusSubscription: Subscription | null = null;
  pendingTicket: OrderTicketValue | null = null;

  constructor(private apiService: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes['candleData']) {
      const newData = changes['candleData'].currentValue as CandleResponse[];
      if (newData && newData.length > 0) {
        this.loadingCandles = false;
      }
    }
    if (changes['instrumentId'] && this.instrumentId) {
      this.loadMarketDepth();
    }
  }

  loadMarketDepth() {
    this.apiService.getMarketDepth(this.instrumentId).subscribe({
      next: (depth: DepthLevel[]) => {
        this.depthLevels = depth;
        this.cdr.detectChanges();
        console.log('Market depth loaded:', depth.length, 'levels');
      },
      error: (e: any) => console.error('Failed to load market depth:', e)
    });
  }

  selectTimeframe(timeframeLabel: string) {
    this.loadCandles(timeframeLabel);
  }

  loadCandles(timeframeLabel: string) {
    const timeframe = this.timeframes.find(tf => tf.label === timeframeLabel);
    if (!timeframe) return;

    this.loadingCandles = true;
    this.selectedTimeframe = timeframeLabel;

    this.apiService.getCandles(this.instrumentId, timeframe.days).subscribe({
      next: (candles: CandleResponse[]) => {
        this.candleData = candles;
        this.loadingCandles = false;
        this.cdr.detectChanges();
      },
      error: (e: any) => {
        console.error('Failed to load candles:', e);
        this.loadingCandles = false;
      }
    });
  }

  closeModal() {
    this.close.emit();
  }

  onOrderPreviewed(ticket: OrderTicketValue) {
    this.pendingTicket = ticket;
    this.isPreviewOpen = true;
  }

  closePreview() {
    this.isPreviewOpen = false;
  }

  editOrder() {
    this.isPreviewOpen = false;
  }

  confirmOrder() {
    const ticket = this.pendingTicket;
    if (!ticket) return;

    const validationError = this.validateTicket(ticket);
    if (validationError) {
      this.errorMessage = validationError;
      this.isErrorOpen = true;
      return;
    }

    if (!ticket.agreedToTerms) {
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

    this.isPreviewOpen = false;
    this.isSubmitting = true;
    this.balanceBeforeOrder = this.accountBalance;

    // The server accepts the order, then prices and executes it against the live quote (BR-06/BR-08)
    const isLimit = ticket.orderType === 'limit';
    this.apiService.placeOrder(
      this.accountId,
      this.selectedAsset.instrumentId,
      ticket.orderSide.toUpperCase(),
      ticket.quantity.toString(),
      undefined,
      isLimit ? 'LIMIT' : 'MARKET',
      isLimit ? ticket.limitPrice : undefined
    ).subscribe({
      next: (response: OrderResponse) => this.followOrder(response.orderId),
      error: (error: any) => {
        this.isSubmitting = false;
        console.error('Order placement failed:', error);

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

  private validateTicket(ticket: OrderTicketValue): string {
    if (!this.selectedAsset) return 'Select an asset before placing an order.';
    if (!Number.isFinite(ticket.quantity) || ticket.quantity <= 0) {
      return 'Quantity must be greater than zero.';
    }
    if (!Number.isFinite(this.selectedAsset.price) || this.selectedAsset.price <= 0) {
      return 'The selected asset does not have a valid market price.';
    }
    if (ticket.orderType === 'limit' && (!Number.isFinite(ticket.limitPrice) || ticket.limitPrice <= 0)) {
      return 'Limit price must be greater than zero.';
    }
    if (ticket.orderSide === 'sell' && ticket.quantity > this.selectedAsset.holdings) {
      return `You can sell at most ${this.selectedAsset.holdings} shares.`;
    }
    const price = ticket.orderType === 'limit' ? ticket.limitPrice : this.selectedAsset.price;
    const total = ticket.quantity * price;
    if (ticket.orderSide === 'buy' && this.accountBalance !== null &&
        (!Number.isFinite(this.accountBalance) || total > this.accountBalance)) {
      return `This order exceeds your available balance of $${this.accountBalance.toFixed(2)}.`;
    }
    return '';
  }

  /** Follow an accepted order until the server reports it FILLED or REJECTED (BR-07). */
  private followOrder(orderId: string) {
    this.orderStatusSubscription?.unsubscribe();
    this.orderStatusSubscription = timer(ORDER_STATUS_FIRST_CHECK_MS, ORDER_STATUS_INTERVAL_MS).pipe(
      // A failed status check is retried on the next tick
      switchMap(() => this.apiService.getOrder(orderId).pipe(catchError(() => EMPTY))),
      filter(order => order.status !== 'ACCEPTED' && order.status !== 'PENDING'),
      take(1),
      timeout({ first: ORDER_STATUS_TIMEOUT_MS })
    ).subscribe({
      next: (order) => this.onOrderOutcome(order),
      error: () => {
        this.isSubmitting = false;
        this.errorMessage = 'Your order was received and is still being executed.';
        this.errorDetails = 'Check Activity in a moment to see whether it filled.';
        this.isErrorOpen = true;
        this.orderCompleted.emit();
        this.cdr.detectChanges();
      }
    });
  }

  private onOrderOutcome(order: OrderResponse) {
    this.isSubmitting = false;
    if (order.status === 'FILLED') {
      this.filledPrice = Number(order.fillPrice) || 0;
      this.filledOrder = order;
      this.filledFee = null;
      this.filledAt = null;
      this.loadFillDetails(order);
      this.isSuccessOpen = true;
    } else {
      this.errorMessage = 'Your order was not executed.';
      this.errorDetails = order.rejectionReason || 'The order was rejected.';
      this.isErrorOpen = true;
    }
    // Refresh cash and holdings either way
    this.orderCompleted.emit();
    this.cdr.detectChanges();
  }

  ngOnDestroy() {
    this.orderStatusSubscription?.unsubscribe();
  }

  closeErrorModal() {
    this.isErrorOpen = false;
  }

  onSuccessClose() {
    this.isSuccessOpen = false;
    this.closeModal();
  }

  /** The fill carries the actual fee and execution time; fills are listed newest first. */
  private loadFillDetails(order: OrderResponse) {
    this.apiService.getFills(order.accountId, 20, 0).subscribe({
      next: (page) => {
        const fill = page.items.find(item => item.orderId === order.orderId);
        if (!fill || this.filledOrder?.orderId !== order.orderId) return;
        const fee = Number(fill.fee);
        this.filledFee = Number.isFinite(fee) ? fee : null;
        this.filledAt = toDate(fill.executedAt);
        this.cdr.detectChanges();
      },
      error: (error) => console.error('Failed to load fill details:', error)
    });
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
