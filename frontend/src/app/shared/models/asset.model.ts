/**
 * Asset displayed across the trading flow (market table, trading modal,
 * order confirmation, order success).
 */
export interface Asset {
  instrumentId: string;
  symbol: string;
  name: string;
  price: number;
  change: number;
  holdings: number;
}
