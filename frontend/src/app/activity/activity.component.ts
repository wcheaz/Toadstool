import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

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
    allTimeExecutions: '2,841',
    successRate: '99.42%'
  };

  ngOnInit() {
    this.initializeTransactions();
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
