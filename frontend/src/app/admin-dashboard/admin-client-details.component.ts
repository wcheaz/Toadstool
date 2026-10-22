import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../core/auth.service';
import { ApiService } from '../core/api/api.service';
import {
  AdminClientAccount,
  AdminClientDetail,
  CashEventResponse,
  FillResponse,
  HoldingsResponse,
  Instrument,
  LoginEventRow,
  OrderResponse,
  QuoteResponse,
  TradeEventRow
} from '../core/api/api.models';
import { SidebarComponent } from '../homepage/components/sidebar/sidebar.component';
import { computePortfolioChange, PortfolioChange, PortfolioHistoryService, PortfolioPoint } from '../shared/services/portfolio-history.service';
import {
  formatDate,
  formatDateTime,
  formatMoney,
  formatPercent,
  formatQuantity,
  humanize,
  initials,
  toDate
} from './admin-format';

interface HoldingRow {
  instrumentId: string;
  symbol: string;
  name: string;
  quantity: number;
  price: number | null;
  value: number | null;
  changePercent: number | null;
  allocation: number | null;
}

interface JournalRow {
  orderId: string;
  submittedAt: Date | null;
  symbol: string;
  side: string;
  quantity: number;
  price: number | null;
  gross: number | null;
  fee: number | null;
  net: number | null;
  status: string;
}

interface ActivityRow {
  at: Date | null;
  activity: string;
  details: string;
  status: string;
  tone: 'ok' | 'warn' | 'neutral';
}

type ChartPoint = PortfolioPoint;

const BATCH_QUOTE_LIMIT = 25;
const JOURNAL_PAGE_SIZE = 10;

@Component({
  selector: 'app-admin-client-details',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  templateUrl: './admin-client-details.component.html',
  styleUrl: './admin-client-details.component.css'
})
export class AdminClientDetailsComponent implements OnInit {
  readonly adminName: string;
  readonly chartRanges = [7, 30, 90];

  readonly formatMoney = formatMoney;
  readonly formatQuantity = formatQuantity;
  readonly formatPercent = formatPercent;
  readonly formatDateTime = formatDateTime;
  readonly formatDate = formatDate;
  readonly humanize = humanize;

  clientId = '';
  detail: AdminClientDetail | null = null;
  selectedAccountId = '';
  loading = true;
  accountLoading = false;
  error = '';

  holdings: HoldingRow[] = [];
  journal: JournalRow[] = [];
  activities: ActivityRow[] = [];
  holdingsValue = 0;
  pricedHoldings = true;

  filledTrades = 0;
  grossVolume = 0;
  feesPaid = 0;
  openOrders = 0;

  assetFilter = '';
  sideFilter = '';
  statusFilter = '';
  journalPage = 0;

  chartDays = 30;
  chartLoading = false;
  chartPoints: ChartPoint[] = [];
  chartLine = '';
  chartArea = '';

  private instruments = new Map<string, Instrument>();
  private cashEvents: CashEventResponse[] = [];
  private fills: FillResponse[] = [];
  private orders: OrderResponse[] = [];
  private loginEvents: LoginEventRow[] = [];
  private tradeEvents: TradeEventRow[] = [];

  constructor(
    private authService: AuthService,
    private apiService: ApiService,
    private router: Router,
    private route: ActivatedRoute,
    private portfolioHistory: PortfolioHistoryService,
    private cdr: ChangeDetectorRef
  ) {
    this.adminName = this.authService.getClientName();
  }

