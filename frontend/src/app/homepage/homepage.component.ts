import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, Instrument, QuoteResponse } from '../api.service';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.css'
})
export class HomepageComponent implements OnInit, OnDestroy {
  clientName: string = '';
  currentBalance: number = 142384.50;
  availableForTrading: number = 12450.00;
  isBalanceVisible: boolean = true;
  activeTab: string = 'news';

  instruments: Instrument[] = [];
  instrumentQuotes: Map<string, QuoteResponse> = new Map();
  loadingInstruments: boolean = false;

  private destroy$ = new Subject<void>();

  constructor(private apiService: ApiService) {
    // Get client name from session/localStorage
    this.clientName = localStorage.getItem('clientName') || 'User';
  }

  ngOnInit() {
    this.loadInstruments();
    // Refresh every 5 seconds (matching Fauxnance cache TTL)
    interval(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.refreshQuotes());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadInstruments() {
    this.loadingInstruments = true;
    this.apiService.getInstruments(10, 0).subscribe({
      next: (response) => {
        this.instruments = response.items;
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
        },
        error: (error) => {
          console.error(`Failed to load quote for ${instrument.symbol}:`, error);
          this.loadingInstruments = false;
        }
      });
    });
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

  logout() {
    localStorage.removeItem('clientName');
    window.location.href = '/login';
  }
}
