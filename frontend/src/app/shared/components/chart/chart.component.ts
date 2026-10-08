import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CandleResponse } from '../../../core/api/api.models';
import { createChart, ColorType, CandlestickSeries, HistogramSeries, UTCTimestamp, IChartApi } from 'lightweight-charts';

export interface ChartTimeframe {
  label: string;
  days: number;
}

/**
 * Candlestick chart (with optional volume series) rendered with
 * lightweight-charts. The parent owns the candle data and timeframe
 * selection; this component only renders and reports user selections.
 */
@Component({
  selector: 'app-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart.component.html',
  styleUrl: './chart.component.css'
})
export class ChartComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() candleData: CandleResponse[] = [];
  @Input() timeframes: ChartTimeframe[] = [];
  @Input() selectedTimeframe: string = '';
  @Input() loading: boolean = false;
  @Input() showVolume: boolean = false;
  @Input() chartHeight: number = 300;
  @Output() timeframeSelected = new EventEmitter<string>();

  @ViewChild('chartContainer') chartContainer: ElementRef | null = null;

  private chartInstance: IChartApi | null = null;

  ngOnDestroy() {
    this.disposeChart();
  }

  private disposeChart() {
    if (this.chartInstance) {
      this.chartInstance.remove();
      this.chartInstance = null;
    }
  }

  ngAfterViewInit() {
    if (this.candleData.length > 0) {
      setTimeout(() => this.renderChart(), 100);
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['candleData']) {
      const newData = changes['candleData'].currentValue as CandleResponse[];
      if (newData && newData.length > 0) {
        setTimeout(() => this.renderChart(), 100);
      }
    }
  }

  selectTimeframe(timeframeLabel: string) {
    this.timeframeSelected.emit(timeframeLabel);
  }

  renderChart() {
    if (!this.chartContainer || this.candleData.length === 0) {
      console.log('Chart render blocked:', {
        hasContainer: !!this.chartContainer,
        dataLength: this.candleData.length
      });
      return;
    }

    const container = this.chartContainer.nativeElement;
    this.disposeChart();
    container.innerHTML = '';

    const width = container.clientWidth || 400;
    const height = this.chartHeight;

    const chart = createChart(container, {
      layout: {
        background: { type: ColorType.Solid, color: '#1e1e1e' },
        textColor: '#d1d5db'
      },
      width: width,
      height: height,
      timeScale: { timeVisible: true, secondsVisible: false }
    });
    this.chartInstance = chart;

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

    candlestickSeries.setData(chartData);

    if (this.showVolume) {
      const volumeSeries = chart.addSeries(HistogramSeries, { color: '#1f77b4' });
      const volumeData = this.candleData.map(c => ({
        time: Math.floor(new Date(c.date).getTime() / 1000) as UTCTimestamp,
        value: c.volume,
        color: c.close >= c.open ? '#26a69a' : '#ef5350'
      }));
      volumeSeries.setData(volumeData);
    }

    chart.timeScale().fitContent();
  }
}
