import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';
import { CandleResponse, Instrument, QuoteResponse } from '../core/api/api.models';
import { NewsService, NewsResponse, EarningsEvent } from '../services/news.service';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';
import { NewsCardComponent } from '../shared/components/news-card/news-card.component';
import { NewsDetailModalComponent } from '../shared/components/news-detail-modal/news-detail-modal.component';
import { NewsNoticeComponent } from '../shared/components/news-notice/news-notice.component';
import { NewsArticle, toNewsArticle } from '../shared/models/news-article.model';
import { Subscription, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';

interface Asset {
  instrumentId: string;
  symbol: string;
  name: string;
  price: number;
  change: number;
  holdings: number;
}

interface TradingAssetRow {
  instrument: Instrument;
  articleCount: number;
}

type CalendarImpact = 'HIGH' | 'MEDIUM' | 'LOW';

interface CalendarRow {
  dateLabel: string;
  title: string;
  detail: string;
  impact: CalendarImpact;
}

const TRADING_ASSET_COUNT = 4;
const CALENDAR_EVENT_COUNT = 6;
const EARNINGS_HOUR_LABELS: { [hour: string]: string } = {
  bmo: 'Before open',
  amc: 'After close',
  dmh: 'During market'
};

@Component({
  selector: 'app-news',
  standalone: true,
  imports: [CommonModule, TradingModalComponent, NewsCardComponent, NewsDetailModalComponent, NewsNoticeComponent],
  templateUrl: './news.component.html',
  styleUrl: './news.component.css'
})
export class NewsComponent implements OnInit, OnDestroy {
  clientName: string = '';
  accountId: string = '';
  accountBalance: number | null = null;
  /** Quantity held per instrumentId for the active account. */
  private holdings: Map<string, number> = new Map();

  // Trading modal state
  isTradeModalOpen: boolean = false;
  selectedAsset: Asset | null = null;
  tradingChartData: CandleResponse[] = [];
  selectedInstrumentId: string = '';
  selectedTimeframeForChart: string = '90';
  apiError: string = '';
  isLoadingNews: boolean = false;
  newsLoadError: string = '';

  // News detail modal state
  selectedNewsArticle: NewsArticle | null = null;

  // Live news articles
  newsArticles: NewsArticle[] = [];
  rawNewsData: NewsResponse[] = [];

  private newsRefreshSubscription: Subscription | null = null;

  // Trading Assets: the instruments mentioned most in the current news feed
  private instruments: Instrument[] = [];
  tradingAssets: TradingAssetRow[] = [];
  assetQuotes: Map<string, QuoteResponse> = new Map();
  isLoadingAssets: boolean = true;

  // Impact Calendar: upcoming earnings for tradable equities
  calendarEvents: CalendarRow[] = [];
  isLoadingCalendar: boolean = true;

  constructor(private router: Router, private apiService: ApiService, private authService: AuthService, private newsService: NewsService, private cdr: ChangeDetectorRef) {
    this.clientName = this.authService.getClientName();
    this.accountId = this.authService.getAccountId();
    this.refreshAccount();
  }

  /** Load cash and holdings for the active account; also called after each trade. */
  refreshAccount() {
    if (!this.accountId) return;
    this.apiService.getAccountById(this.accountId).subscribe({
      next: (account) => this.accountBalance = account.balance,
      error: (error) => console.error('Failed to load account balance:', error)
    });
    this.apiService.getAccountHoldings(this.accountId).subscribe({
      next: (response) => {
        this.holdings = new Map(response.holdings.map(item => [item.instrumentId, Number(item.quantity)]));
        if (this.selectedAsset) {
          this.selectedAsset = { ...this.selectedAsset, holdings: this.holdings.get(this.selectedAsset.instrumentId) ?? 0 };
        }
        this.cdr.detectChanges();
      },
      error: (error) => console.error('Failed to load holdings:', error)
    });
  }

  ngOnInit() {
    this.loadInstruments();
    this.loadNews();
    this.loadCalendar();

    // Auto-refresh news every 5 minutes
    this.newsRefreshSubscription = interval(300000)
      .pipe(switchMap(() => this.newsService.getLatestNews()))
      .subscribe({
        next: (news) => {
          this.rawNewsData = news;
          this.convertNewsToArticles(news);
          this.newsLoadError = '';
          this.updateTradingAssets();
          this.loadCalendar();
          this.cdr.detectChanges();
        },
        error: (e) => {
          console.error('Failed to auto-refresh news:', e);
        }
      });
  }

  ngOnDestroy() {
    if (this.newsRefreshSubscription) {
      this.newsRefreshSubscription.unsubscribe();
    }
  }

  private loadNews() {
    this.isLoadingNews = true;
    this.newsLoadError = '';

    this.newsService.getLatestNews().subscribe({
      next: (news) => {
        this.rawNewsData = news;
        this.convertNewsToArticles(news);
        this.isLoadingNews = false;
        this.updateTradingAssets();
        this.cdr.detectChanges();
      },
      error: (e) => {
        console.error('Failed to load news:', e);
        this.newsLoadError = 'Failed to load news. Please try again later.';
        this.isLoadingNews = false;
        this.newsArticles = [];
        this.cdr.detectChanges();
      }
    });
  }

  private loadInstruments() {
    this.apiService.getInstruments(50, 0).subscribe({
      next: (response) => {
        this.instruments = response.items;
        this.updateTradingAssets();
      },
      error: (e) => {
        console.error('Failed to load instruments:', e);
        this.isLoadingAssets = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Rank instruments by how many current articles mention them, then refresh quotes for the top few. */
  private updateTradingAssets() {
    if (this.instruments.length === 0) return;

    const articleCounts = new Map<string, number>();
    this.rawNewsData.forEach(news => articleCounts.set(news.symbol, (articleCounts.get(news.symbol) ?? 0) + 1));

    // Stable sort keeps the instrument list order for symbols with equal counts
    this.tradingAssets = this.instruments
      .map(instrument => ({ instrument, articleCount: articleCounts.get(instrument.symbol) ?? 0 }))
      .sort((a, b) => b.articleCount - a.articleCount)
      .slice(0, TRADING_ASSET_COUNT);

    this.apiService.getBatchQuotes(this.tradingAssets.map(row => row.instrument.instrumentId)).subscribe({
      next: (quotes) => {
        this.assetQuotes = new Map(Object.entries(quotes));
        this.isLoadingAssets = false;
        this.cdr.detectChanges();
      },
      error: (e) => {
        console.error('Failed to load trading asset quotes:', e);
        this.isLoadingAssets = false;
        this.cdr.detectChanges();
      }
    });
  }

  private loadCalendar() {
    this.newsService.getEarningsCalendar().subscribe({
      next: (events) => {
        this.calendarEvents = events.slice(0, CALENDAR_EVENT_COUNT).map(event => this.toCalendarRow(event));
        this.isLoadingCalendar = false;
        this.cdr.detectChanges();
      },
      error: (e) => {
        console.error('Failed to load earnings calendar:', e);
        this.isLoadingCalendar = false;
        this.cdr.detectChanges();
      }
    });
  }

  private toCalendarRow(event: EarningsEvent): CalendarRow {
    const [year, month, day] = event.date.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    const dateLabel = [
      date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
      EARNINGS_HOUR_LABELS[event.hour],
      event.symbol
    ].filter(Boolean).join(' · ');
    const period = event.quarter && event.year ? ` Q${event.quarter} ${event.year}` : '';

    return {
      dateLabel,
      title: `${event.symbol}${period} Earnings`,
      detail: event.epsEstimate != null ? `EPS est. $${event.epsEstimate.toFixed(2)}` : '',
      impact: this.getEarningsImpact(date)
    };
  }

  /** Sooner reports matter more: within 3 days is HIGH, within a week MEDIUM. */
  private getEarningsImpact(date: Date): CalendarImpact {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const daysUntil = Math.round((date.getTime() - today.getTime()) / 86400000);
    if (daysUntil <= 3) return 'HIGH';
    if (daysUntil <= 7) return 'MEDIUM';
    return 'LOW';
  }

  getAssetQuote(row: TradingAssetRow): QuoteResponse | undefined {
    return this.assetQuotes.get(row.instrument.instrumentId);
  }

  private convertNewsToArticles(newsResponses: NewsResponse[]): void {
    this.newsArticles = newsResponses.map(toNewsArticle);
  }

  get filteredNewsArticles(): NewsArticle[] {
    return this.newsArticles;
  }

  navigateTo(route: string) {
    if (route === 'dashboard') {
      this.router.navigate(['/homepage']);
    } else if (route === 'news') {
      this.router.navigate(['/news']);
    } else {
      console.log('Navigate to:', route);
    }
  }

  openProfile() {
    console.log('Open profile');
  }

  openNotifications() {
    console.log('Open notifications');
  }

  logout() {
    this.authService.logout();
  }

  openTradeModal(row: TradingAssetRow) {
    const quote = this.getAssetQuote(row);
    if (!quote) return;
    const instrument = row.instrument;

    this.apiError = '';
    this.selectedAsset = {
      instrumentId: instrument.instrumentId,
      symbol: instrument.symbol,
      name: instrument.name,
      price: quote.price,
      change: quote.changePercent,
      holdings: this.holdings.get(instrument.instrumentId) ?? 0
    };
    this.selectedInstrumentId = instrument.instrumentId;
    this.selectedTimeframeForChart = '90';
    this.tradingChartData = [];
    this.isTradeModalOpen = true;

    this.apiService.getCandles(instrument.instrumentId, 90).subscribe({
      next: (candles: CandleResponse[]) => {
        this.tradingChartData = candles;
        this.apiError = '';
        this.cdr.detectChanges();
      },
      error: (e: any) => {
        console.error('Failed to load chart data:', e);
        this.apiError = 'API is down';
        this.cdr.detectChanges();
      }
    });
  }

  closeTradeModal() {
    this.isTradeModalOpen = false;
    this.selectedAsset = null;
  }

  openNewsDetail(article: NewsArticle) {
    this.selectedNewsArticle = article;
  }

  closeNewsDetail() {
    this.selectedNewsArticle = null;
  }
}
