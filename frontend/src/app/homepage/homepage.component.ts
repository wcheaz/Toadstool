import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../core/api/api.service';
import { AccountResponse, Instrument, QuoteResponse, CandleResponse, CashEventResponse, OrderResponse } from '../core/api/api.models';
import { Asset } from '../shared/models/asset.model';
import { AuthService } from '../core/auth.service';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';
import { ErrorModalComponent } from '../shared/components/error-modal/error-modal.component';
import { PortfolioChartComponent } from './components/portfolio-chart/portfolio-chart.component';
import { DepositModalComponent } from '../deposit-modal/deposit-modal.component';
import { WithdrawalModalComponent, WithdrawalSummary } from '../withdrawal-modal/withdrawal-modal.component';
import { TransferModalComponent, TransferSummary } from '../transfer-modal/transfer-modal.component';
import { BalanceWidgetComponent } from './components/balance-widget/balance-widget.component';
import { NewsFeedComponent } from './components/news-feed/news-feed.component';
import { MarketAssetsTableComponent, PositionCost } from './components/market-assets-table/market-assets-table.component';
import { AccountStateService } from '../core/account-state.service';
import { CdkDrag, CdkDragDrop, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { CardControlsComponent } from './components/card-controls/card-controls.component';
import { CardSize, DashboardCard, DashboardCardId, DashboardLayoutService } from './dashboard-layout.service';
import { CardResizeHandleDirective } from './components/card-resize-handle/card-resize-handle.directive';
import { SymbolLinkService } from './symbol-link.service';
import { Subject, forkJoin, interval, of } from 'rxjs';
import { catchError, map, takeUntil } from 'rxjs/operators';

/** Enough history to cover every trade on a learner account. */
const COST_HISTORY_PAGE_SIZE = 1000;

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [
    CommonModule,
    TradingModalComponent,
    ErrorModalComponent,
    DepositModalComponent,
    WithdrawalModalComponent,
    TransferModalComponent,
    BalanceWidgetComponent,
    NewsFeedComponent,
    MarketAssetsTableComponent,
    PortfolioChartComponent,
    CardControlsComponent,
    CardResizeHandleDirective,
    CdkDropList,
    CdkDrag
  ],
  // One shared symbol selection per dashboard, read by the linked cards
  providers: [SymbolLinkService],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.css'
})
export class HomepageComponent implements OnInit, OnDestroy {
  clientName: string = '';
  accountId: string = '';
  accountName: string = '';
  currentBalance: number = 0;
  availableForTrading: number = 0;
  portfolioValue: number = 0;
  accounts: AccountResponse[] = [];
  activeTab: string = 'overview';
  platformStatus: string = 'Platform live status: Standard Trading Hours';

  // Trading modal state
  isTradeModalOpen: boolean = false;
  selectedAsset: Asset | null = null;

  // Error modal state
  isErrorModalOpen: boolean = false;
  errorMessage: string = '';
  errorDetails?: string;

  // Deposit modal state
  isDepositModalOpen: boolean = false;
  isWithdrawalModalOpen: boolean = false;
  isTransferModalOpen: boolean = false;

  // UI state
  isBalanceVisible: boolean = true;

  instruments: Instrument[] = [];
  instrumentQuotes: Map<string, QuoteResponse> = new Map();
  /** Quantity held per instrumentId for the active account. */
  holdings: Map<string, number> = new Map();
  /** Cash paid and received per instrumentId, from the account's trade cash events. */
  positionCosts: Map<string, PositionCost> = new Map();
  private heldInstruments: Instrument[] = [];
  private tradableInstruments: Instrument[] = [];
  loadingInstruments: boolean = false;

  // Trading modal state
  tradingChartData: CandleResponse[] = [];
  selectedInstrumentId: string = '';
  selectedTimeframeForChart: string = '90'; // default 3M

  // Dashboard card order and sizes, saved per user in this browser
  cards: DashboardCard[] = [];
  private layoutUserId = '';
  /** Tradable symbols that news ticker tags can link to. */
  selectableSymbols: string[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private accountState: AccountStateService,
    private layoutService: DashboardLayoutService,
    private symbolLink: SymbolLinkService,
    private cdr: ChangeDetectorRef
  ) {
    // Get client info from the current session
    this.clientName = this.authService.getClientName();
    this.accountId = this.authService.getAccountId();
    this.layoutUserId = this.authService.getClientId();
    this.cards = this.layoutService.load(this.layoutUserId);
  }

