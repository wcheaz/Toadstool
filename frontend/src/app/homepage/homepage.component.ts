import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, Instrument, QuoteResponse, CandleResponse } from '../api.service';
import { TradingModalComponent } from '../trading-modal/trading-modal.component';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { createChart, ColorType, CandlestickSeries, HistogramSeries, UTCTimestamp } from 'lightweight-charts';

interface Asset {
  instrumentId: string;
  symbol: string;
  name: string;
  price: number;
  change: number;
  holdings: number;
}

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [CommonModule, TradingModalComponent],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.css'
})
export class HomepageComponent implements OnInit, OnDestroy, AfterViewInit {
  clientName: string = '';
  accountId: string = '';
  currentBalance: number = 142384.50;
  availableForTrading: number = 12450.00;
  isBalanceVisible: boolean = true;
  activeTab: string = 'news';
  platformStatus: string = 'Platform live status: Standard Trading Hours';
  
  // Trading modal state
  isTradeModalOpen: boolean = false;
  selectedAsset: Asset | null = null;

  // Available assets
  assets: Asset[] = [
    { instrumentId: '11111111-1111-1111-1111-111111111111', symbol: 'NEXS', name: 'Nexus Equity Fund', price: 142.10, change: 1.76, holdings: 40.0 },
    { instrumentId: '22222222-2222-2222-2222-222222222222', symbol: 'USDT', name: 'US Digital Dollar', price: 1.00, change: 0.00, holdings: 12450.0 },
    { instrumentId: '33333333-3333-3333-3333-333333333333', symbol: 'BTC', name: 'Bitcoin Vault Share', price: 67230.00, change: -0.45, holdings: 0.15 }
  ];

  instruments: Instrument[] = [];
  instrumentQuotes: Map<string, QuoteResponse> = new Map();
  loadingInstruments: boolean = false;

  // Test: display first instrument price
  testInstrument: Instrument | null = null;
  testPrice: number | null = null;

  // Charting
  @ViewChild('chartContainer') chartContainer: ElementRef | null = null;
  selectedTimeframe: string = '1day';
  timeframes = [
    { label: '1m', days: 0.0007 },
    { label: '15m', days: 0.01 },
    { label: '30m', days: 0.02 },
    { label: '1h', days: 0.042 },
    { label: '1d', days: 1 },
    { label: '7d', days: 7 },
    { label: '1mo', days: 30 },
    { label: '6mo', days: 180 },
    { label: '1y', days: 365 },
    { label: 'all', days: 1825 }
  ];
  candles: CandleResponse[] = [];
  loadingCandles: boolean = false;

  private destroy$ = new Subject<void>();

  constructor(private apiService: ApiService, private cdr: ChangeDetectorRef) {
    // Get client info from localStorage
    this.clientName = localStorage.getItem('clientName') || 'User';
    this.accountId = localStorage.getItem('accountId') || '';
  }

  ngOnInit() {
    this.loadInstruments();
  }

