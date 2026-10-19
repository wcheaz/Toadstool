import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from '../core/auth.service';
import { ApiService } from '../core/api/api.service';
import { ApiTimestamp, PaginatedResponse } from '../core/api/api.models';
import { SidebarComponent } from '../homepage/components/sidebar/sidebar.component';
import { formatDateTime, humanize } from '../admin-dashboard/admin-format';

type AuditTab = 'logins' | 'trades' | 'access' | 'cash';

interface AuditRow {
  at: ApiTimestamp;
  who: string;
  whoDetail: string;
  clientId: string | null;
  event: string;
  details: string;
  status: string;
  tone: 'ok' | 'warn' | 'neutral';
}

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  templateUrl: './admin-audit.component.html',
  styleUrls: ['../admin-dashboard/admin-dashboard.component.css', './admin-audit.component.css']
})
export class AdminAuditComponent implements OnInit {
  readonly pageSize = 25;
  readonly adminName: string;
  readonly tabs: Array<{ key: AuditTab; label: string }> = [
    { key: 'logins', label: 'Sign-ins' },
    { key: 'trades', label: 'Trade events' },
    { key: 'access', label: 'Admin access' },
    { key: 'cash', label: 'Cash flow' }
  ];
  readonly formatDateTime = formatDateTime;

  tab: AuditTab = 'logins';
  outcomeFilter = '';
  emailFilter = '';
  actionFilter = '';
  clientFilter = '';
  clientFilterName = '';

  rows: AuditRow[] = [];
  totalCount = 0;
  offset = 0;
  loading = false;
  error = '';

  constructor(
    private authService: AuthService,
    private apiService: ApiService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {
    this.adminName = this.authService.getClientName();
  }

  ngOnInit(): void {
    const query = this.route.snapshot.queryParamMap;
    const tab = query.get('tab') as AuditTab | null;
    if (tab && this.tabs.some((item) => item.key === tab)) {
      this.tab = tab;
    }
    this.clientFilter = query.get('clientId') ?? '';
    this.clientFilterName = query.get('client') ?? '';
    this.load();
  }

  get pageStart(): number {
    return this.totalCount === 0 ? 0 : this.offset + 1;
  }

  get pageEnd(): number {
    return Math.min(this.offset + this.rows.length, this.totalCount);
  }

  get hasPrevious(): boolean {
    return this.offset > 0;
  }

  get hasNext(): boolean {
    return this.offset + this.pageSize < this.totalCount;
  }

  selectTab(tab: AuditTab): void {
    if (tab !== this.tab) {
      this.tab = tab;
      this.offset = 0;
      this.rows = [];
      this.load();
    }
  }

  applyFilters(): void {
    this.offset = 0;
    this.load();
  }

  clearClientFilter(): void {
    this.clientFilter = '';
    this.clientFilterName = '';
    this.router.navigate([], { relativeTo: this.route, queryParams: { tab: this.tab }, replaceUrl: true });
    this.applyFilters();
  }

  previousPage(): void {
    if (this.hasPrevious) {
      this.offset = Math.max(0, this.offset - this.pageSize);
      this.load();
    }
  }

  nextPage(): void {
    if (this.hasNext) {
      this.offset += this.pageSize;
      this.load();
    }
  }

  openClient(row: AuditRow): void {
    if (row.clientId) {
      this.router.navigate(['/admin-client', row.clientId]);
    }
  }

  logout(): void {
    this.authService.logout();
  }

  private load(): void {
    const tab = this.tab;
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();
    this.request(tab).subscribe({
      next: (page) => {
        if (tab !== this.tab) {
          return;
        }
        this.rows = page.items;
        this.totalCount = page.pagination.totalCount;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load audit events:', err);
        this.rows = [];
        this.totalCount = 0;
        this.loading = false;
        this.error = 'Could not load audit events. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  private request(tab: AuditTab): Observable<PaginatedResponse<AuditRow>> {
    const clientId = this.clientFilter || undefined;
    switch (tab) {
      case 'logins':
        return this.apiService.getAuditLoginEvents(
          { outcome: this.outcomeFilter, emailAttempted: this.emailFilter, clientId }, this.pageSize, this.offset
        ).pipe(map((page) => ({
          ...page,
          items: page.items.map((event) => ({
            at: event.occurredAt,
            who: event.clientName ?? 'Unknown client',
            whoDetail: event.emailAttempted,
            clientId: event.clientId,
            event: event.outcome === 'SUCCESS' ? 'Successful sign-in' : 'Failed sign-in',
            details: this.reason(event.details),
            status: event.outcome,
            tone: event.outcome === 'SUCCESS' ? 'ok' as const : 'warn' as const
          }))
        })));
      case 'trades':
        return this.apiService.getAuditTradeEvents(
          { action: this.actionFilter, clientId }, this.pageSize, this.offset
        ).pipe(map((page) => ({
          ...page,
          items: page.items.map((event) => ({
            at: event.occurredAt,
            who: event.clientName ?? 'Unknown client',
            whoDetail: event.clientId ? event.clientId.slice(0, 8) : '',
            clientId: event.clientId,
            event: humanize(event.action),
            details: `${humanize(event.entityType)} ${event.entityId.slice(0, 8)}${this.tradeSummary(event.details)}`,
            status: 'RECORDED',
            tone: 'neutral' as const
          }))
        })));
      case 'access':
        return this.apiService.getAuditAccessEvents({ clientId }, this.pageSize, this.offset)
          .pipe(map((page) => ({
            ...page,
            items: page.items.map((event) => ({
              at: event.occurredAt,
              who: event.adminEmail,
              whoDetail: 'Admin',
              clientId: event.clientId,
              event: humanize(event.action),
              details: event.clientName ? `Client: ${event.clientName}` : 'Platform data',
              status: 'LOGGED',
              tone: 'neutral' as const
            }))
          })));
      case 'cash':
        return this.apiService.getAuditCashEvents({ clientId }, this.pageSize, this.offset)
          .pipe(map((page) => ({
            ...page,
            items: page.items.map((event) => ({
              at: event.occurredAt,
              who: event.clientName,
              whoDetail: event.accountName,
              clientId: event.clientId,
              event: humanize(event.eventType),
              details: this.formatAmount(event.amount),
              status: 'COMPLETED',
              tone: 'neutral' as const
            }))
          })));
    }
  }

  private reason(details: string): string {
    const parsed = this.parse(details);
    return parsed?.reason ?? '--';
  }

  private tradeSummary(details: string): string {
    const parsed = this.parse(details);
    if (parsed?.side && parsed?.quantity) {
      return ` · ${humanize(parsed.side)} ${parsed.quantity}`;
    }
    return '';
  }

  private formatAmount(amount: number): string {
    return Math.abs(amount).toLocaleString('en-US', {
      style: 'currency',
      currency: 'USD'
    });
  }

  private parse(details: string): any {
    try {
      return JSON.parse(details || '{}');
    } catch {
      return null;
    }
  }
}
