import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, CandleResponse, DepthLevel } from '../api.service';
import { createChart, ColorType, CandlestickSeries, UTCTimestamp } from 'lightweight-charts';
import { OrderConfirmationComponent } from '../order-confirmation/order-confirmation.component';
import { OrderSuccessComponent } from '../order-success/order-success.component';

interface Asset {
  symbol: string;
  name: string;
  price: number;
  change: number;
  holdings: number;
}

@Component({
  selector: 'app-trading-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, OrderConfirmationComponent, OrderSuccessComponent],
  templateUrl: './trading-modal.component.html',
  styleUrl: './trading-modal.component.css'
})
export class TradingModalComponent implements AfterViewInit, OnChanges {
  @Input() isOpen: boolean = false;
  @Input() selectedAsset: Asset | null = null;
  @Input() candleData: CandleResponse[] = [];
  @Input() instrumentId: string = '';
  @Output() close = new EventEmitter<void>();

  @ViewChild('chartContainer') chartContainer: ElementRef | null = null;

  orderType: 'market' | 'limit' = 'market';
  orderSide: 'buy' | 'sell' = 'buy';
  quantity: number = 50;
  limitPrice: number = 0;
  duration: 'day' | 'gtc' = 'day';
  agreedToTerms: boolean = false;
  isPreviewOpen: boolean = false;
  isSuccessOpen: boolean = false;

  selectedTimeframe: string = '3M';
  loadingCandles: boolean = true;

  depthLevels: DepthLevel[] = [];

  timeframes = [
    { label: '1D', days: 1 },
    { label: '1W', days: 7 },
    { label: '1M', days: 30 },
    { label: '3M', days: 90 },
    { label: '1Y', days: 365 }
  ];

  constructor(private apiService: ApiService, private cdr: ChangeDetectorRef) {}

  ngAfterViewInit() {
    console.log('ngAfterViewInit - candleData length:', this.candleData.length);
    if (this.candleData.length > 0) {
      setTimeout(() => this.renderChart(), 100);
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    console.log('ngOnChanges detected:', {
      candleData: changes['candleData']?.currentValue?.length || 0,
      isFirstChange: changes['candleData']?.firstChange
    });
    if (changes['candleData']) {
      const newData = changes['candleData'].currentValue as CandleResponse[];
      if (newData && newData.length > 0) {
        this.loadingCandles = false;
        setTimeout(() => this.renderChart(), 100);
      }
    }
    if (changes['instrumentId'] && this.instrumentId) {
      this.loadMarketDepth();
    }
  }

  loadMarketDepth() {
    this.apiService.getMarketDepth(this.instrumentId).subscribe({
      next: (depth) => {
        this.depthLevels = depth;
        this.cdr.detectChanges();
        console.log('Market depth loaded:', depth.length, 'levels');
      },
      error: (e) => console.error('Failed to load market depth:', e)
    });
  }

  loadCandles(timeframeLabel: string) {
    const timeframe = this.timeframes.find(tf => tf.label === timeframeLabel);
    if (!timeframe) return;

    this.loadingCandles = true;
    this.selectedTimeframe = timeframeLabel;

    this.apiService.getCandles(this.instrumentId, timeframe.days).subscribe({
      next: (candles) => {
        this.candleData = candles;
        this.loadingCandles = false;
        this.renderChart();
        this.cdr.detectChanges();
      },
      error: (e) => {
        console.error('Failed to load candles:', e);
        this.loadingCandles = false;
      }
    });
  }

  renderChart() {
    if (!this.chartContainer || this.candleData.length === 0) {
      console.log('Chart render blocked:', {
        hasContainer: !!this.chartContainer,
        dataLength: this.candleData.length,
        containerRef: this.chartContainer?.nativeElement
      });
      return;
    }

    console.log('Rendering chart with', this.candleData.length, 'candles');

    const container = this.chartContainer.nativeElement;

    // Ensure container has dimensions
    if (!container.clientWidth || !container.clientHeight) {
      console.warn('Container has no dimensions:', {
        width: container.clientWidth,
        height: container.clientHeight
      });
    }

    container.innerHTML = '';

    const width = container.clientWidth || 400;
    const height = 300;

    console.log('Chart dimensions:', { width, height });

    const chart = createChart(container, {
      layout: {
        background: { type: ColorType.Solid, color: '#1e1e1e' },
        textColor: '#d1d5db'
      },
      width: width,
      height: height,
      timeScale: { timeVisible: true, secondsVisible: false }
    });

    const candlestickSeries = chart.addSeries(CandlestickSeries, {
      upColor: '#26a69a',
      downColor: '#ef5350',
      borderUpColor: '#26a69a',
      borderDownColor: '#ef5350',
      wickUpColor: '#26a69a',
      wickDownColor: '#ef5350'
    });

    const chartData = this.candleData.map(c => ({
      time: Math.floor(new Date(c.date).getTime() / 1000) as UTCTimestamp,
      open: c.open,
      high: c.high,
      low: c.low,
      close: c.close
    }));

    console.log('Chart data points:', chartData.length);
    candlestickSeries.setData(chartData);
    chart.timeScale().fitContent();
    console.log('Chart rendered successfully');
  }

  selectTimeframe(timeframeLabel: string) {
    this.loadCandles(timeframeLabel);
  }

  get estimatedValue(): number {
    if (!this.selectedAsset) return 0;
    return this.quantity * this.selectedAsset.price;
  }

  get estimatedFees(): number {
    return 0; // Placeholder for fee calculation
  }

  get orderTotal(): number {
    return this.estimatedValue + this.estimatedFees;
  }

  get estFillPrice(): string {
    if (!this.selectedAsset) return '$0.00';
    if (this.orderType === 'limit') {
      return `$${this.limitPrice.toFixed(2)}`;
    }
    return `$${this.selectedAsset.price.toFixed(2)}`;
  }

  closeModal() {
    this.close.emit();
  }

  placeOrder() {
    if (!this.agreedToTerms) {
      alert('Please agree to market conditions before placing an order');
      return;
    }
    this.isSuccessOpen = true;
  }

  previewOrder() {
    this.isPreviewOpen = true;
  }

  closePreview() {
    this.isPreviewOpen = false;
  }

  editOrder() {
    this.isPreviewOpen = false;
  }

  confirmOrder() {
    this.isPreviewOpen = false;
    this.isSuccessOpen = true;
  }

  onSuccessClose() {
    this.isSuccessOpen = false;
    this.closeModal();
  }

  onReturnToDashboard() {
    this.onSuccessClose();
  }

  onViewActivity() {
    // Navigate to activity or show activity view
    console.log('View activity clicked');
    this.onSuccessClose();
  }
}
