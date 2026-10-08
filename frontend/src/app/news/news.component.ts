import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../core/api/api.service';
import { CandleResponse } from '../core/api/api.models';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';

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
export class NewsComponent implements OnInit {
  clientName: string = '';
  activeTab: string = 'all-feed';

  // Trading modal state
  isTradeModalOpen: boolean = false;
  selectedAsset: Asset | null = null;
  tradingChartData: CandleResponse[] = [];
  selectedInstrumentId: string = '';
  selectedTimeframeForChart: string = '90';
  apiError: string = '';

  // News categories
  categories = ['All Feed', 'Macro Policy', 'Earnings', 'Commodities', 'Crypto', 'Forex'];

  // Sample news articles
  newsArticles: NewsArticle[] = [
    {
      id: '1',
      category: 'MACRO POLICY',
      title: 'Federal Reserve Holds Benchmark Rates Steady, Pivot Signals Calibre to Q4 Output Strength',
      description: 'Chair Powell reaffirmed the committee\'s steady-state posture on the labor-session plot, suggesting that strong policy into the upcoming weeks and months could trigger fresh 25bp adjustment matrix as done December.',
      timestamp: '16 minutes ago',
      impact: 'HIGH',
      assets: ['NEMS', 'USDT', 'BTC'],
      market: 'FIXED INCOME • 16 MINUTES • Recent Treasuries Slides Neath'
    },
    {
      id: '2',
      category: 'MACRO POLICY',
      title: 'Bank of Tokyo Government Bond Purchase Volume Guidelines',
      description: 'In an unexpected policy shift, the Bank of Tokyo calibrated bond-buying bands to contain anomalous margin compression, dampening regional retail consumer policy.',
      timestamp: 'implied asset',
      impact: 'MEDIUM',
      assets: ['JPY', 'JGB'],
      market: 'FIXED INCOME'
    },
    {
      id: '3',
      category: 'EARNINGS',
      title: 'Silicon Core Yield Metrics Rise Beyond Forecasts in Q3',
      description: 'Industrial manufacturers register their blood-core curve, revolving outstanding supply bottlenecks across primary micro-scale suppliers.',
      timestamp: 'Estimated Asset',
      impact: 'MEDIUM',
      assets: ['NEMS'],
      market: 'EARNINGS'
    },
    {
      id: '4',
      category: 'COMMODITIES',
      title: 'Crude Supply Chains Normalise Following Maritime Accord Re-alignment',
      description: 'Strategic transit lines resume unified daily vessel allocations, prompting consolidation of flush-month energy commodity derivatives over recent support levels.',
      timestamp: 'Expected',
      impact: 'MEDIUM',
      assets: ['OIL', 'BRT'],
      market: 'COMMODITIES'
    },
    {
      id: '5',
      category: 'CRYPTO',
      title: 'Consolidated Vault Inflows Stabilise Amid Low OTC Volatility Periods',
      description: 'Network liquidity wholesale velocity desuture liquidation constraints impacting with fixed custody exits, reducing monetary liquidity flight parameters to recent lows.',
      timestamp: 'Network',
      impact: 'LOW',
      assets: ['BTC'],
      market: 'CRYPTO'
    }
  ];

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

  constructor(private router: Router, private apiService: ApiService, private cdr: ChangeDetectorRef) {
    this.clientName = localStorage.getItem('clientName') || 'User';
  }

  ngOnInit() {
    // Initialize component
  }

  selectTab(category: string) {
    this.activeTab = category.toLowerCase().replace(/\s+/g, '-');
    console.log('Selected tab:', this.activeTab);
  }

  get filteredNewsArticles(): NewsArticle[] {
    if (this.activeTab === 'all-feed') {
      return this.newsArticles;
    }

    const categoryMap: { [key: string]: string } = {
      'macro-policy': 'MACRO POLICY',
      'earnings': 'EARNINGS',
      'commodities': 'COMMODITIES',
      'crypto': 'CRYPTO',
      'forex': 'FOREX'
    };

    const targetCategory = categoryMap[this.activeTab];
    return this.newsArticles.filter(article => article.category === targetCategory);
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
