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
  /** Quantity held per instrumentId for the active account. */
  @Input() holdings: Map<string, number> = new Map();
  @Output() tradeRequested = new EventEmitter<TradeRequest>();

  getQuote(instrumentId: string): QuoteResponse | undefined {
    return this.quotes.get(instrumentId);
  }

  getHolding(instrumentId: string): number {
    return this.holdings.get(instrumentId) ?? 0;
  }

  get ownedInstruments(): Instrument[] {
    return this.instruments.filter((instrument) => this.getHolding(instrument.instrumentId) > 0);
  }

  getHoldingValue(instrument: Instrument): number | null {
    const price = this.getQuote(instrument.instrumentId)?.price;
    return price ? price * this.getHolding(instrument.instrumentId) : null;
  }

  get heldCount(): number {
    return this.ownedInstruments.length;
  }

  getPriceChangeClass(changePercent: number | undefined): string {
    if (!changePercent) return '';
    return changePercent >= 0 ? 'price-up' : 'price-down';
  }

  onTradeClick(instrument: Instrument, quote: QuoteResponse | undefined) {
    this.tradeRequested.emit({ instrument, quote });
  }
}
