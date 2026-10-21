import { Injectable } from '@angular/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { ApiService } from '../../core/api/api.service';
import { CandleResponse, CashEventResponse, FillResponse, OrderResponse, QuoteResponse } from '../../core/api/api.models';
import { toDate } from '../../admin-dashboard/admin-format';

export interface PortfolioPoint {
  date: Date;
  value: number;
}

export interface PortfolioChange {
  /** Value change over the window, excluding deposits/withdrawals made inside it. */
  amount: number;
  /** Gain relative to the starting value plus money deposited in the window; null when that base is ~0. */
  percent: number | null;
  netDeposits: number;
}

export interface PortfolioHistoryInput {
  days: number;
  cashBalance: number;
  holdings: { instrumentId: string; quantity: number }[];
  orders: OrderResponse[];
  fills: FillResponse[];
  cashEvents: CashEventResponse[];
  /** Live portfolio value appended as the latest point; null when some holdings have no quote. */
  currentValue: number | null;
}

export interface AccountPortfolioHistory {
  points: PortfolioPoint[];
  change: PortfolioChange | null;
  currentValue: number | null;
}

/** The batch quote endpoint accepts at most 25 ids. */
const BATCH_QUOTE_LIMIT = 25;
const HISTORY_PAGE_SIZE = 1000;
/** Cash events that move money into or out of the account (trades are BUY/SELL and count as performance). */
const EXTERNAL_CASH_EVENT_TYPES = new Set(['DEPOSIT', 'WITHDRAWAL']);
/** Below one cent the starting value is treated as zero, so no percentage is shown. */
const MIN_PERCENT_BASE = 0.01;

/** Rounds to whole cents, removing floating-point residue from summed amounts. */
export function roundToCents(value: number): number {
  return Math.round(value * 100) / 100;
}

/**
 * Investment gain over the chart window: the value change minus deposits/withdrawals made inside it,
 * so money paid in is not reported as growth. The percentage is measured against the starting value
 * plus money deposited during the window, and is omitted when that base is effectively zero.
 */
export function computePortfolioChange(points: PortfolioPoint[], cashEvents: CashEventResponse[]): PortfolioChange | null {
  if (points.length < 2) {
    return null;
  }
  const first = points[0].value;
  const last = points[points.length - 1].value;
  const windowStart = points[0].date.getTime();
  const externalFlows = cashEvents
    .filter((event) => EXTERNAL_CASH_EVENT_TYPES.has(event.eventType))
    .map((event) => ({ amount: Number(event.amount), at: toDate(event.occurredAt)?.getTime() ?? 0 }))
    .filter((flow) => Number.isFinite(flow.amount) && flow.at > windowStart);
  const netDeposits = roundToCents(externalFlows.reduce((sum, flow) => sum + flow.amount, 0));
  const deposited = externalFlows.filter((flow) => flow.amount > 0).reduce((sum, flow) => sum + flow.amount, 0);

  const amount = roundToCents(last - first - netDeposits);
  const base = first + deposited;
  return { amount, percent: base >= MIN_PERCENT_BASE ? (amount / base) * 100 : null, netDeposits };
}

/**
 * Reconstructs an account's portfolio value over time. There is no stored value history, so each
 * day's value = cash + Σ quantity × close, where cash and quantities are rolled back from today's
 * values using the cash events and fills that happened after that day.
 */
@Injectable({ providedIn: 'root' })
export class PortfolioHistoryService {
  constructor(private apiService: ApiService) {}

  /** Fetches candles for every relevant instrument and builds the daily value series. */
  buildHistory(input: PortfolioHistoryInput): Observable<PortfolioPoint[]> {
    const orderById = new Map(input.orders.map((order) => [order.orderId, order]));
    const windowStart = Date.now() - input.days * 86_400_000;

    const instrumentIds = new Set(input.holdings.map((row) => row.instrumentId));
    input.fills.forEach((fill) => {
      const order = orderById.get(fill.orderId);
      const executed = toDate(fill.executedAt);
      if (order && executed && executed.getTime() >= windowStart) {
        instrumentIds.add(order.instrumentId);
      }
    });

    if (instrumentIds.size === 0 && input.cashEvents.length === 0) {
      return of([]);
    }

    const ids = Array.from(instrumentIds);
    const candleRequests = ids.map((id) => this.apiService.getCandles(id, input.days).pipe(catchError(() => of([] as CandleResponse[]))));
    return (candleRequests.length ? forkJoin(candleRequests) : of([] as CandleResponse[][])).pipe(
      map((candleSets) => this.buildPoints(input, ids, candleSets, orderById, windowStart))
    );
  }

