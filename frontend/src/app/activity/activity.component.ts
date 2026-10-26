import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EMPTY, forkJoin, of, Subscription } from 'rxjs';
import { catchError, expand, reduce, timeout } from 'rxjs/operators';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';
import { CashEventResponse, Instrument, OrderResponse } from '../core/api/api.models';

interface Transaction {
  id: string;
  date: string;
  asset: string;
  orderType: string;
  quantity: string;
  status: string;
  /** Why the order was rejected, shown on hover. */
  reason: string | null;
}

/** While an order is still being executed, the blotter re-checks it at this interval (BR-07). */
const PENDING_REFRESH_MS = 3000;

interface CashActivity {
  id: string;
  date: string;
  type: string;
  amount: string;
  status: string;
}

type ActivityTab = 'orders' | 'cash';

@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity.component.html',
  styleUrl: './activity.component.css'
})
export class ActivityComponent implements OnInit, OnDestroy {
  readonly tabs: ReadonlyArray<{ key: ActivityTab; label: string }> = [
    { key: 'orders', label: 'Order activity' },
    { key: 'cash', label: 'Deposits & withdrawals' }
  ];
  tab: ActivityTab = 'orders';
  transactions: Transaction[] = [];
  cashActivity: CashActivity[] = [];
  cashActivityLoading = false;
  cashActivityError = '';
  private cashActivityLoaded = false;
  readonly pageSize = 20;
  currentPage = 1;
  totalOrderCount = 0;

  stats = {
    allTimeExecutions: 'Loading...',
    monthlyExecutions: 'Loading...',
    weeklyExecutions: 'Loading...',
    dailyExecutions: 'Loading...'
  };
  private statsSubscription?: Subscription;

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  private pendingRefresh: ReturnType<typeof setTimeout> | null = null;

  ngOnInit() {
    console.log('[Activity] Initializing activity page');
    this.loadOrders();
    this.loadPeriodStats();
  }

  ngOnDestroy() {
    this.clearPendingRefresh();
    this.statsSubscription?.unsubscribe();
  }

  private loadPeriodStats(): void {
    const accountId = this.authService.getAccountId();
    if (!accountId) {
      console.warn('[Activity] No account ID found; cannot load transaction statistics');
      this.setPeriodStatsUnavailable();
      return;
    }

    const now = Date.now();
    const dayMs = 24 * 60 * 60 * 1000;
    const monthStart = now - 30 * dayMs;
    const weekStart = now - 7 * dayMs;
    const dayStart = now - dayMs;
    const limit = 1000;

    this.statsSubscription = this.apiService.getOrders(accountId, limit, 0).pipe(
      timeout(5000),
      // Orders are newest first; history older than 30 days cannot contribute.
      expand(response => {
        if (!response.pagination.hasMore ||
            response.items.some(order => this.parseTimestamp(order.submittedAt).getTime() < monthStart)) {
          return EMPTY;
        }
        if (response.items.length === 0) {
          throw new Error('Order pagination returned an empty page with more results');
        }
        return this.apiService.getOrders(
          accountId, limit, response.pagination.offset + response.items.length
        ).pipe(timeout(5000));
      }),
      reduce((counts, response) => {
        for (const order of response.items) {
          const timestamp = this.parseTimestamp(order.submittedAt).getTime();
          if (!Number.isFinite(timestamp)) {
            throw new Error(`Invalid submitted timestamp for order ${order.orderId}`);
          }
          if (timestamp > now) {
            continue;
          }
          if (timestamp >= monthStart) counts.monthly++;
          if (timestamp >= weekStart) counts.weekly++;
          if (timestamp >= dayStart) counts.daily++;
        }
        return counts;
      }, { monthly: 0, weekly: 0, daily: 0 })
    ).subscribe({
      next: counts => {
        this.stats.monthlyExecutions = counts.monthly.toLocaleString();
        this.stats.weeklyExecutions = counts.weekly.toLocaleString();
        this.stats.dailyExecutions = counts.daily.toLocaleString();
        this.cdr.detectChanges();
      },
      error: error => {
        console.error('[Activity] Failed to load transaction statistics:', error);
        this.setPeriodStatsUnavailable();
      }
    });
  }

  private setPeriodStatsUnavailable(): void {
    this.stats.monthlyExecutions = 'Unavailable';
    this.stats.weeklyExecutions = 'Unavailable';
    this.stats.dailyExecutions = 'Unavailable';
    this.cdr.detectChanges();
  }

  /** Re-load the page of orders while any of them is still awaiting execution. */
  private scheduleRefreshIfPending(): void {
    this.clearPendingRefresh();
    if (this.transactions.some(tx => tx.status === 'ACCEPTED' || tx.status === 'PENDING')) {
      this.pendingRefresh = setTimeout(() => this.loadOrders(this.currentPage), PENDING_REFRESH_MS);
    }
  }

  private clearPendingRefresh(): void {
    if (this.pendingRefresh !== null) {
      clearTimeout(this.pendingRefresh);
      this.pendingRefresh = null;
    }
  }

