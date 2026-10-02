import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, CandleResponse } from '../api.service';
import { createChart, ColorType, LineSeries, UTCTimestamp } from 'lightweight-charts';

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
  imports: [CommonModule, FormsModule],
  templateUrl: './trading-modal.component.html',
  styleUrl: './trading-modal.component.css'
})
export class TradingModalComponent implements AfterViewInit {
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

  selectedTimeframe: string = '3M';
  loadingCandles: boolean = false;

  timeframes = [
    { label: '1D', days: 1 },
    { label: '1W', days: 7 },
    { label: '1M', days: 30 },
    { label: '3M', days: 90 },
    { label: '1Y', days: 365 }
  ];

  constructor(private apiService: ApiService, private cdr: ChangeDetectorRef) {}

  ngAfterViewInit() {
    if (this.candleData.length > 0) {
      this.renderChart();
    }
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
    if (!this.chartContainer || this.candleData.length === 0) return;

    const container = this.chartContainer.nativeElement;
    container.innerHTML = '';

    const chart = createChart(container, {
      layout: {
        background: { type: ColorType.Solid, color: '#1e1e1e' },
        textColor: '#d1d5db'
      },
      width: container.clientWidth,
      height: 300,
      timeScale: { timeVisible: true, secondsVisible: false }
    });

    const lineSeries = chart.addSeries(LineSeries, { color: '#26a69a', lineWidth: 2 });

    const chartData = this.candleData.map(c => ({
      time: Math.floor(new Date(c.date).getTime() / 1000) as UTCTimestamp,
      value: c.close
    }));

    lineSeries.setData(chartData);
    chart.timeScale().fitContent();
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
    alert(`${this.orderSide.toUpperCase()} order for ${this.quantity} units of ${this.selectedAsset?.symbol} placed!`);
    this.closeModal();
  }

  previewOrder() {
    alert('Order preview: ' + JSON.stringify({
      side: this.orderSide,
      symbol: this.selectedAsset?.symbol,
      quantity: this.quantity,
      orderType: this.orderType,
      total: this.orderTotal
    }));
  }
}
