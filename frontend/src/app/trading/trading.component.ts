import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ApiService } from '../core/api/api.service';
import { CandleResponse, Instrument, QuoteResponse } from '../core/api/api.models';
import { AuthService } from '../core/auth.service';
import { Asset } from '../shared/models/asset.model';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';

interface RecommendedStock {
  symbol: string;
  name: string;
  assetClass: string;
  instrumentId: string;
  price: number;
  change: number;
  changePercent: number;
}

interface NewsItem {
  level: 'HIGH' | 'MEDIUM' | 'LOW';
  headline: string;
  summary: string;
  detail: string;
}

@Component({
  selector: 'app-trading',
  standalone: true,
  imports: [CommonModule, FormsModule, TradingModalComponent],
  templateUrl: './trading.component.html',
  styleUrl: './trading.component.css'
})
export class TradingComponent implements OnInit, OnDestroy {
  searchTerm = '';
  selectedStock: RecommendedStock | null = null;
  stocks: RecommendedStock[] = [];
  quotes = new Map<string, QuoteResponse>();
  accountId = '';
  isLoading = true;
  apiError = '';
  isTradeModalOpen = false;
  selectedAsset: Asset | null = null;
  selectedInstrumentId = '';
  tradingChartData: CandleResponse[] = [];

  private readonly destroy$ = new Subject<void>();

  private readonly curatedStocks: RecommendedStock[] = [
    this.curated('AAPL', 'Apple Inc.', 227.52, 2.84, 1.26),
    this.curated('MSFT', 'Microsoft Corp.', 416.32, 3.41, 0.83),
    this.curated('AMZN', 'Amazon.com Inc.', 187.54, 1.92, 1.03),
    this.curated('GOOGL', 'Alphabet Inc.', 162.14, -0.71, -0.44),
    this.curated('META', 'Meta Platforms', 567.78, 8.91, 1.59),
    this.curated('NFLX', 'Netflix Inc.', 721.55, 5.48, 0.77),
    this.curated('NVDA', 'NVIDIA Corp.', 138.20, 2.34, 1.72),
    this.curated('TSLA', 'Tesla Inc.', 248.98, -3.86, -1.55),
    this.curated('AVGO', 'Broadcom Inc.', 174.65, 1.44, 0.83),
    this.curated('ORCL', 'Oracle Corp.', 177.23, 2.61, 1.50),
    this.curated('AMD', 'Advanced Micro Devices', 164.12, -1.35, -0.82),
    this.curated('CRM', 'Salesforce Inc.', 292.44, 4.22, 1.47),
    this.curated('COST', 'Costco Wholesale', 886.11, 6.09, 0.69),
    this.curated('JPM', 'JPMorgan Chase', 224.08, 0.84, 0.38),
    this.curated('V', 'Visa Inc.', 289.44, 1.12, 0.39),
    this.curated('WMT', 'Walmart Inc.', 81.74, 0.53, 0.65),
    this.curated('DIS', 'Walt Disney Co.', 96.28, -0.92, -0.95),
    this.curated('NKE', 'Nike Inc.', 81.50, 1.34, 1.67),
    this.curated('KO', 'Coca-Cola Co.', 68.92, 0.28, 0.41),
    this.curated('PEP', 'PepsiCo Inc.', 172.61, -0.47, -0.27),
    this.curated('INTC', 'Intel Corp.', 23.71, 0.61, 2.64),
    this.curated('SPOT', 'Spotify Technology', 386.43, 4.73, 1.24),
    this.curated('UBER', 'Uber Technologies', 74.16, -0.36, -0.48),
    this.curated('ADBE', 'Adobe Inc.', 496.27, 3.19, 0.65),
    this.curated('PYPL', 'PayPal Holdings', 80.14, 1.08, 1.37)
  ];

  constructor(private apiService: ApiService, private authService: AuthService, private cdr: ChangeDetectorRef) {
    this.accountId = this.authService.getAccountId();
  }

