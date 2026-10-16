import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NewsResponse {
  newsId: string;
  symbol: string;
  category: string;
  headline: string;
  summary: string;
  source: string;
  publishedTimestamp: number;
  imageUrl: string;
  sourceUrl: string;
  createdAt: string;
  batchId: string;
}

export interface EarningsEvent {
  symbol: string;
  /** yyyy-MM-dd */
  date: string;
  /** 'bmo' (before open), 'amc' (after close), 'dmh' (during hours) or '' */
  hour: string;
  quarter: number | null;
  year: number | null;
  epsEstimate: number | null;
  revenueEstimate: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class NewsService {
  private apiUrl = 'http://localhost:8081/api/news';

  constructor(private http: HttpClient) {}

  getLatestNews(symbols?: string[]): Observable<NewsResponse[]> {
    let url = `${this.apiUrl}/latest`;
    if (symbols && symbols.length > 0) {
      url += `?symbols=${symbols.join(',')}`;
    }
    return this.http.get<NewsResponse[]>(url);
  }

  getMarketNews(): Observable<NewsResponse[]> {
    return this.http.get<NewsResponse[]>(`${this.apiUrl}/market`);
  }

  getEarningsCalendar(): Observable<EarningsEvent[]> {
    return this.http.get<EarningsEvent[]>(`${this.apiUrl}/calendar`);
  }

  refreshNews():Observable<{ success: boolean; message: string; timestamp: number }> {
    return this.http.get<{ success: boolean; message: string; timestamp: number }>(`${this.apiUrl}/refresh`);
  }
}
