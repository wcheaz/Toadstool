import { ApiTimestamp } from '../core/api/api.models';

/** Parse an API timestamp: ISO strings, or epoch seconds (some endpoints serialise numerically). */
export function toDate(value: ApiTimestamp | null | undefined): Date | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const numeric = typeof value === 'number' ? value : Number(value);
  if (Number.isFinite(numeric)) {
    // Seconds below ~year 2286 when read as seconds; anything larger is already milliseconds.
    return new Date(numeric < 1e11 ? numeric * 1000 : numeric);
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

export function formatMoney(value: number | string | null | undefined, signed = false): string {
  const amount = Number(value ?? 0);
  if (!Number.isFinite(amount)) {
    return '--';
  }
  const formatted = Math.abs(amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  if (amount < 0) {
    return `-$${formatted}`;
  }
  return `${signed && amount > 0 ? '+' : ''}$${formatted}`;
}

export function formatQuantity(value: number | string | null | undefined): string {
  const quantity = Number(value ?? 0);
  return Number.isFinite(quantity)
    ? quantity.toLocaleString('en-US', { maximumFractionDigits: 6 })
    : '--';
}

export function formatPercent(value: number | null | undefined, signed = true): string {
  if (value === null || value === undefined || !Number.isFinite(value)) {
    return '--';
  }
  return `${signed && value > 0 ? '+' : ''}${value.toFixed(2)}%`;
}

/** "14 Oct 2026, 09:05 UTC" */
export function formatDateTime(value: ApiTimestamp | null | undefined): string {
  const date = toDate(value);
  if (!date) {
    return '--';
  }
  const day = date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' });
  const time = date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', timeZone: 'UTC' });
  return `${day}, ${time} UTC`;
}

export function formatDate(value: ApiTimestamp | null | undefined): string {
  const date = toDate(value);
  return date
    ? date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' })
    : '--';
}

/** ORDER_SUBMITTED -> "Order submitted" */
export function humanize(value: string | null | undefined): string {
  if (!value) {
    return '--';
  }
  const words = value.replace(/[_-]+/g, ' ').toLowerCase().trim();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

export function initials(name: string | null | undefined): string {
  return (name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
    .slice(0, 2);
}
