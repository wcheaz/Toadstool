import { Injectable, signal } from '@angular/core';
import { Instrument } from '../core/api/api.models';

/**
 * The symbol shared by the dashboard's linked cards: picking an instrument in one card
 * (market assets row, news ticker tag) filters the news card to it and highlights the asset row.
 * Provided by HomepageComponent, so each dashboard instance has its own selection.
 */
@Injectable()
export class SymbolLinkService {
  readonly selected = signal<Instrument | null>(null);

  select(instrument: Instrument): void {
    if (instrument.instrumentId !== this.selected()?.instrumentId) {
      this.selected.set(instrument);
    }
  }
}
