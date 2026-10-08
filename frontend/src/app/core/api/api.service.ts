import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
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
  TokenValidationResponse
} from './api.models';

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

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/logout`, {});
  }

  validateToken(token: string): Observable<TokenValidationResponse> {
    return this.http.post<TokenValidationResponse>(`${this.apiUrl}/auth/validate`, { token });
  }

  /**
   * Get all instruments (without live prices)
   */
  getInstruments(limit: number = 50, offset: number = 0): Observable<PaginatedResponse<Instrument>> {
    return this.http.get<PaginatedResponse<Instrument>>(
      `${this.apiUrl}/instruments?limit=${limit}&offset=${offset}`
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

  /**
   * Get the first account for a client
   */
  getAccountByClientId(clientId: string): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(
      `${this.apiUrl}/clients/${clientId}/accounts?limit=1&offset=0`
    ).pipe(
      // Map the paginated response to just the first account
      map((response: any) => {
        if (response.items && response.items.length > 0) {
          return response.items[0];
        }
        throw new Error('No account found for client');
      })
    );
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
   * Fill/execute an order at a specified price
   */
  fillOrder(orderId: string, price: number): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(
      `${this.apiUrl}/orders/${orderId}/fill?price=${price}`,
      {}
    );
  }
}
