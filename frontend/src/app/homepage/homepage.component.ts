import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../core/api/api.service';
import { AccountResponse, Instrument, QuoteResponse, CandleResponse } from '../core/api/api.models';
import { Asset } from '../shared/models/asset.model';
import { AuthService } from '../core/auth.service';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';
import { ErrorModalComponent } from '../shared/components/error-modal/error-modal.component';
import { DepositModalComponent } from '../deposit-modal/deposit-modal.component';
import { BalanceWidgetComponent } from './components/balance-widget/balance-widget.component';
import { NewsFeedComponent } from './components/news-feed/news-feed.component';
import { MarketAssetsTableComponent } from './components/market-assets-table/market-assets-table.component';
import { FeaturesGridComponent } from './components/features-grid/features-grid.component';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [
    CommonModule,
    TradingModalComponent,
    ErrorModalComponent,
    DepositModalComponent,
    BalanceWidgetComponent,
    NewsFeedComponent,
    MarketAssetsTableComponent,
    FeaturesGridComponent
  ],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.css'
})
export class HomepageComponent implements OnInit, OnDestroy {
  clientName: string = '';
  accountId: string = '';
  currentBalance: number = 0;
  availableForTrading: number = 0;
  accounts: AccountResponse[] = [];
  isCreatingAccount: boolean = false;
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

  // UI state
  isBalanceVisible: boolean = true;

  // Available assets
  assets: Asset[] = [
    { instrumentId: '11111111-1111-1111-1111-111111111111', symbol: 'NEXS', name: 'Nexus Equity Fund', price: 142.10, change: 1.76, holdings: 40.0 },
    { instrumentId: '22222222-2222-2222-2222-222222222222', symbol: 'USDT', name: 'US Digital Dollar', price: 1.00, change: 0.00, holdings: 12450.0 },
    { instrumentId: '33333333-3333-3333-3333-333333333333', symbol: 'BTC', name: 'Bitcoin Vault Share', price: 67230.00, change: -0.45, holdings: 0.15 }
  ];

  instruments: Instrument[] = [];
  instrumentQuotes: Map<string, QuoteResponse> = new Map();
  loadingInstruments: boolean = false;

  // Trading modal state
  tradingChartData: CandleResponse[] = [];
  selectedInstrumentId: string = '';
  selectedTimeframeForChart: string = '90'; // default 3M

  private destroy$ = new Subject<void>();

  constructor(private apiService: ApiService, private authService: AuthService, private cdr: ChangeDetectorRef) {
    // Get client info from localStorage
    this.clientName = this.authService.getClientName();
    this.accountId = this.authService.getAccountId();
  }

  ngOnInit() {
    this.loadAccounts();
    this.loadInstruments();
    interval(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.refreshQuotes());
  }

  loadAccounts() {
    const clientId = this.authService.getClientId();
    if (!clientId) return;

    this.apiService.getAccountsByClientId(clientId).subscribe({
      next: (response) => {
        this.accounts = response.items;
        const selectedAccount = this.accounts.find(account => account.accountId === this.authService.getAccountId())
          || this.accounts[0];
        if (selectedAccount) this.selectAccount(selectedAccount);
        this.cdr.detectChanges();
      },
      error: (error) => console.error('Failed to load accounts:', error)
    });
  }

  selectAccount(account: AccountResponse) {
    this.accountId = account.accountId;
    this.currentBalance = account.balance;
    this.availableForTrading = account.balance;
    this.authService.setAccountId(account.accountId);
  }

  createAccount() {
    const clientId = this.authService.getClientId();
    if (!clientId || this.isCreatingAccount) return;
    const accountName = window.prompt('Name this account', `Account ${this.accounts.length + 1}`)?.trim();
    if (!accountName) return;

    this.isCreatingAccount = true;
    this.apiService.createAccount(clientId, accountName).subscribe({
      next: (account) => {
        this.accounts = [...this.accounts, account];
        this.selectAccount(account);
        this.isCreatingAccount = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to create account:', error);
        this.isCreatingAccount = false;
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadInstruments() {
    this.loadingInstruments = true;
    this.apiService.getInstruments(25, 0).subscribe({
      next: (response) => {
        this.instruments = response.items;
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

    const instrumentIds = this.instruments.map(i => i.instrumentId);
    this.apiService.getBatchQuotes(instrumentIds).subscribe({
      next: (quotes) => {
        this.instrumentQuotes = new Map(Object.entries(quotes));
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
      holdings: 0
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

  onWithdrawClicked() {
    const amount = Number(window.prompt('Withdrawal amount') || 0);
    if (!amount || amount <= 0 || !this.accountId) return;
    this.apiService.postCashEvent(this.accountId, { eventType: 'WITHDRAWAL', amount }).subscribe({
      next: (account) => this.selectAccount(account),
      error: () => alert('Withdrawal failed. Check the amount and available balance.')
    });
  }

  onTransferClicked() {
    alert('Transfer functionality coming soon');
  }

  onDepositSubmit(summary: any) {
    if (!this.accountId) return;
    this.apiService.postCashEvent(this.accountId, { eventType: 'DEPOSIT', amount: summary.amount }).subscribe({
      next: (account) => {
        this.selectAccount(account);
        alert(`Deposit of ${new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(summary.amount)} completed successfully!`);
      },
      error: () => alert('Deposit failed. Please try again.')
    });
  }

  logout() {
    this.authService.logout();
    // Redirect to login
    window.location.href = '/login';
  }
}
