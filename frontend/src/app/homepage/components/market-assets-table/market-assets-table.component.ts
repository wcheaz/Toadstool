import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Instrument, QuoteResponse } from '../../../core/api/api.models';
import { SymbolLinkService } from '../../symbol-link.service';

/** What an instrument's trades cost and returned, from its BUY/SELL cash events. */
export interface PositionCost {
  /** Total cash paid on buys, fees included. */
  boughtCost: number;
  /** Sum of the trade cash events: buys negative, sells positive. */
  netCash: number;
  /** Units bought minus units sold across those trades. */
  netQuantity: number;
}

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
  /** Trade cash per instrumentId, for all-time P/L. */
  @Input() positionCosts: Map<string, PositionCost> = new Map();
  @Output() tradeRequested = new EventEmitter<TradeRequest>();

  /** Shared dashboard selection; rows pick the symbol shown in linked cards. */
  private readonly link = inject(SymbolLinkService, { optional: true });

  selectInstrument(instrument: Instrument) {
    this.link?.select(instrument);
  }

  isLinked(instrument: Instrument): boolean {
    return this.link?.selected()?.instrumentId === instrument.instrumentId;
  }

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

  /** Today's profit/loss on the position: the quote's per-unit change times units held. */
  getDailyPnl(instrument: Instrument): number {
    const change = this.getQuote(instrument.instrumentId)?.change;
    return Number.isFinite(change) ? (change as number) * this.getHolding(instrument.instrumentId) : 0;
  }

  /**
   * All-time P/L: current value plus everything received from sells, minus everything paid on buys (fees included).
   * Null when the trade history doesn't account for the units held (e.g. positions from before cash events existed).
   */
  getAllTimePnl(instrument: Instrument): { amount: number; percent: number } | null {
    const cost = this.positionCosts.get(instrument.instrumentId);
    const value = this.getHoldingValue(instrument);
    const held = this.getHolding(instrument.instrumentId);
    if (!cost || value === null || cost.boughtCost <= 0 || Math.abs(cost.netQuantity - held) > 1e-6) return null;
    const amount = value + cost.netCash;
    return { amount, percent: (amount / cost.boughtCost) * 100 };
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
