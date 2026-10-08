import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Instrument, QuoteResponse } from '../../../core/api/api.models';

export interface TradeRequest {
  instrument: Instrument;
  quote: QuoteResponse | undefined;
}

@Component({
  selector: 'app-market-assets-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './market-assets-table.component.html',
  styleUrls: ['./market-assets-table.component.css', '../dashboard-module.css']
})
export class MarketAssetsTableComponent {
  @Input() instruments: Instrument[] = [];
  @Input() quotes: Map<string, QuoteResponse> = new Map();
  @Input() loading: boolean = false;
  @Output() tradeRequested = new EventEmitter<TradeRequest>();

  getQuote(instrumentId: string): QuoteResponse | undefined {
    return this.quotes.get(instrumentId);
  }

  getPriceChangeClass(changePercent: number | undefined): string {
    if (!changePercent) return '';
    return changePercent >= 0 ? 'price-up' : 'price-down';
  }

  onTradeClick(instrument: Instrument, quote: QuoteResponse | undefined) {
    this.tradeRequested.emit({ instrument, quote });
  }
}
