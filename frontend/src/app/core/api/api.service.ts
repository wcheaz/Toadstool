import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LoginResponse,
  RegisterRequest,
  Instrument,
  QuoteResponse,
  CandleResponse,
  DepthLevel,
  QuotePreviewResponse,
  AccountResponse,
  OrderResponse,
  PaginatedResponse,
  AuthResponse,
  LoginRequest,
  AdminLoginRequest,
  TokenValidationResponse
} from './api.models';
import {
  AccessEventRow,
  AdminCashEventRow,
  AdminClientDetail,
  AdminClientSummary,
  CashEventRequest,
  CashEventResponse,
  FillResponse,
  HoldingsResponse,
  InsightsResponse,
  LoginEventRow,
  PlatformActivityRow,
  TradeEventRow
} from './api.models';
import { HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request);
  }

  adminLogin(request: AdminLoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/admin/login`, request);
  }

  refreshSession(refreshToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/refresh`, { token: refreshToken });
  }

  logout(accessToken?: string | null): Observable<void> {
    const headers = accessToken
      ? new HttpHeaders({ Authorization: `Bearer ${accessToken}` })
      : undefined;

    const body = accessToken ? { token: accessToken } : {};
    return this.http.post<void>(`${this.apiUrl}/auth/logout`, body, headers ? { headers } : undefined);
  }

  validateToken(token: string): Observable<TokenValidationResponse> {
    return this.http.post<TokenValidationResponse>(`${this.apiUrl}/auth/validate`, { token });
  }

  /**
   * Get all instruments (without live prices)
   */
  getInstruments(limit: number = 50, offset: number = 0, search?: string): Observable<PaginatedResponse<Instrument>> {
    const params = new URLSearchParams({
      limit: String(limit),
      offset: String(offset)
    });
    if (search?.trim()) {
      params.set('search', search.trim());
    }
    return this.http.get<PaginatedResponse<Instrument>>(
      `${this.apiUrl}/instruments?${params.toString()}`
    );
  }

  /**
   * Get live quote for an instrument
   */
  getInstrumentQuote(instrumentId: string): Observable<QuoteResponse> {
    return this.http.get<QuoteResponse>(
      `${this.apiUrl}/instruments/${instrumentId}/quote`
    );
  }

  /**
   * Get batch quotes for multiple instruments (up to 25)
   */
  getBatchQuotes(instrumentIds: string[]): Observable<{ [key: string]: QuoteResponse }> {
    const params = instrumentIds.join(',');
    return this.http.get<{ [key: string]: QuoteResponse }>(
      `${this.apiUrl}/instruments/quotes/batch?instrumentIds=${params}`
    );
  }

  /**
   * Get historical candles for a chart
   */
  getCandles(instrumentId: string, days: number = 30): Observable<CandleResponse[]> {
    return this.http.get<CandleResponse[]>(
      `${this.apiUrl}/instruments/${instrumentId}/candles?days=${days}`
    );
  }

  /**
   * Get market depth (Level 2) for an instrument
   */
  getMarketDepth(instrumentId: string): Observable<DepthLevel[]> {
    return this.http.get<DepthLevel[]>(
      `${this.apiUrl}/instruments/${instrumentId}/depth`
    );
  }

  /**
   * Get quote preview before placing order
   */
  getQuotePreview(
    accountId: string,
    instrumentId: string,
    side: string,
    quantity: string
  ): Observable<QuotePreviewResponse> {
    return this.http.get<QuotePreviewResponse>(
      `${this.apiUrl}/accounts/${accountId}/quote-preview?instrumentId=${instrumentId}&side=${side}&quantity=${quantity}`
    );
  }

  getAccountsByClientId(clientId: string): Observable<PaginatedResponse<AccountResponse>> {
    return this.http.get<PaginatedResponse<AccountResponse>>(
      `${this.apiUrl}/clients/${clientId}/accounts?limit=1000&offset=0`
    );
  }

  createAccount(clientId: string, accountName: string): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(`${this.apiUrl}/accounts`, { clientId, accountName });
  }

  getAccountById(accountId: string): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.apiUrl}/accounts/${accountId}`);
  }

  updateAccount(accountId: string, request: { accountName?: string; status?: string }): Observable<AccountResponse> {
    return this.http.patch<AccountResponse>(`${this.apiUrl}/accounts/${accountId}`, request);
  }

  getAccountHoldings(accountId: string): Observable<HoldingsResponse> {
    return this.http.get<HoldingsResponse>(
      `${this.apiUrl}/accounts/${accountId}/holdings`
    );
  }

  postCashEvent(accountId: string, request: CashEventRequest): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(`${this.apiUrl}/accounts/${accountId}/cash-events`, request);
  }

  /**
   * Place a new buy/sell order
   */
  placeOrder(
    accountId: string,
    instrumentId: string,
    side: string,
    quantity: string,
    idempotencyKey?: string
  ): Observable<OrderResponse> {
    const headers: any = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }

    const body = {
      instrumentId: instrumentId,
      side: side,
      quantity: quantity
    };

    return this.http.post<OrderResponse>(
      `${this.apiUrl}/accounts/${accountId}/orders`,
      body,
      { headers }
    );
  }

  /**
   * Get paginated orders for an account.
   */
  getOrders(
    accountId: string,
    limit: number = 50,
    offset: number = 0
  ): Observable<PaginatedResponse<OrderResponse>> {
    return this.http.get<PaginatedResponse<OrderResponse>>(
      `${this.apiUrl}/accounts/${accountId}/orders?limit=${limit}&offset=${offset}`
    );
  }

  /**
   * Fill/execute an order at a specified price
   */
  fillOrder(orderId: string, price: number): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(
      `${this.apiUrl}/orders/${orderId}/fill?price=${price}`,
      {}
    );
  }

  getFills(accountId: string, limit: number = 1000, offset: number = 0): Observable<PaginatedResponse<FillResponse>> {
    return this.http.get<PaginatedResponse<FillResponse>>(
      `${this.apiUrl}/accounts/${accountId}/fills?limit=${limit}&offset=${offset}`
    );
  }

  getCashEvents(accountId: string, limit: number = 1000, offset: number = 0): Observable<PaginatedResponse<CashEventResponse>> {
    return this.http.get<PaginatedResponse<CashEventResponse>>(
      `${this.apiUrl}/accounts/${accountId}/cash-events?limit=${limit}&offset=${offset}`
    );
  }

  getClientLoginEvents(clientId: string, limit: number = 20): Observable<PaginatedResponse<LoginEventRow>> {
    return this.http.get<PaginatedResponse<LoginEventRow>>(
      `${this.apiUrl}/clients/${clientId}/login-events?limit=${limit}&offset=0`
    );
  }

  getClientTradeEvents(clientId: string, limit: number = 20): Observable<PaginatedResponse<TradeEventRow>> {
    return this.http.get<PaginatedResponse<TradeEventRow>>(
      `${this.apiUrl}/clients/${clientId}/events?limit=${limit}&offset=0`
    );
  }

  // ---- Admin oversight ----

  searchAdminClients(q: string, status: string, limit: number, offset: number): Observable<PaginatedResponse<AdminClientSummary>> {
    const params = this.params({ q, status, limit, offset });
    return this.http.get<PaginatedResponse<AdminClientSummary>>(`${this.apiUrl}/admin/clients`, { params });
  }

  getAdminClient(clientId: string): Observable<AdminClientDetail> {
    return this.http.get<AdminClientDetail>(`${this.apiUrl}/admin/clients/${clientId}`);
  }

  getAuditLoginEvents(filters: { outcome?: string; emailAttempted?: string; clientId?: string },
                      limit: number, offset: number): Observable<PaginatedResponse<LoginEventRow>> {
    const params = this.params({ ...filters, limit, offset });
    return this.http.get<PaginatedResponse<LoginEventRow>>(`${this.apiUrl}/admin/audit/login-events`, { params });
  }

  getAuditTradeEvents(filters: { action?: string; clientId?: string },
                      limit: number, offset: number): Observable<PaginatedResponse<TradeEventRow>> {
    const params = this.params({ ...filters, limit, offset });
    return this.http.get<PaginatedResponse<TradeEventRow>>(`${this.apiUrl}/admin/audit/trade-events`, { params });
  }

  getAuditAccessEvents(filters: { clientId?: string },
                       limit: number, offset: number): Observable<PaginatedResponse<AccessEventRow>> {
    const params = this.params({ ...filters, limit, offset });
    return this.http.get<PaginatedResponse<AccessEventRow>>(`${this.apiUrl}/admin/audit/access-events`, { params });
  }

  getAuditCashEvents(filters: { clientId?: string; eventType?: string },
                     limit: number, offset: number): Observable<PaginatedResponse<AdminCashEventRow>> {
    const params = this.params({ ...filters, limit, offset });
    return this.http.get<PaginatedResponse<AdminCashEventRow>>(`${this.apiUrl}/admin/audit/cash-events`, { params });
  }

  getPlatformActivity(limit: number = 30): Observable<PaginatedResponse<PlatformActivityRow>> {
    return this.http.get<PaginatedResponse<PlatformActivityRow>>(
      `${this.apiUrl}/admin/analytics/platform-activity?limit=${limit}&offset=0`
    );
  }

  getInsights(timeframe: 'day' | 'week' | 'month' = 'month'): Observable<InsightsResponse> {
    return this.http.get<InsightsResponse>(`${this.apiUrl}/admin/analytics/insights?timeframe=${timeframe}`);
  }

  /** Build query params, skipping empty values. */
  private params(values: { [key: string]: string | number | undefined | null }): HttpParams {
    let params = new HttpParams();
    Object.entries(values).forEach(([key, value]) => {
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        params = params.set(key, String(value).trim());
      }
    });
    return params;
  }
}