  get isDefaultLayout(): boolean {
    return this.layoutService.isDefault(this.cards);
  }

  onCardDropped(event: CdkDragDrop<DashboardCard[]>) {
    if (event.previousIndex === event.currentIndex) return;
    const cards = [...this.cards];
    moveItemInArray(cards, event.previousIndex, event.currentIndex);
    this.setLayout(cards);
  }

  /** Keyboard alternative to dragging: move the card one position earlier (-1) or later (+1). */
  moveCard(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= this.cards.length) return;
    const cards = [...this.cards];
    moveItemInArray(cards, index, target);
    this.setLayout(cards);
  }

  /** Live size updates while a card is being resized; saved once the resize ends. */
  resizeCard(id: DashboardCardId, size: CardSize) {
    this.cards = this.cards.map(card => card.id === id ? { ...card, ...size } : card);
  }

  saveLayout() {
    this.layoutService.save(this.layoutUserId, this.cards);
  }

  resetLayout() {
    this.cards = this.layoutService.reset(this.layoutUserId);
  }

  private setLayout(cards: DashboardCard[]) {
    this.cards = cards;
    this.saveLayout();
  }

  ngOnInit() {
    this.accountState.accounts$.subscribe(accounts => this.accounts = accounts);
    this.accountState.selectedAccount$.subscribe(account => {
      if (account) this.selectAccount(account);
    });
    this.accountState.loadAccounts();
    this.loadInstruments();
    interval(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.refreshQuotes());
  }

  selectAccount(account: AccountResponse) {
    const switched = account.accountId !== this.accountId || this.holdings.size === 0;
    this.accountId = account.accountId;
    this.accountName = account.accountName;
    this.currentBalance = account.balance;
    this.availableForTrading = account.balance;
    this.updatePortfolioValue();
    this.accounts = this.accounts.map(existing => existing.accountId === account.accountId ? account : existing);
    if (switched) {
      // Don't show the previous account's positions while the new ones load.
      this.holdings = new Map();
      this.positionCosts = new Map();
      this.heldInstruments = [];
      this.mergeInstruments();
      this.loadHoldings();
    }
  }

  /** Load the active account's positions and surface them at the top of the market assets table. */
  loadHoldings() {
    const accountId = this.accountId;
    if (!accountId) return;

    this.apiService.getAccountHoldings(accountId).subscribe({
      next: (response) => {
        if (accountId !== this.accountId) return;
        this.holdings = new Map(response.holdings.map(item => [item.instrumentId, Number(item.quantity)]));
        this.heldInstruments = response.holdings.map(item => ({
          instrumentId: item.instrumentId,
          symbol: item.symbol ?? '',
          name: item.name ?? '',
          assetClass: item.assetClass ?? '',
          status: 'TRADABLE'
        }));
        this.mergeInstruments();
        this.cdr.detectChanges();
        this.refreshQuotes();
      },
      error: (error) => console.error('Failed to load holdings:', error)
    });
    this.loadPositionCosts(accountId);
  }

  /** Join BUY/SELL cash events to their orders to get what was paid and received per instrument. */
  private loadPositionCosts(accountId: string) {
    forkJoin({
      orders: this.apiService.getOrders(accountId, COST_HISTORY_PAGE_SIZE, 0).pipe(
        map(page => page.items),
        catchError(() => of([] as OrderResponse[]))
      ),
      cashEvents: this.apiService.getCashEvents(accountId, COST_HISTORY_PAGE_SIZE, 0).pipe(
        map(page => page.items),
        catchError(() => of([] as CashEventResponse[]))
      )
    }).subscribe(({ orders, cashEvents }) => {
      if (accountId !== this.accountId) return;
      const orderById = new Map(orders.map(order => [order.orderId, order]));
      const costs = new Map<string, PositionCost>();
      cashEvents
        .filter(event => (event.eventType === 'BUY' || event.eventType === 'SELL') && event.orderId)
        .forEach(event => {
          const order = orderById.get(event.orderId as string);
          const amount = Number(event.amount);
          const quantity = Number(order?.quantity);
          if (!order || !Number.isFinite(amount) || !Number.isFinite(quantity)) return;
          const cost = costs.get(order.instrumentId) ?? { boughtCost: 0, netCash: 0, netQuantity: 0 };
          cost.netCash += amount; // BUY is negative, SELL positive
          if (event.eventType === 'BUY') {
            cost.boughtCost += -amount;
            cost.netQuantity += quantity;
          } else {
            cost.netQuantity -= quantity;
          }
          costs.set(order.instrumentId, cost);
        });
      this.positionCosts = costs;
      this.cdr.detectChanges();
    });
  }

  /** Re-read cash and holdings after a trade so the dashboard reflects it immediately. */
  onOrderCompleted() {
    if (!this.accountId) return;
    this.apiService.getAccountById(this.accountId).subscribe({
      next: (account) => {
        this.accountState.updateAccount(account);
        this.cdr.detectChanges();
      },
      error: (error) => console.error('Failed to refresh account after order:', error)
    });
    this.loadHoldings();
  }

  /** Held instruments first (largest position first), then the rest of the tradable list. */
  private mergeInstruments() {
    const byId = new Map<string, Instrument>();
    this.tradableInstruments.forEach(instrument => byId.set(instrument.instrumentId, instrument));
    this.heldInstruments.forEach(instrument => {
      if (!byId.has(instrument.instrumentId)) byId.set(instrument.instrumentId, instrument);
    });
    const all = Array.from(byId.values());
    const held = all.filter(instrument => (this.holdings.get(instrument.instrumentId) ?? 0) > 0);
    const rest = all.filter(instrument => (this.holdings.get(instrument.instrumentId) ?? 0) <= 0);
    this.instruments = [...held, ...rest];
    this.selectableSymbols = this.instruments.map(instrument => instrument.symbol);
  }

  /** A ticker tag in the news card was clicked: link every card to that instrument. */
  selectSymbol(symbol: string) {
    const instrument = this.instruments.find(item => item.symbol === symbol);
    if (instrument) {
      this.symbolLink.select(instrument);
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadInstruments() {
    this.loadingInstruments = true;
    this.apiService.getInstruments(25, 0).subscribe({
      next: (response) => {
        this.tradableInstruments = response.items;
        this.mergeInstruments();
        this.loadingInstruments = false;
        this.cdr.detectChanges();
        this.refreshQuotes();
      },
      error: (error: any) => {
        console.error('Failed to load instruments:', error);
        this.loadingInstruments = false;
      }
    });
  }

  refreshQuotes() {
    if (this.instruments.length === 0) return;

    // The batch endpoint accepts at most 25 ids; held positions are first in the list.
    const instrumentIds = this.instruments.map(i => i.instrumentId).slice(0, 25);
    this.apiService.getBatchQuotes(instrumentIds).subscribe({
      next: (quotes) => {
        this.instrumentQuotes = new Map(Object.entries(quotes));
        this.updatePortfolioValue();
        this.loadingInstruments = false;
        this.cdr.detectChanges();
        console.log('Batch quotes refreshed for', this.instruments.length, 'instruments');
      },
      error: (error) => {
        console.error('Failed to load batch quotes:', error);
        this.loadingInstruments = false;
      }
    });
  }

  private updatePortfolioValue() {
    const holdingsValue = Array.from(this.holdings.entries()).reduce((total, [instrumentId, quantity]) => {
      const price = this.instrumentQuotes.get(instrumentId)?.price ?? 0;
      return total + quantity * price;
    }, 0);
    this.portfolioValue = this.availableForTrading + holdingsValue;
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  toggleBalanceVisibility() {
    this.isBalanceVisible = !this.isBalanceVisible;
  }

  openTradeModal(instrument: Instrument, quote: QuoteResponse | undefined) {
    if (!quote) {
      this.errorMessage = 'API is down';
      this.errorDetails = 'Unable to fetch quote data for this asset. Please try again later.';
      this.isErrorModalOpen = true;
      return;
    }
    this.selectedAsset = {
      instrumentId: instrument.instrumentId,
      symbol: instrument.symbol,
      name: instrument.name,
      price: quote.price,
      change: quote.changePercent,
      holdings: this.holdings.get(instrument.instrumentId) ?? 0
    };
    this.selectedInstrumentId = instrument.instrumentId;
    this.selectedTimeframeForChart = '90'; // default to 3M
    this.tradingChartData = [];
    this.isTradeModalOpen = true;
    this.loadHoldingForSelectedAsset(instrument.instrumentId);

    this.apiService.getCandles(instrument.instrumentId, 90).subscribe({
      next: (candles: CandleResponse[]) => {
        console.log('Candles received:', candles.length, 'items');
        this.tradingChartData = candles;
        this.cdr.detectChanges();
      },
      error: (e: any) => {
        console.error('Failed to load chart data:', e);
        console.error('Error details:', e.status, e.statusText, e.message);
      }
    });
  }

  private loadHoldingForSelectedAsset(instrumentId: string): void {
    if (!this.accountId) return;
    this.apiService.getAccountHoldings(this.accountId).subscribe({
      next: (response) => {
        const holding = response.holdings.find(item => item.instrumentId === instrumentId);
        if (this.selectedAsset?.instrumentId === instrumentId) {
          const quantity = holding ? Number(holding.quantity) : 0;
          this.selectedAsset = { ...this.selectedAsset, holdings: Number.isFinite(quantity) ? Math.max(0, quantity) : 0 };
          this.cdr.detectChanges();
        }
      },
      error: (error) => console.error('Failed to load account holdings:', error)
    });
  }

  closeTradeModal() {
    this.isTradeModalOpen = false;
    this.selectedAsset = null;
  }

  closeErrorModal() {
    this.isErrorModalOpen = false;
    this.errorMessage = '';
    this.errorDetails = '';
  }

  closeDepositModal() {
    this.isDepositModalOpen = false;
  }

  openDepositModal() {
    this.isDepositModalOpen = true;
  }

  closeWithdrawalModal() {
    this.isWithdrawalModalOpen = false;
  }

  openWithdrawalModal() {
    this.isWithdrawalModalOpen = true;
  }

  onWithdrawClicked() {
    this.openWithdrawalModal();
  }

  onTransferClicked() {
    this.isTransferModalOpen = true;
  }

  closeTransferModal() {
    this.isTransferModalOpen = false;
  }

  onDepositSubmit(summary: any) {
    if (!this.accountId) return;
    this.apiService.postCashEvent(this.accountId, { eventType: 'DEPOSIT', amount: summary.amount }).subscribe({
      next: (account) => {
        this.accountState.updateAccount(account);
        alert(`Deposit of ${new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(summary.amount)} completed successfully!`);
      },
      error: () => alert('Deposit failed. Please try again.')
    });
  }

  onWithdrawalSubmit(summary: WithdrawalSummary) {
    if (!this.accountId) return;
    this.apiService.postCashEvent(this.accountId, { eventType: 'WITHDRAWAL', amount: summary.amount }).subscribe({
      next: (account) => {
        this.accountState.updateAccount(account);
        alert(`Withdrawal of ${new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(summary.amount)} submitted successfully.`);
      },
      error: () => alert('Withdrawal failed. Check the amount and available balance.')
    });
  }

  onTransferSubmit(summary: TransferSummary) {
    this.apiService.postCashEvent(summary.sourceAccount.accountId, { eventType: 'WITHDRAWAL', amount: summary.amount }).subscribe({
      next: source => this.apiService.postCashEvent(summary.destinationAccount.accountId, { eventType: 'DEPOSIT', amount: summary.amount }).subscribe({
        next: destination => {
          this.accountState.updateAccount(source);
          this.accountState.updateAccount(destination);
          this.accountState.selectAccount(source);
          alert(`Transfer of ${new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(summary.amount)} completed successfully.`);
        },
        error: () => alert('The destination account could not receive the funds.')
      }),
      error: () => alert('Transfer declined. Check the available balance.')
    });
  }

  logout() {
    this.authService.logout();
  }
}
