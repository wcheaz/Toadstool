import {
  AfterViewInit, ChangeDetectorRef, Component, effect, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AreaSeries, ColorType, createChart, IChartApi, ISeriesApi, UTCTimestamp } from 'lightweight-charts';
import { Subject, interval, of } from 'rxjs';
import { catchError, switchMap, takeUntil } from 'rxjs/operators';
import { formatMoney, formatPercent } from '../../../admin-dashboard/admin-format';
import { ThemeService } from '../../../services/theme.service';
import {
  AccountPortfolioHistory, PortfolioChange, PortfolioHistoryService, PortfolioPoint
} from '../../../shared/services/portfolio-history.service';

const REFRESH_INTERVAL_MS = 5 * 60 * 1000;
/** Used until the container has been laid out. */
const FALLBACK_CHART_HEIGHT = 240;

const CHART_THEMES = {
  dark: { text: '#aeb5bb', grid: 'rgba(148, 163, 184, 0.1)', line: '#55b990', top: 'rgba(0, 116, 74, 0.35)', bottom: 'rgba(0, 116, 74, 0.02)' },
  light: { text: '#596268', grid: 'rgba(36, 39, 42, 0.08)', line: '#087a52', top: 'rgba(0, 116, 74, 0.25)', bottom: 'rgba(0, 116, 74, 0.02)' }
};

/**
 * Dashboard widget charting the active account's total portfolio value (cash + holdings) over time.
 * Reloads every 5 minutes and whenever the account or its cash balance changes (trades, deposits, withdrawals).
 */
@Component({
  selector: 'app-portfolio-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portfolio-chart.component.html',
  styleUrls: ['./portfolio-chart.component.css', '../dashboard-module.css']
})
export class PortfolioChartComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() accountId = '';
  @Input() cashBalance: number | null = null;

  readonly ranges = [7, 30, 90];
  readonly formatMoney = formatMoney;
  readonly formatPercent = formatPercent;
  days = 30;
  loading = false;
  error = '';
  points: PortfolioPoint[] = [];
  change: PortfolioChange | null = null;
  currentValue: number | null = null;
  lastUpdated: Date | null = null;

  @ViewChild('chartContainer') private chartContainer?: ElementRef<HTMLDivElement>;
  private chart: IChartApi | null = null;
  private series: ISeriesApi<'Area'> | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private portfolioHistory: PortfolioHistoryService,
    private themeService: ThemeService,
    private cdr: ChangeDetectorRef
  ) {
    effect(() => this.applyTheme(this.themeService.theme()));

    this.reload$.pipe(
      switchMap(() => {
        if (!this.accountId) {
          return of(null);
        }
        this.loading = true;
        this.cdr.detectChanges();
        return this.portfolioHistory.loadAccountHistory(this.accountId, Number(this.cashBalance ?? 0), this.days).pipe(
          catchError((error) => {
            console.error('Failed to load portfolio history:', error);
            this.error = 'Portfolio history is unavailable right now.';
            return of(null);
          })
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe((history) => this.onHistory(history));

    interval(REFRESH_INTERVAL_MS).pipe(takeUntil(this.destroy$)).subscribe(() => this.reload$.next());
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['accountId'] || changes['cashBalance']) {
      this.reload$.next();
    }
  }

  ngAfterViewInit(): void {
    this.createChart();
    this.renderPoints();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.resizeObserver?.disconnect();
    this.chart?.remove();
    this.chart = null;
    this.series = null;
  }

  setRange(days: number): void {
    if (days !== this.days) {
      this.days = days;
      this.reload$.next();
    }
  }

  private onHistory(history: AccountPortfolioHistory | null): void {
    this.loading = false;
    if (history) {
      this.error = '';
      this.points = history.points;
      this.change = history.change;
      this.currentValue = history.currentValue;
      this.lastUpdated = new Date();
      this.renderPoints();
    }
    this.cdr.detectChanges();
  }

  private createChart(): void {
    const container = this.chartContainer?.nativeElement;
    if (!container || this.chart) {
      return;
    }
    this.chart = createChart(container, {
      width: container.clientWidth || 400,
      height: container.clientHeight || FALLBACK_CHART_HEIGHT,
      layout: { background: { type: ColorType.Solid, color: 'transparent' }, attributionLogo: false },
      rightPriceScale: { borderVisible: false },
      timeScale: { borderVisible: false, timeVisible: false },
      localization: { priceFormatter: (price: number) => formatMoney(price) },
      handleScroll: false,
      handleScale: false
    });
    this.series = this.chart.addSeries(AreaSeries, { lineWidth: 2, priceLineVisible: false });
    this.applyTheme(this.themeService.theme());

    // Follow the card as it is resized (width/height presets) or moved between grid columns
    this.resizeObserver = new ResizeObserver(() => {
      if (container.clientWidth > 0 && container.clientHeight > 0) {
        this.chart?.resize(container.clientWidth, container.clientHeight);
      }
    });
    this.resizeObserver.observe(container);
  }

  private renderPoints(): void {
    if (!this.series || !this.chart) {
      return;
    }
    // lightweight-charts needs strictly increasing times (seconds)
    const data: { time: UTCTimestamp; value: number }[] = [];
    this.points.forEach((point) => {
      const time = Math.floor(point.date.getTime() / 1000) as UTCTimestamp;
      if (data.length === 0 || time > data[data.length - 1].time) {
        data.push({ time, value: point.value });
      }
    });
    this.series.setData(data);
    this.chart.timeScale().fitContent();
  }

  private applyTheme(theme: 'dark' | 'light'): void {
    const colors = CHART_THEMES[theme];
    this.chart?.applyOptions({
      layout: { textColor: colors.text },
      grid: { vertLines: { color: colors.grid }, horzLines: { color: colors.grid } }
    });
    this.series?.applyOptions({ lineColor: colors.line, topColor: colors.top, bottomColor: colors.bottom });
  }
}