  ngOnInit(): void {
    this.clientId = this.route.snapshot.paramMap.get('clientId') ?? '';
    if (!this.clientId) {
      this.fail('No client selected.');
      return;
    }

    forkJoin({
      detail: this.apiService.getAdminClient(this.clientId),
      instruments: this.apiService.getInstruments(1000, 0).pipe(
        map((page) => page.items),
        catchError(() => of([] as Instrument[]))
      ),
      logins: this.apiService.getClientLoginEvents(this.clientId, 20).pipe(
        map((page) => page.items),
        catchError(() => of([] as LoginEventRow[]))
      ),
      trades: this.apiService.getClientTradeEvents(this.clientId, 20).pipe(
        map((page) => page.items),
        catchError(() => of([] as TradeEventRow[]))
      )
    }).subscribe({
      next: ({ detail, instruments, logins, trades }) => {
        this.detail = detail;
        this.instruments = new Map(instruments.map((instrument) => [instrument.instrumentId, instrument]));
        this.loginEvents = logins;
        this.tradeEvents = trades;
        this.loading = false;
        const requested = this.route.snapshot.queryParamMap.get('account');
        const account = detail.accounts.find((item) => item.accountId === requested) ?? detail.accounts[0];
        if (account) {
          this.selectAccount(account.accountId);
        } else {
          this.buildActivities();
          this.cdr.detectChanges();
        }
      },
      error: (err) => this.fail(err?.status === 404 ? 'This client does not exist.' : 'Could not load this client.')
    });
  }

  // ---- Template helpers ----

  get client() {
    return this.detail?.client ?? null;
  }

  get selectedAccount(): AdminClientAccount | null {
    return this.detail?.accounts.find((account) => account.accountId === this.selectedAccountId) ?? null;
  }

  get clientInitials(): string {
    return initials(this.client?.email || this.client?.displayName);
  }

  get cashBalance(): number {
    return Number(this.selectedAccount?.balance ?? 0);
  }

  get portfolioValue(): number {
    return this.holdingsValue + this.cashBalance;
  }

  get assetOptions(): string[] {
    return Array.from(new Set(this.journal.map((row) => row.symbol))).sort();
  }

  get filteredJournal(): JournalRow[] {
    return this.journal.filter((row) =>
      (!this.assetFilter || row.symbol === this.assetFilter)
      && (!this.sideFilter || row.side === this.sideFilter)
      && (!this.statusFilter || row.status === this.statusFilter));
  }

  get journalPageRows(): JournalRow[] {
    const start = this.journalPage * JOURNAL_PAGE_SIZE;
    return this.filteredJournal.slice(start, start + JOURNAL_PAGE_SIZE);
  }

  get journalPageCount(): number {
    return Math.max(1, Math.ceil(this.filteredJournal.length / JOURNAL_PAGE_SIZE));
  }

  get journalRangeLabel(): string {
    const total = this.filteredJournal.length;
    if (total === 0) {
      return 'No orders match these filters';
    }
    const start = this.journalPage * JOURNAL_PAGE_SIZE + 1;
    const end = Math.min(start + JOURNAL_PAGE_SIZE - 1, total);
    return `Showing ${start}–${end} of ${total} orders`;
  }

  /** Investment gain over the chart window, excluding deposits/withdrawals (see computePortfolioChange). */
  get chartChange(): PortfolioChange | null {
    return computePortfolioChange(this.chartPoints, this.cashEvents);
  }

  get chartMax(): number {
    return Math.max(...this.chartPoints.map((point) => point.value));
  }

  get chartMin(): number {
    return Math.min(...this.chartPoints.map((point) => point.value));
  }

  get asOfLabel(): string {
    return formatDateTime(new Date().toISOString());
  }

  onJournalFilterChange(): void {
    this.journalPage = 0;
  }

  previousJournalPage(): void {
    this.journalPage = Math.max(0, this.journalPage - 1);
  }

  nextJournalPage(): void {
    this.journalPage = Math.min(this.journalPageCount - 1, this.journalPage + 1);
  }

