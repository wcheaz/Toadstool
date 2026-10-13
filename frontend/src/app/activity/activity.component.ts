import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { timeout } from 'rxjs/operators';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';

interface Transaction {
  id: string;
  date: string;
  asset: string;
  orderType: string;
  amount: number;
  status: string;
}

@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity.component.html',
  styleUrl: './activity.component.css'
})
export class ActivityComponent implements OnInit {
  transactions: Transaction[] = [];
  readonly pageSize = 20;
  currentPage = 1;

  stats = {
    allTimeExecutions: 'Loading...',
    successRate: '99.42%'
  };

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    console.log('[Activity] Initializing activity page');
    this.initializeTransactions();
    this.loadTransactionCount();
  }

  private loadTransactionCount(): void {
    const accountId = this.authService.getAccountId();
    console.log('[Activity] Account ID:', accountId);
    if (!accountId) {
      console.warn('[Activity] No account ID found; cannot load transaction count');
      this.stats.allTimeExecutions = 'Unavailable';
      this.cdr.detectChanges();
      return;
    }

    console.log('[Activity] Loading transaction count from backend');
    this.apiService.getOrders(accountId, 1, 0).pipe(timeout(5000)).subscribe({
      next: (response) => {
        console.log('[Activity] Orders response:', response);
        const totalCount = response?.pagination?.totalCount;
        this.stats.allTimeExecutions = typeof totalCount === 'number'
          ? totalCount.toLocaleString()
          : 'Unavailable';
        this.cdr.detectChanges();
        console.log('[Activity] Transaction count:', this.stats.allTimeExecutions);
      },
      error: (error) => {
        console.error('[Activity] Failed to load transaction count:', error);
        this.stats.allTimeExecutions = 'Unavailable';
        this.cdr.detectChanges();
      }
    });
  }

  private initializeTransactions() {
    this.transactions = [
      {
        id: 'TX-RR2014-HEMS',
        date: 'Oct 25, 2024 at 14:23 UTC',
        asset: 'HEMS',
        orderType: 'BUY (Standard)',
        amount: 1423.84,
        status: 'COMPLETED'
      },
      {
        id: 'TX-762810-USDT',
        date: 'Oct 25, 2024 at 13:45 UTC',
        asset: 'USDT',
        orderType: 'DEPOSIT',
        amount: 5000.00,
        status: 'COMPLETED'
      },
      {
        id: 'TX-210245-BTC',
        date: 'Oct 24, 2024 at 12:15 UTC',
        asset: 'BTC',
        orderType: 'SELL (Limit)',
        amount: 10084.50,
        status: 'PENDING'
      },
      {
        id: 'TX-182854-HEMS',
        date: 'Oct 24, 2024 at 11:22 UTC',
        asset: 'HEMS',
        orderType: 'BUY (Standard)',
        amount: 2844.00,
        status: 'REJECTED'
      }
    ];
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  get paginatedTransactions(): Transaction[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.transactions.slice(startIndex, startIndex + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.transactions.length / this.pageSize);
  }

  goToPreviousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }
}