  /** Loads everything needed for an account's history (holdings, quotes, orders, fills, cash events). */
  loadAccountHistory(accountId: string, cashBalance: number, days: number): Observable<AccountPortfolioHistory> {
    return forkJoin({
      holdings: this.apiService.getAccountHoldings(accountId).pipe(
        map((response) => response.holdings),
        catchError(() => of([]))
      ),
      orders: this.apiService.getOrders(accountId, HISTORY_PAGE_SIZE, 0).pipe(
        map((page) => page.items),
        catchError(() => of([] as OrderResponse[]))
      ),
      fills: this.apiService.getFills(accountId, HISTORY_PAGE_SIZE, 0).pipe(
        map((page) => page.items),
        catchError(() => of([] as FillResponse[]))
      ),
      cashEvents: this.apiService.getCashEvents(accountId, HISTORY_PAGE_SIZE, 0).pipe(
        map((page) => page.items),
        catchError(() => of([] as CashEventResponse[]))
      )
    }).pipe(
      switchMap(({ holdings, orders, fills, cashEvents }) => {
        const positions = holdings
          .map((item) => ({ instrumentId: item.instrumentId, quantity: Number(item.quantity) }))
          .filter((item) => Number.isFinite(item.quantity) && item.quantity > 0);
        return this.fetchQuotes(positions.map((item) => item.instrumentId)).pipe(
          switchMap((quotes) => {
            const priced = positions.every((item) => Number.isFinite(quotes[item.instrumentId]?.price));
            const holdingsValue = positions.reduce((sum, item) => sum + item.quantity * (quotes[item.instrumentId]?.price ?? 0), 0);
            const currentValue = priced ? roundToCents(cashBalance + holdingsValue) : null;
            return this.buildHistory({ days, cashBalance, holdings: positions, orders, fills, cashEvents, currentValue }).pipe(
              map((points) => ({ points, change: computePortfolioChange(points, cashEvents), currentValue }))
            );
          })
        );
      })
    );
  }

  private fetchQuotes(ids: string[]): Observable<{ [instrumentId: string]: QuoteResponse }> {
    if (ids.length === 0) {
      return of({});
    }
    const batches: string[][] = [];
    for (let i = 0; i < ids.length; i += BATCH_QUOTE_LIMIT) {
      batches.push(ids.slice(i, i + BATCH_QUOTE_LIMIT));
    }
    return forkJoin(batches.map((batch) => this.apiService.getBatchQuotes(batch).pipe(catchError(() => of({})))))
      .pipe(map((results) => Object.assign({}, ...results)));
  }

  private buildPoints(input: PortfolioHistoryInput, ids: string[], candleSets: CandleResponse[][],
                      orderById: Map<string, OrderResponse>, windowStart: number): PortfolioPoint[] {
    const closes = new Map<string, Map<string, number>>();
    const dayKeys = new Set<string>();
    ids.forEach((id, index) => {
      const byDay = new Map<string, number>();
      (candleSets[index] ?? []).forEach((candle) => {
        const date = toDate(candle.date);
        if (date && Number.isFinite(candle.close)) {
          const key = date.toISOString().slice(0, 10);
          byDay.set(key, candle.close);
          dayKeys.add(key);
        }
      });
      closes.set(id, byDay);
    });

    const currentQuantity = new Map(input.holdings.map((row) => [row.instrumentId, row.quantity]));
    const signedFills = input.fills
      .filter((fill) => fill.status === 'Filled')
      .map((fill) => {
        const order = orderById.get(fill.orderId);
        const quantity = Number(fill.quantity) * (order?.side === 'SELL' ? -1 : 1);
        return { instrumentId: order?.instrumentId ?? '', quantity, at: toDate(fill.executedAt)?.getTime() ?? 0 };
      });
    const cashMoves = input.cashEvents.map((event) => ({ amount: Number(event.amount), at: toDate(event.occurredAt)?.getTime() ?? 0 }));
    const lastClose = new Map<string, number>();
    const points: PortfolioPoint[] = [];
    const now = Date.now();

    Array.from(dayKeys).sort().forEach((key) => {
      const endOfDay = new Date(`${key}T23:59:59.999Z`).getTime();
      ids.forEach((id) => {
        const close = closes.get(id)?.get(key);
        if (close !== undefined) {
          lastClose.set(id, close);
        }
      });
      // Skip days before the window, and today's not-yet-finished day when the live value covers it
      if (endOfDay < windowStart || (input.currentValue !== null && endOfDay >= now)) {
        return;
      }
      const cash = input.cashBalance - cashMoves.filter((move) => move.at > endOfDay).reduce((sum, move) => sum + move.amount, 0);
      let value = cash;
      ids.forEach((id) => {
        const later = signedFills.filter((fill) => fill.instrumentId === id && fill.at > endOfDay)
          .reduce((sum, fill) => sum + fill.quantity, 0);
        const quantity = Math.max(0, (currentQuantity.get(id) ?? 0) - later);
        value += quantity * (lastClose.get(id) ?? 0);
      });
      points.push({ date: new Date(endOfDay), value: roundToCents(value) });
    });

    if (input.currentValue !== null) {
      points.push({ date: new Date(now), value: input.currentValue });
    }
    return points;
  }
}
