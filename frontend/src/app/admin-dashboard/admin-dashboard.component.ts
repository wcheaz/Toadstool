import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ApiService } from '../core/api/api.service';
import { finalize } from 'rxjs/operators';
import { AdminClientSummary, Insight, PlatformOverview } from '../core/api/api.models';
import { SidebarComponent } from '../homepage/components/sidebar/sidebar.component';
import { formatDateTime, formatMoney, humanize } from './admin-format';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  readonly pageSize = 25;
  readonly adminName: string;

  searchTerm = '';
  appliedSearchTerm = '';
  statusFilter = '';

  clients: AdminClientSummary[] = [];
  totalCount = 0;
  offset = 0;
  loading = false;
  error = '';

  overview: PlatformOverview | null = null;
  visibleInsights: Insight[] = [];
  timeframe: 'day' | 'week' | 'month' = 'month';
  loadedAt: Date | null = null;
  private clientsRequestId = 0;

  readonly formatMoney = formatMoney;
  readonly formatDateTime = formatDateTime;
  readonly humanize = humanize;

  constructor(
    private authService: AuthService,
    private apiService: ApiService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.adminName = this.authService.getClientName();
  }

  ngOnInit(): void {
    this.loadClients();
    this.loadInsights();
  }

  get pageStart(): number {
    return this.totalCount === 0 ? 0 : this.offset + 1;
  }

  get pageEnd(): number {
    return Math.min(this.offset + this.clients.length, this.totalCount);
  }

  get hasPrevious(): boolean {
    return this.offset > 0;
  }

  get hasNext(): boolean {
    return this.offset + this.pageSize < this.totalCount;
  }

  applySearch(): void {
    this.appliedSearchTerm = this.searchTerm.trim();
    this.offset = 0;
    this.loadClients();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.applySearch();
  }

  onStatusChange(): void {
    this.offset = 0;
    this.loadClients();
  }

  previousPage(): void {
    if (this.hasPrevious) {
      this.offset = Math.max(0, this.offset - this.pageSize);
      this.loadClients();
    }
  }

  nextPage(): void {
    if (this.hasNext) {
      this.offset += this.pageSize;
      this.loadClients();
    }
  }

  logout(): void {
    this.authService.logout();
  }

  viewDetails(client: AdminClientSummary): void {
    this.router.navigate(['/admin-client', client.clientId]);
  }

  onTimeframeChange(): void {
    this.loadInsights();
  }

  private loadClients(): void {
    const requestId = ++this.clientsRequestId;
    this.loading = true;
    this.error = '';
    this.apiService.searchAdminClients(this.appliedSearchTerm, this.statusFilter, this.pageSize, this.offset).pipe(
      finalize(() => {
        if (requestId === this.clientsRequestId) {
          this.loading = false;
          this.cdr.detectChanges();
        }
      })
    ).subscribe({
      next: (page) => {
        if (requestId !== this.clientsRequestId) {
          return;
        }
        this.clients = page.items;
        this.totalCount = page.pagination.totalCount;
        this.loadedAt = new Date();
        this.cdr.detectChanges();
      },
      error: (err) => {
        if (requestId !== this.clientsRequestId) {
          return;
        }
        console.error('Failed to load clients:', err);
        this.clients = [];
        this.totalCount = 0;
        this.error = err?.status === 403
          ? 'Your session does not have admin access.'
          : 'Could not load clients. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  private loadInsights(): void {
    this.apiService.getInsights(this.timeframe).subscribe({
      next: (response) => {
        this.overview = response.overview;
        this.visibleInsights = response.insights.filter((insight) =>
          insight.name !== 'most-traded-instrument' &&
          insight.name !== 'gross-volume-30d' &&
          insight.name !== 'open-orders' &&
          insight.name !== 'failed-logins-24h'
        );
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load platform insights:', err)
    });
  }
}