  ngOnInit(): void {
    this.loadStocks();
    interval(5000).pipe(takeUntil(this.destroy$)).subscribe(() => this.refreshQuotes());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredStocks(): RecommendedStock[] {
    const query = this.searchTerm.trim().toLowerCase();
    if (!query) return this.stocks;
    return this.stocks.filter((stock) => `${stock.symbol} ${stock.name}`.toLowerCase().includes(query));
  }

  get selectedNews(): NewsItem {
    const stock = this.selectedStock ?? this.stocks[0];
    if (!stock) return { level: 'LOW', headline: 'Market updates are loading', summary: 'Live headlines will appear when market data is available.', detail: 'Fauxnance news feed' };
    const direction = stock.changePercent >= 0 ? 'gains' : 'pullback';
    return {
      level: Math.abs(stock.changePercent) > 1.5 ? 'HIGH' : 'MEDIUM',
      headline: `${stock.name} ${direction} draw attention across the market`,
      summary: `${stock.symbol} is moving ${stock.changePercent >= 0 ? 'higher' : 'lower'} as investors watch the latest market catalysts.`,
      detail: `Asset: ${stock.symbol} · Price: $${stock.price.toFixed(2)} · Daily change: ${stock.changePercent >= 0 ? '+' : ''}${stock.changePercent.toFixed(2)}%`
    };
  }

  selectStock(stock: RecommendedStock): void {
    this.selectedStock = stock;
  }

  openTrade(stock: RecommendedStock): void {
    this.selectStock(stock);
    const quote = this.quotes.get(stock.instrumentId) ?? this.toQuote(stock);
    this.selectedAsset = { instrumentId: stock.instrumentId, symbol: stock.symbol, name: stock.name, price: quote.price, change: quote.changePercent, holdings: 0 };
    this.selectedInstrumentId = stock.instrumentId;
    this.tradingChartData = [];
    this.isTradeModalOpen = true;
    this.apiService.getCandles(stock.instrumentId, 90).subscribe({
      next: (candles) => { this.tradingChartData = candles; this.cdr.detectChanges(); },
      error: () => this.cdr.detectChanges()
    });
  }

  closeTrade(): void {
    this.isTradeModalOpen = false;
    this.selectedAsset = null;
  }

  private loadStocks(): void {
    this.apiService.getInstruments(50, 0).subscribe({
      next: (response) => {
        const apiBySymbol = new Map(response.items.map((instrument) => [instrument.symbol.toUpperCase(), instrument]));
        this.stocks = this.curatedStocks.map((stock) => this.mergeApiInstrument(stock, apiBySymbol.get(stock.symbol))).slice(0, 25);
        this.isLoading = false;
        this.selectStock(this.stocks[0]);
        this.refreshQuotes();
        this.cdr.detectChanges();
      },
      error: () => {
        this.apiError = 'Live market data is unavailable. Showing the recommended watchlist.';
        this.stocks = this.curatedStocks;
        this.isLoading = false;
        this.selectStock(this.stocks[0]);
        this.seedFallbackQuotes();
        this.cdr.detectChanges();
      }
    });
  }

  private refreshQuotes(): void {
    if (!this.stocks.length) return;
    this.apiService.getBatchQuotes(this.stocks.map((stock) => stock.instrumentId)).subscribe({
      next: (quotes) => {
        Object.entries(quotes).forEach(([instrumentId, quote]) => this.quotes.set(instrumentId, quote));
        this.cdr.detectChanges();
      },
      error: () => this.seedFallbackQuotes()
    });
  }

  private seedFallbackQuotes(): void {
    this.stocks.forEach((stock) => this.quotes.set(stock.instrumentId, this.toQuote(stock)));
  }

  private mergeApiInstrument(stock: RecommendedStock, instrument?: Instrument): RecommendedStock {
    return instrument ? { ...stock, instrumentId: instrument.instrumentId, name: instrument.name } : stock;
  }

  private toQuote(stock: RecommendedStock): QuoteResponse {
    return { symbol: stock.symbol, price: stock.price, bid: stock.price - 0.02, ask: stock.price + 0.02, change: stock.change, changePercent: stock.changePercent, asOf: new Date().toISOString() };
  }

  private curated(symbol: string, name: string, price: number, change: number, changePercent: number): RecommendedStock {
    return { symbol, name, price, change, changePercent, assetClass: 'EQUITY', instrumentId: `faux-${symbol.toLowerCase()}` };
  }
}