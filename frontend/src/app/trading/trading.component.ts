import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ApiService } from '../core/api/api.service';
import { CandleResponse, Instrument, QuoteResponse } from '../core/api/api.models';
import { AuthService } from '../core/auth.service';
import { NewsResponse, NewsService } from '../services/news.service';
import { Asset } from '../shared/models/asset.model';
import { NewsArticle, toNewsArticle } from '../shared/models/news-article.model';
import { NewsCardComponent } from '../shared/components/news-card/news-card.component';
import { NewsDetailModalComponent } from '../shared/components/news-detail-modal/news-detail-modal.component';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';

interface RecommendedAsset {
  instrument: Instrument;
  /** Articles mentioning this symbol in the current news batch; used for ranking. */
  articleCount: number;
}

/** The batch quote endpoint accepts at most 25 ids. */
const MAX_ASSETS = 25;
const MAX_SELECTED_ARTICLES = 3;
const QUOTE_REFRESH_MS = 5000;
const NEWS_REFRESH_MS = 300000;

@Component({
  selector: 'app-trading',
  standalone: true,
  imports: [CommonModule, FormsModule, TradingModalComponent, NewsCardComponent, NewsDetailModalComponent],
  templateUrl: './trading.component.html',
  styleUrl: './trading.component.css'
})
export class TradingComponent implements OnInit, OnDestroy {
  searchTerm = '';
  assets: RecommendedAsset[] = [];
  selectedAsset: RecommendedAsset | null = null;
  quotes = new Map<string, QuoteResponse>();
  accountId = '';
  accountBalance: number | null = null;
  isLoading = true;
  apiError = '';

  // News for the selected asset
  private news: NewsResponse[] = [];
  isLoadingNews = true;
  selectedNewsArticle: NewsArticle | null = null;

  // Trade modal state
  isTradeModalOpen = false;
  tradeAsset: Asset | null = null;
  selectedInstrumentId = '';
  tradingChartData: CandleResponse[] = [];

