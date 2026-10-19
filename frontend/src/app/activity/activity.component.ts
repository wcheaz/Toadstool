import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';
import { Instrument, OrderResponse } from '../core/api/api.models';

interface Transaction {
  id: string;
  date: string;
  asset: string;
  orderType: string;
  quantity: string;
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
export class ActivityComponent implements OnInit {
  readonly tabs: ReadonlyArray<{ key: ActivityTab; label: string }> = [
    { key: 'orders', label: 'Order activity' },
    { key: 'cash', label: 'Deposits & withdrawals' }
  ];
  tab: ActivityTab = 'orders';
  transactions: Transaction[] = [];
  readonly pageSize = 20;
  currentPage = 1;
  totalOrderCount = 0;

  stats = {
    allTimeExecutions: 'Loading...'
  };

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    console.log('[Activity] Initializing activity page');
    this.loadOrders();
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
      status: order.status
    };
  }

  private formatSubmittedAt(submittedAt: string | number): string {
    const numericTimestamp = typeof submittedAt === 'number'
      ? submittedAt
      : Number(submittedAt);
    const timestampInMilliseconds = Number.isFinite(numericTimestamp)
      ? numericTimestamp < 1_000_000_000_000
        ? numericTimestamp * 1_000
        : numericTimestamp
      : NaN;
    const parsedDate = Number.isFinite(timestampInMilliseconds)
      ? new Date(timestampInMilliseconds)
      : new Date(submittedAt);

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
