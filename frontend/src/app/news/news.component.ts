import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';
import { CandleResponse } from '../core/api/api.models';
import { NewsService, NewsResponse } from '../services/news.service';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';
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

interface NewsArticle {
  id: string;
  category: string;
  title: string;
  description: string;
  timestamp: string;
  impact: 'HIGH' | 'MEDIUM' | 'LOW' | 'NEUTRAL';
  assets: string[];
  market?: string;
  sourceUrl?: string;
  imageUrl?: string;
}

interface MarketData {
  symbol: string;
  members: string;
  price: string;
  change: string;
}

interface CalendarEvent {
  date: string;
  event: string;
  impact: 'HIGH' | 'MEDIUM' | 'LOW' | 'NEUTRAL' | 'BEARING';
  forecast: string;
}

@Component({
  selector: 'app-news',
  standalone: true,
  imports: [CommonModule, TradingModalComponent],
  templateUrl: './news.component.html',
  styleUrl: './news.component.css'
})
export class NewsComponent implements OnInit, OnDestroy {
  clientName: string = '';
  accountId: string = '';
  activeTab: string = 'all-feed';

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
  selectedStockPrice: number | null = null;

  // News categories
  categories = ['News'];

  // Live news articles
  newsArticles: NewsArticle[] = [];
  rawNewsData: NewsResponse[] = [];

  private newsRefreshSubscription: Subscription | null = null;

  // Live Market Sentiment
  marketSentiment = {
    bullish: 64,
    bearish: 36
  };

  // Trading Assets
  tradingAssets: MarketData[] = [
    {
      symbol: 'BTC',
      members: '3,024 members',
      price: '$67,230.00',
      change: '+0.45%'
    },
    {
      symbol: 'NEMS',
      members: '4,371 members',
      price: '$142.10',
      change: '+1.76%'
    },
    {
      symbol: 'USDT',
      members: '5,181 members',
      price: '$1.00',
      change: '+0.02%'
    },
    {
      symbol: 'MUSD',
      members: '2,844 members',
      price: '$84.95',
      change: '+0.657%'
    }
  ];

  // Impact Calendar
  calendarEvents: CalendarEvent[] = [
    {
      date: '08:30 - BTC',
      event: 'BTC Impact',
      impact: 'NEUTRAL',
      forecast: '1.5'
    },
    {
      date: '13:00 - EUR',
      event: 'EURO IMPACT - NEUTRAL',
      impact: 'NEUTRAL',
      forecast: '10.0'
    },
    {
      date: '13:00 - EUR',
      event: 'EU Consumer Confidence Draft',
      impact: 'BEARING',
      forecast: '10.0'
    },
    {
      date: '20:30 - USD',
      event: 'Bank of England Rate Decision',
      impact: 'BEARING',
      forecast: '5.0'
    }
  ];

  constructor(private router: Router, private apiService: ApiService, private authService: AuthService, private newsService: NewsService, private cdr: ChangeDetectorRef) {
    this.clientName = this.authService.getClientName();
    this.accountId = this.authService.getAccountId();
  }