  backToClients(): void {
    this.router.navigate(['/admin-dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  // ---- Loading ----

  selectAccount(accountId: string): void {
    this.selectedAccountId = accountId;
    this.accountLoading = true;
    this.assetFilter = '';
    this.sideFilter = '';
    this.statusFilter = '';
    this.journalPage = 0;
    this.router.navigate([], { relativeTo: this.route, queryParams: { account: accountId }, replaceUrl: true });

    forkJoin({
      holdings: this.apiService.getAccountHoldings(accountId).pipe(catchError(() => of(null as HoldingsResponse | null))),
      orders: this.apiService.getOrders(accountId, 1000, 0).pipe(
        map((page) => page.items),
        catchError(() => of([] as OrderResponse[]))
      ),
      fills: this.apiService.getFills(accountId, 1000, 0).pipe(
        map((page) => page.items),
        catchError(() => of([] as FillResponse[]))
      ),
      cashEvents: this.apiService.getCashEvents(accountId, 1000, 0).pipe(
        map((page) => page.items),
        catchError(() => of([] as CashEventResponse[]))
      )
    }).subscribe(({ holdings, orders, fills, cashEvents }) => {
      if (accountId !== this.selectedAccountId) {
        return;
      }
      this.orders = orders;
      this.fills = fills;
      this.cashEvents = cashEvents;
      this.buildJournal();
      this.buildActivities();
      this.loadHoldingPrices(holdings, accountId);
    });
  }

  setChartRange(days: number): void {
    if (days !== this.chartDays) {
      this.chartDays = days;
      this.loadChart();
    }
  }

  private loadHoldingPrices(holdings: HoldingsResponse | null, accountId: string): void {
    const items = holdings?.holdings ?? [];
    const ids = items.map((item) => item.instrumentId);
    this.fetchQuotes(ids).subscribe((quotes) => {
      if (accountId !== this.selectedAccountId) {
        return;
      }
      const rows: HoldingRow[] = items.map((item) => {
        const instrument = this.instruments.get(item.instrumentId);
        const quote = quotes[item.instrumentId];
        const quantity = Number(item.quantity);
        const price = quote && Number.isFinite(quote.price) ? quote.price : null;
        return {
          instrumentId: item.instrumentId,
          symbol: item.symbol ?? instrument?.symbol ?? '--',
          name: item.name ?? instrument?.name ?? '',
          quantity,
          price,
          value: price !== null ? price * quantity : null,
          changePercent: quote && Number.isFinite(quote.changePercent) ? quote.changePercent : null,
          allocation: null
        };
      });
      this.holdingsValue = rows.reduce((sum, row) => sum + (row.value ?? 0), 0);
      this.pricedHoldings = rows.every((row) => row.value !== null);
      rows.forEach((row) => {
        row.allocation = row.value !== null && this.holdingsValue > 0 ? (row.value / this.holdingsValue) * 100 : null;
      });
      this.holdings = rows.sort((a, b) => (b.value ?? 0) - (a.value ?? 0));
      this.accountLoading = false;
      this.cdr.detectChanges();
      this.loadChart();
    });
  }

  private fetchQuotes(ids: string[]): Observable<{ [instrumentId: string]: QuoteResponse }> {
    if (ids.length === 0) {
      return of({});
    }
    const batches: string[][] = [];
    for (let i = 0; i < ids.length; i += BATCH_QUOTE_LIMIT) {
      batches.push(ids.slice(i, i + BATCH_QUOTE_LIMIT));
    }
    return forkJoin(batches.map((batch) => this.apiService.getBatchQuotes(batch).pipe(catchError(() => of({})))))
      .pipe(map((results) => Object.assign({}, ...results)));
  }

  /** Portfolio value per day, reconstructed from fills, cash events and daily closes (see PortfolioHistoryService). */
  private loadChart(): void {
    const accountId = this.selectedAccountId;
    const days = this.chartDays;
    this.chartLoading = true;
    this.cdr.detectChanges();
    this.portfolioHistory.buildHistory({
      days,
      cashBalance: this.cashBalance,
      holdings: this.holdings.map((row) => ({ instrumentId: row.instrumentId, quantity: row.quantity })),
      orders: this.orders,
      fills: this.fills,
      cashEvents: this.cashEvents,
      currentValue: this.pricedHoldings ? this.portfolioValue : null
    }).subscribe((points) => {
      if (accountId !== this.selectedAccountId || days !== this.chartDays) {
        return;
      }
      this.setChart(points);
    });
  }

  private setChart(points: ChartPoint[]): void {
    this.chartPoints = points;
    this.chartLoading = false;
    if (points.length < 2) {
      this.chartLine = '';
      this.chartArea = '';
    } else {
      const min = this.chartMin;
      const span = this.chartMax - min || 1;
      const coords = points.map((point, index) => {
        const x = (index / (points.length - 1)) * 600;
        const y = 130 - ((point.value - min) / span) * 115;
        return `${x.toFixed(1)} ${y.toFixed(1)}`;
      });
      this.chartLine = `M${coords.join(' L')}`;
      this.chartArea = `${this.chartLine} L600 145 L0 145 Z`;
    }
    this.cdr.detectChanges();
  }

  private buildJournal(): void {
    const fillsByOrder = new Map<string, FillResponse>();
    this.fills.forEach((fill) => fillsByOrder.set(fill.orderId, fill));

    this.journal = this.orders.map((order) => {
      const fill = fillsByOrder.get(order.orderId);
      const quantity = Number(order.quantity);
      const price = fill ? Number(fill.price) : null;
      const gross = price !== null ? price * Number(fill!.quantity) : null;
      const fee = fill ? Number(fill.fee ?? 0) : null;
      const net = gross === null || fee === null ? null : order.side === 'BUY' ? -(gross + fee) : gross - fee;
      return {
        orderId: order.orderId,
        submittedAt: toDate(order.submittedAt),
        symbol: this.instruments.get(order.instrumentId)?.symbol ?? order.instrumentId.slice(0, 8),
        side: order.side,
        quantity,
        price,
        gross,
        fee,
        net,
        status: order.status || 'ACCEPTED'
      };
    }).sort((a, b) => (b.submittedAt?.getTime() ?? 0) - (a.submittedAt?.getTime() ?? 0));

    const filled = this.fills.filter((fill) => fill.status === 'Filled');
    this.filledTrades = filled.length;
    this.grossVolume = filled.reduce((sum, fill) => sum + Number(fill.price) * Number(fill.quantity), 0);
    this.feesPaid = filled.reduce((sum, fill) => sum + Number(fill.fee ?? 0), 0);
    this.openOrders = this.journal.filter((row) => row.status === 'ACCEPTED' || row.status === 'PENDING').length;
  }

  private buildActivities(): void {
    const rows: ActivityRow[] = [];
    this.loginEvents.forEach((event) => rows.push({
      at: toDate(event.occurredAt),
      activity: event.outcome === 'SUCCESS' ? 'Successful sign-in' : 'Failed sign-in',
      details: `${event.emailAttempted}${this.reason(event.details)}`,
      status: event.outcome,
      tone: event.outcome === 'SUCCESS' ? 'ok' : 'warn'
    }));
    this.cashEvents.forEach((event) => rows.push({
      at: toDate(event.occurredAt),
      activity: this.cashActivity(event.eventType),
      details: `${formatMoney(event.amount, true)} USD · ${this.selectedAccount?.accountName || 'Account'}`,
      status: 'SETTLED',
      tone: 'ok'
    }));
    this.tradeEvents.forEach((event) => rows.push({
      at: toDate(event.occurredAt),
      activity: humanize(event.action),
      details: `${humanize(event.entityType)} ${event.entityId.slice(0, 8)}`,
      status: 'RECORDED',
      tone: 'neutral'
    }));
    this.activities = rows
      .sort((a, b) => (b.at?.getTime() ?? 0) - (a.at?.getTime() ?? 0))
      .slice(0, 12);
  }

  private cashActivity(eventType: string): string {
    switch (eventType) {
      case 'DEPOSIT': return 'Deposit completed';
      case 'WITHDRAWAL': return 'Withdrawal completed';
      case 'BUY': return 'Buy settled';
      case 'SELL': return 'Sell proceeds settled';
      default: return humanize(eventType);
    }
  }

  private reason(details: string): string {
    try {
      const parsed = JSON.parse(details || '{}');
      return parsed?.reason ? ` · ${parsed.reason}` : '';
    } catch {
      return '';
    }
  }

  private fail(message: string): void {
    this.error = message;
    this.loading = false;
    this.cdr.detectChanges();
  }
}