  private loadOrders(page: number = this.currentPage): void {
    const accountId = this.authService.getAccountId();
    console.log('[Activity] Account ID:', accountId);
    if (!accountId) {
      console.warn('[Activity] No account ID found; cannot load orders');
      this.stats.allTimeExecutions = 'Unavailable';
      this.transactions = [];
      this.totalOrderCount = 0;
      this.cdr.detectChanges();
      return;
    }

    console.log('[Activity] Loading orders from backend');
    const offset = (page - 1) * this.pageSize;
    forkJoin({
      orders: this.apiService.getOrders(accountId, this.pageSize, offset),
      instruments: this.apiService.getInstruments(1000, 0).pipe(
        catchError(() => of(null))
      )
    }).pipe(timeout(5000)).subscribe({
      next: ({ orders, instruments }) => {
        console.log('[Activity] Orders response:', orders);
        const totalCount = orders?.pagination?.totalCount;
        this.totalOrderCount = typeof totalCount === 'number' ? totalCount : 0;
        this.stats.allTimeExecutions = this.totalOrderCount.toLocaleString();
        this.currentPage = page;
        const instrumentNames = this.createInstrumentNameMap(instruments?.items ?? []);
        this.transactions = orders.items.map((order) => this.mapOrderToTransaction(order, instrumentNames));
        this.cdr.detectChanges();
        this.scheduleRefreshIfPending();
        console.log('[Activity] Transaction count:', this.stats.allTimeExecutions);
      },
      error: (error) => {
        console.error('[Activity] Failed to load orders:', error);
        this.stats.allTimeExecutions = 'Unavailable';
        this.transactions = [];
        this.totalOrderCount = 0;
        this.cdr.detectChanges();
      }
    });
  }

  private createInstrumentNameMap(instruments: Instrument[]): Map<string, string> {
    return new Map(instruments.map((instrument) => [instrument.instrumentId, instrument.name]));
  }

  private mapOrderToTransaction(order: OrderResponse, instrumentNames: Map<string, string>): Transaction {
    return {
      id: order.orderId,
      date: this.formatSubmittedAt(order.submittedAt),
      asset: instrumentNames.get(order.instrumentId) ?? order.instrumentId,
      orderType: order.side,
      quantity: this.formatQuantity(order.quantity),
      status: order.status,
      reason: order.rejectionReason ?? null
    };
  }

  private parseTimestamp(submittedAt: string | number): Date {
    const numericTimestamp = typeof submittedAt === 'number'
      ? submittedAt
      : Number(submittedAt);
    const timestampInMilliseconds = Number.isFinite(numericTimestamp)
      ? numericTimestamp < 1_000_000_000_000
        ? numericTimestamp * 1_000
        : numericTimestamp
      : NaN;
    return Number.isFinite(timestampInMilliseconds)
      ? new Date(timestampInMilliseconds)
      : new Date(submittedAt);
  }

  private formatSubmittedAt(submittedAt: string | number): string {
    const parsedDate = this.parseTimestamp(submittedAt);
    return Number.isNaN(parsedDate.getTime())
      ? String(submittedAt)
      : parsedDate.toLocaleString();
  }

  private formatQuantity(quantity: string): string {
    const numericQuantity = Number(quantity);
    return Number.isFinite(numericQuantity)
      ? numericQuantity.toLocaleString('en-US', { maximumFractionDigits: 4 })
      : quantity;
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  get paginatedTransactions(): Transaction[] {
    return this.transactions;
  }

  selectTab(tab: ActivityTab): void {
    this.tab = tab;
    if (tab === 'cash' && !this.cashActivityLoaded && !this.cashActivityLoading) {
      this.loadCashActivity();
    }
  }

  private loadCashActivity(): void {
    const accountId = this.authService.getAccountId();
    if (!accountId) {
      this.cashActivityError = 'Cash activity is unavailable because no account is signed in.';
      return;
    }

    this.cashActivityLoading = true;
    this.cashActivityError = '';
    this.apiService.getCashEvents(accountId, 1000, 0).pipe(timeout(5000)).subscribe({
      next: (response) => {
        this.cashActivity = response.items
          .filter((event) => event.eventType === 'DEPOSIT' || event.eventType === 'WITHDRAWAL')
          .map((event) => this.mapCashEventToActivity(event));
        this.cashActivityLoaded = true;
        this.cashActivityLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('[Activity] Failed to load cash activity:', error);
        this.cashActivityError = 'Unable to load cash activity. Please try again later.';
        this.cashActivityLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private mapCashEventToActivity(event: CashEventResponse): CashActivity {
    const isWithdrawal = event.eventType === 'WITHDRAWAL';
    const amount = Math.abs(Number(event.amount));
    return {
      id: event.cashEventId,
      date: this.formatSubmittedAt(event.occurredAt),
      type: isWithdrawal ? 'Withdrawal' : 'Deposit',
      amount: Number.isFinite(amount) ? amount.toLocaleString('en-US', {
        style: 'currency',
        currency: 'USD'
      }) : String(event.amount),
      status: 'Completed'
    };
  }

  get totalPages(): number {
    return Math.ceil(this.totalOrderCount / this.pageSize);
  }

  goToPreviousPage(): void {
    if (this.currentPage > 1) {
      this.loadOrders(this.currentPage - 1);
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.loadOrders(this.currentPage + 1);
    }
  }
}
