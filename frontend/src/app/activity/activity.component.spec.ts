import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ActivityComponent } from './activity.component';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';
import { OrderResponse, PaginatedResponse } from '../core/api/api.models';

describe('ActivityComponent transaction statistics', () => {
  let fixture: ComponentFixture<ActivityComponent>;
  const getOrders = vi.fn<ApiService['getOrders']>();
  const getAccountId = vi.fn<AuthService['getAccountId']>();

  function order(submittedAt: string | number, id = String(submittedAt)): OrderResponse {
    return {
      orderId: id,
      accountId: 'account-1',
      instrumentId: 'instrument-1',
      side: 'BUY',
      quantity: '1',
      idempotencyKey: id,
      status: 'FILLED',
      submittedAt
    };
  }

  function page(items: OrderResponse[], offset = 0, hasMore = false, totalCount = items.length):
      PaginatedResponse<OrderResponse> {
    return { items, pagination: { limit: 1000, offset, hasMore, totalCount } };
  }

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 23, 12));
    getOrders.mockReset();
    getAccountId.mockReset().mockReturnValue('account-1');
    getOrders.mockReturnValue(of(page([])));
    await TestBed.configureTestingModule({
      imports: [ActivityComponent],
      providers: [
        { provide: ApiService, useValue: { getOrders, getInstruments: vi.fn().mockReturnValue(of({ items: [] })) } },
        { provide: AuthService, useValue: { getAccountId } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ActivityComponent);
  });

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('renders all four cards and counts exact rolling boundaries across statistics pages, not the table page', () => {
    const dayMs = 24 * 60 * 60 * 1000;
    const day = Date.now() - dayMs;
    const week = Date.now() - 7 * dayMs;
    const month = Date.now() - 30 * dayMs;
    const first = [order(day / 1000), order(day - 1), order(String(week))];
    const second = [order(week - 1), order(new Date(month).toISOString()), order(month - 1)];
    getOrders.mockImplementation((_account, limit, offset) => {
      if (limit === 20) return of(page([first[0]], offset, true, 2001));
      return of(offset === 0 ? page(first, 0, true, 2001) : page(second, 3, true, 2001));
    });

    fixture.detectChanges();

    expect(fixture.componentInstance.stats).toEqual({
      allTimeExecutions: (2001).toLocaleString(),
      monthlyExecutions: '5',
      weeklyExecutions: '3',
      dailyExecutions: '1'
    });
    expect(getOrders).toHaveBeenCalledWith('account-1', 1000, 3);
    expect(getOrders).toHaveBeenCalledTimes(3);
    const element: HTMLElement = fixture.nativeElement;
    expect(Array.from(element.querySelectorAll('.stat-label'), card => card.textContent?.trim())).toEqual([
      'ALL-TIME TRANSACTIONS', 'MONTHLY TRANSACTIONS', 'WEEKLY TRANSACTIONS', 'DAILY TRANSACTIONS'
    ]);
    expect(fixture.componentInstance.transactions).toHaveLength(1);
    expect(Array.from(element.querySelectorAll('.stat-unit'), card => card.textContent?.trim())).toEqual([
      'Executions', 'Past 30 days', 'Past 7 days', 'Past 24 hours'
    ]);
  });

  it('counts elapsed days across month and daylight saving boundaries rather than calendar periods', () => {
    vi.setSystemTime(new Date('2026-11-02T12:00:00-05:00'));
    const dayMs = 24 * 60 * 60 * 1000;
    getOrders.mockReturnValue(of(page([
      order(Date.now()),
      order(new Date(Date.now() - dayMs).toISOString()),
      order(Date.now() - dayMs - 1),
      order(Date.now() - 7 * dayMs),
      order(Date.now() - 30 * dayMs),
      order(Date.now() - 30 * dayMs - 1)
    ])));

    fixture.detectChanges();

    expect(fixture.componentInstance.stats.monthlyExecutions).toBe('5');
    expect(fixture.componentInstance.stats.weeklyExecutions).toBe('4');
    expect(fixture.componentInstance.stats.dailyExecutions).toBe('2');
  });

  it('shows zero for empty periods and excludes future orders', () => {
    getOrders.mockReturnValue(of(page([order(new Date(2026, 9, 24).toISOString())])));

    fixture.detectChanges();

    expect(fixture.componentInstance.stats.monthlyExecutions).toBe('0');
    expect(fixture.componentInstance.stats.weeklyExecutions).toBe('0');
    expect(fixture.componentInstance.stats.dailyExecutions).toBe('0');
  });

  it('shows unavailable rather than partial totals when a later statistics page fails', () => {
    const errorLog = vi.spyOn(console, 'error').mockImplementation(() => {});
    getOrders.mockImplementation((_account, limit, offset) => {
      if (limit === 20) return of(page([], 0, false, 2001));
      return offset === 0
        ? of(page([order(new Date().toISOString())], 0, true, 2001))
        : throwError(() => new Error('Network error'));
    });

    fixture.detectChanges();

    expect(fixture.componentInstance.stats).toEqual({
      allTimeExecutions: (2001).toLocaleString(),
      monthlyExecutions: 'Unavailable',
      weeklyExecutions: 'Unavailable',
      dailyExecutions: 'Unavailable'
    });
    expect(errorLog).toHaveBeenCalled();
  });

  it('reports invalid timestamps instead of silently undercounting', () => {
    const errorLog = vi.spyOn(console, 'error').mockImplementation(() => {});
    getOrders.mockReturnValue(of(page([order('invalid-date')])));

    fixture.detectChanges();

    expect(fixture.componentInstance.stats.dailyExecutions).toBe('Unavailable');
    expect(errorLog).toHaveBeenCalled();
  });

  it('shows unavailable without requesting orders when no account is signed in', () => {
    getAccountId.mockReturnValue('');

    fixture.detectChanges();

    expect(getOrders).not.toHaveBeenCalled();
    expect(Object.values(fixture.componentInstance.stats)).toEqual(Array(4).fill('Unavailable'));
  });

  it('cancels unfinished statistics requests when the component is destroyed', () => {
    const response = new Subject<PaginatedResponse<OrderResponse>>();
    getOrders.mockImplementation((_account, limit) => limit === 20 ? of(page([])) : response.asObservable());
    fixture.detectChanges();
    expect(response.observed).toBe(true);

    fixture.destroy();

    expect(response.observed).toBe(false);
  });
});