  ngAfterViewInit() {
    if (this.testInstrument) {
      this.loadCandles(this.selectedTimeframe);
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadInstruments() {
    this.loadingInstruments = true;
    this.apiService.getInstruments(1, 0).subscribe({
      next: (response) => {
        console.log('Instruments loaded:', response.items);
        this.instruments = response.items;
        this.cdr.detectChanges();

        // TEST: Load first instrument
        if (response.items.length > 0) {
          this.testInstrument = response.items[0];
          this.cdr.detectChanges();
          this.loadCandles(this.selectedTimeframe);
          this.apiService.getInstrumentQuote(this.testInstrument.instrumentId).subscribe({
            next: (quote) => {
              this.testPrice = quote.price;
              console.log('Test price loaded:', this.testPrice);
              this.cdr.detectChanges();
            },
            error: (e) => console.error('Test price error:', e)
          });
        }

        this.refreshQuotes();
      },
      error: (error) => {
        console.error('Failed to load instruments:', error);
        this.loadingInstruments = false;
      }
    });
  }

  refreshQuotes() {
    this.instruments.forEach(instrument => {
      this.apiService.getInstrumentQuote(instrument.instrumentId).subscribe({
        next: (quote) => {
          this.instrumentQuotes.set(instrument.instrumentId, quote);
          this.loadingInstruments = false;
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error(`Failed to load quote for ${instrument.symbol}:`, error);
          this.loadingInstruments = false;
        }
      });
    });
  }

  selectTimeframe(timeframe: string) {
    this.selectedTimeframe = timeframe;
    if (this.testInstrument) {
      this.loadCandles(timeframe);
    }
  }

  loadCandles(timeframe: string) {
    if (!this.testInstrument) return;

    this.loadingCandles = true;
    const timeframeObj = this.timeframes.find(tf => tf.label === timeframe);
    if (!timeframeObj) {
      this.loadingCandles = false;
      return;
    }

    const days = Math.max(1, Math.ceil(timeframeObj.days));
    this.apiService.getCandles(this.testInstrument.instrumentId, days).subscribe({
      next: (candles) => {
        this.candles = candles;
        this.loadingCandles = false;
        this.cdr.detectChanges();
        setTimeout(() => this.renderChart(), 0);
      },
      error: (error) => {
        console.error('Failed to load candles:', error);
        this.loadingCandles = false;
        this.cdr.detectChanges();
      }
    });
  }

  renderChart() {
    if (!this.chartContainer || this.candles.length === 0) return;

    const container = this.chartContainer.nativeElement;
    container.innerHTML = '';

    const chart = createChart(container, {
      layout: {
        background: { type: ColorType.Solid, color: '#1e1e1e' },
        textColor: '#d1d5db'
      },
      width: container.clientWidth,
      height: 400,
      timeScale: { timeVisible: true, secondsVisible: true }
    });

    const candlestickSeries = chart.addSeries(CandlestickSeries, { upColor: '#26a69a', downColor: '#ef5350' });
    const volumeSeries = chart.addSeries(HistogramSeries, { color: '#1f77b4' });

    const candleData = this.candles.map(c => ({
      time: Math.floor(new Date(c.date).getTime() / 1000) as UTCTimestamp,
      open: c.open,
      high: c.high,
      low: c.low,
      close: c.close
    }));

    const volumeData = this.candles.map(c => ({
      time: Math.floor(new Date(c.date).getTime() / 1000) as UTCTimestamp,
      value: c.volume,
      color: c.close >= c.open ? '#26a69a' : '#ef5350'
    }));

    candlestickSeries.setData(candleData);
    volumeSeries.setData(volumeData);
    chart.timeScale().fitContent();
  }

  getQuote(instrumentId: string): QuoteResponse | undefined {
    return this.instrumentQuotes.get(instrumentId);
  }

  getPriceChangeClass(changePercent: number | undefined): string {
    if (!changePercent) return '';
    return changePercent >= 0 ? 'price-up' : 'price-down';
  }

  toggleBalanceVisibility() {
    this.isBalanceVisible = !this.isBalanceVisible;
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  deposit() {
    alert('Deposit functionality coming soon');
  }

  withdraw() {
    alert('Withdraw functionality coming soon');
  }

  transfer() {
    alert('Transfer functionality coming soon');
  }

  navigateTo(section: string) {
    console.log('Navigating to:', section);
  }

  openProfile() {
    alert('Profile settings coming soon');
  }

  openNotifications() {
    alert('Notifications settings coming soon');
  }

  openTradeModal(instrument: Instrument, quote: QuoteResponse | undefined) {
    if (!quote) return;
    this.selectedAsset = {
      instrumentId: instrument.instrumentId,
      symbol: instrument.symbol,
      name: instrument.name,
      price: quote.price,
      change: quote.changePercent,
      holdings: 0
    };
    this.isTradeModalOpen = true;
  }

  closeTradeModal() {
    this.isTradeModalOpen = false;
    this.selectedAsset = null;
  }

  logout() {
    localStorage.removeItem('clientName');
    window.location.href = '/login';
  }
}