  ngOnInit() {
    this.loadNews();

    // Auto-refresh news every 5 minutes
    this.newsRefreshSubscription = interval(300000)
      .pipe(switchMap(() => this.newsService.getLatestNews()))
      .subscribe({
        next: (news) => {
          this.rawNewsData = news;
          this.convertNewsToArticles(news);
          this.newsLoadError = '';
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

  private convertNewsToArticles(newsResponses: NewsResponse[]): void {
    this.newsArticles = newsResponses.map((news, index) => ({
      id: news.newsId,
      category: news.category === 'company' ? 'Company News' : 'Market News',
      title: news.headline,
      description: news.summary || '',
      timestamp: this.formatTimestamp(news.publishedTimestamp),
      impact: this.getImpactLevel(news.source),
      assets: [news.symbol],
      market: `${news.source} • ${this.formatTimestamp(news.publishedTimestamp)}`,
      sourceUrl: news.sourceUrl,
      imageUrl: news.imageUrl
    }));
  }

  private formatTimestamp(timestamp: number): string {
    if (!timestamp) return 'Recently';
    const date = new Date(timestamp * 1000);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    return date.toLocaleDateString();
  }

  private getImpactLevel(source: string): 'HIGH' | 'MEDIUM' | 'LOW' | 'NEUTRAL' {
    const sourceUpper = (source || '').toUpperCase();
    if (sourceUpper.includes('REUTERS') || sourceUpper.includes('BLOOMBERG') || sourceUpper.includes('CNBC')) {
      return 'HIGH';
    }
    if (sourceUpper.includes('SEEKING ALPHA') || sourceUpper.includes('INVESTOR')) {
      return 'MEDIUM';
    }
    return 'LOW';
  }

  selectTab(category: string) {
    this.activeTab = category.toLowerCase().replace(/\s+/g, '-');
    console.log('Selected tab:', this.activeTab);
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
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  openTradeModal(symbol: string, price: string) {
    this.apiError = '';
    const numPrice = parseFloat(price.replace('$', '').replace(',', ''));
    this.selectedAsset = {
      instrumentId: this.getInstrumentIdBySymbol(symbol),
      symbol: symbol,
      name: symbol,
      price: numPrice,
      change: 0,
      holdings: 0
    };
    this.selectedInstrumentId = this.selectedAsset.instrumentId;
    this.selectedTimeframeForChart = '90';
    this.isTradeModalOpen = true;

    this.apiService.getCandles(this.selectedAsset.instrumentId, 90).subscribe({
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

  getInstrumentIdBySymbol(symbol: string): string {
    const symbolMap: { [key: string]: string } = {
      'BTC': '33333333-3333-3333-3333-333333333333',
      'NEMS': '11111111-1111-1111-1111-111111111111',
      'USDT': '22222222-2222-2222-2222-222222222222',
      'MUSD': '44444444-4444-4444-4444-444444444444'
    };
    return symbolMap[symbol] || '';
  }

  getImpactColor(impact: string): string {
    switch (impact) {
      case 'HIGH':
        return '#ef4444';
      case 'MEDIUM':
        return '#f59e0b';
      case 'LOW':
        return '#10b981';
      case 'NEUTRAL':
        return '#6b7280';
      default:
        return '#6b7280';
    }
  }

  openNewsDetail(article: NewsArticle) {
    this.selectedNewsArticle = article;
    this.selectedStockPrice = null;
    this.fetchStockPrice(article.assets[0]);
  }

  closeNewsDetail() {
    this.selectedNewsArticle = null;
    this.selectedStockPrice = null;
  }

  private fetchStockPrice(symbol: string) {
    this.apiService.getCandles(this.getInstrumentIdBySymbol(symbol), 1).subscribe({
      next: (candles) => {
        if (candles && candles.length > 0) {
          this.selectedStockPrice = candles[candles.length - 1].close;
        }
      },
      error: (e) => {
        console.error('Failed to fetch stock price:', e);
        this.selectedStockPrice = null;
      }
    });
  }

  tradeSelectedStock(symbol: string) {
    // Close news modal and open trading modal for the selected stock
    this.closeNewsDetail();
    this.openTradeModal(symbol, this.selectedStockPrice?.toString() || '0');
  }

  getImpactBgColor(impact: string): string {
    switch (impact) {
      case 'HIGH':
        return 'rgba(239, 68, 68, 0.1)';
      case 'MEDIUM':
        return 'rgba(245, 158, 11, 0.1)';
      case 'LOW':
        return 'rgba(16, 185, 129, 0.1)';
      case 'NEUTRAL':
        return 'rgba(107, 114, 128, 0.1)';
      default:
        return 'rgba(107, 114, 128, 0.1)';
    }
  }
}