  private instruments: Instrument[] = [];
  private readonly destroy$ = new Subject<void>();

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private newsService: NewsService,
    private cdr: ChangeDetectorRef
  ) {
    this.accountId = this.authService.getAccountId();
  }

  ngOnInit(): void {
    this.loadInstruments();
    this.loadNews();
    this.refreshAccount();
    interval(QUOTE_REFRESH_MS).pipe(takeUntil(this.destroy$)).subscribe(() => this.refreshQuotes());
    interval(NEWS_REFRESH_MS).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadNews());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredAssets(): RecommendedAsset[] {
    const query = this.searchTerm.trim().toLowerCase();
    if (!query) return this.assets;
    return this.assets.filter(({ instrument }) => `${instrument.symbol} ${instrument.name}`.toLowerCase().includes(query));
  }

  /** Latest articles for the selected asset, or general market news when it has none. */
  get selectedArticles(): NewsArticle[] {
    const symbol = this.selectedAsset?.instrument.symbol;
    const forSymbol = this.news.filter(item => item.symbol === symbol);
    const source = forSymbol.length > 0 ? forSymbol : this.news.filter(item => item.category === 'market');
    return [...source]
      .sort((a, b) => (b.publishedTimestamp ?? 0) - (a.publishedTimestamp ?? 0))
      .slice(0, MAX_SELECTED_ARTICLES)
      .map(toNewsArticle);
  }

  get showingMarketNews(): boolean {
    const symbol = this.selectedAsset?.instrument.symbol;
    return !this.news.some(item => item.symbol === symbol);
  }

  getQuote(asset: RecommendedAsset | null): QuoteResponse | undefined {
    return asset ? this.quotes.get(asset.instrument.instrumentId) : undefined;
  }

  selectAsset(asset: RecommendedAsset): void {
    this.selectedAsset = asset;
  }

  openTrade(asset: RecommendedAsset): void {
    this.selectAsset(asset);
    const quote = this.getQuote(asset);
    if (!quote) return;

    const instrument = asset.instrument;
    this.tradeAsset = {
      instrumentId: instrument.instrumentId,
      symbol: instrument.symbol,
      name: instrument.name,
      price: quote.price,
      change: quote.changePercent,
      holdings: 0
    };
    this.selectedInstrumentId = instrument.instrumentId;
    this.tradingChartData = [];
    this.isTradeModalOpen = true;
    this.loadHoldingForTradeAsset(instrument.instrumentId);
    this.apiService.getCandles(instrument.instrumentId, 90).subscribe({
      next: (candles) => { this.tradingChartData = candles; this.cdr.detectChanges(); },
      error: () => this.cdr.detectChanges()
    });
  }

  closeTrade(): void {
    this.isTradeModalOpen = false;
    this.tradeAsset = null;
  }

  openNewsDetail(article: NewsArticle): void {
    this.selectedNewsArticle = article;
  }

  closeNewsDetail(): void {
    this.selectedNewsArticle = null;
  }

  /** Load cash for the active account; also called after each trade. */
  refreshAccount(): void {
    if (!this.accountId) return;
    this.apiService.getAccountById(this.accountId).subscribe({
      next: (account) => { this.accountBalance = account.balance; this.cdr.detectChanges(); },
      error: (error) => console.error('Failed to load account balance:', error)
    });
  }

  private loadHoldingForTradeAsset(instrumentId: string): void {
    if (!this.accountId) return;
    this.apiService.getAccountHoldings(this.accountId).subscribe({
      next: (response) => {
        const holding = response.holdings.find(item => item.instrumentId === instrumentId);
        if (this.tradeAsset?.instrumentId === instrumentId) {
          const quantity = holding ? Number(holding.quantity) : 0;
          this.tradeAsset = { ...this.tradeAsset, holdings: Number.isFinite(quantity) ? Math.max(0, quantity) : 0 };
          this.cdr.detectChanges();
        }
      },
      error: (error) => console.error('Failed to load account holdings:', error)
    });
  }

  private loadInstruments(): void {
    this.apiService.getInstruments(MAX_ASSETS, 0).subscribe({
      next: (response) => {
        this.instruments = response.items;
        this.apiError = '';
        this.isLoading = false;
        this.rankAssets();
        this.refreshQuotes();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load instruments:', error);
        this.apiError = 'Market data is unavailable right now. Please try again later.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private loadNews(): void {
    this.newsService.getLatestNews().subscribe({
      next: (news) => {
        this.news = news;
        this.isLoadingNews = false;
        this.rankAssets();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load news:', error);
        this.isLoadingNews = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Most-covered assets first; ties keep the instrument list order. */
  private rankAssets(): void {
    if (this.instruments.length === 0) return;

    const articleCounts = new Map<string, number>();
    this.news.forEach(item => articleCounts.set(item.symbol, (articleCounts.get(item.symbol) ?? 0) + 1));

    this.assets = this.instruments
      .map(instrument => ({ instrument, articleCount: articleCounts.get(instrument.symbol) ?? 0 }))
      .sort((a, b) => b.articleCount - a.articleCount)
      .slice(0, MAX_ASSETS);

    const selectedId = this.selectedAsset?.instrument.instrumentId;
    this.selectedAsset = this.assets.find(asset => asset.instrument.instrumentId === selectedId) ?? this.assets[0] ?? null;
  }

  private refreshQuotes(): void {
    if (this.assets.length === 0) return;
    this.apiService.getBatchQuotes(this.assets.map(asset => asset.instrument.instrumentId)).subscribe({
      next: (quotes) => {
        Object.entries(quotes).forEach(([instrumentId, quote]) => this.quotes.set(instrumentId, quote));
        this.cdr.detectChanges();
      },
      error: (error) => console.error('Failed to refresh quotes:', error)
    });
  }
}
