import { Injectable } from '@angular/core';

export type DashboardCardId = 'news' | 'market' | 'portfolio';

/** A card's position is its index in the layout; its size is in grid tracks. */
export interface DashboardCard {
  id: DashboardCardId;
  /** Columns spanned on the 12-column desktop grid (ignored on mobile, where cards are full width). */
  cols: number;
  /** 20px grid rows spanned. */
  rows: number;
}

export interface CardSize {
  cols: number;
  rows: number;
}

/** Must match the dashboard grid in homepage.component.css. */
export const GRID_COLUMNS = 12;
export const MIN_CARD_COLS = 4;
export const MIN_CARD_ROWS = 8;
export const MAX_CARD_ROWS = 40;

/** News on the left as tall as the two cards stacked on the right. */
export const DEFAULT_DASHBOARD_LAYOUT: readonly DashboardCard[] = [
  { id: 'news', cols: 6, rows: 26 },
  { id: 'market', cols: 6, rows: 13 },
  { id: 'portfolio', cols: 6, rows: 13 }
];

// v2: sizes changed from presets to grid tracks; older saved layouts fall back to the default
const STORAGE_KEY_PREFIX = 'toadstool-dashboard-layout-v2';
const CARD_IDS = new Set<string>(DEFAULT_DASHBOARD_LAYOUT.map(card => card.id));

export function clampCardSize(size: CardSize): CardSize {
  return {
    cols: Math.min(GRID_COLUMNS, Math.max(MIN_CARD_COLS, Math.round(size.cols))),
    rows: Math.min(MAX_CARD_ROWS, Math.max(MIN_CARD_ROWS, Math.round(size.rows)))
  };
}

/**
 * Persists each user's dashboard card order and sizes in this browser. Storage can be unavailable
 * (private windows, blocked site data), so every access falls back to the default layout.
 */
@Injectable({ providedIn: 'root' })
export class DashboardLayoutService {
  load(userId: string): DashboardCard[] {
    try {
      const saved = localStorage.getItem(this.storageKey(userId));
      const parsed = saved ? JSON.parse(saved) : null;
      if (Array.isArray(parsed)) {
        // Drop cards that no longer exist (e.g. a removed card) so the rest of a saved layout is kept
        const known = parsed.filter((card: DashboardCard) => CARD_IDS.has(card?.id));
        if (this.isValidLayout(known)) {
          return this.withMissingCards(known.map((card: DashboardCard) => ({ id: card.id, ...clampCardSize(card) })));
        }
      }
    } catch {
      // Unreadable or blocked storage: use the default layout
    }
    return this.defaultLayout();
  }

  save(userId: string, cards: DashboardCard[]): void {
    try {
      localStorage.setItem(this.storageKey(userId), JSON.stringify(cards));
    } catch {
      // Layout just won't persist
    }
  }

  reset(userId: string): DashboardCard[] {
    try {
      localStorage.removeItem(this.storageKey(userId));
    } catch {
      // Nothing stored to remove
    }
    return this.defaultLayout();
  }

  isDefault(cards: DashboardCard[]): boolean {
    return JSON.stringify(cards) === JSON.stringify(DEFAULT_DASHBOARD_LAYOUT);
  }

  private defaultLayout(): DashboardCard[] {
    return DEFAULT_DASHBOARD_LAYOUT.map(card => ({ ...card }));
  }

  private storageKey(userId: string): string {
    return `${STORAGE_KEY_PREFIX}:${userId || 'anonymous'}`;
  }

  /** Adds cards introduced after the layout was saved, at their default position and size. */
  private withMissingCards(cards: DashboardCard[]): DashboardCard[] {
    const result = [...cards];
    DEFAULT_DASHBOARD_LAYOUT.forEach((card, index) => {
      if (!result.some(existing => existing.id === card.id)) {
        result.splice(Math.min(index, result.length), 0, { ...card });
      }
    });
    return result;
  }

  /** Known cards at most once each, with numeric sizes. */
  private isValidLayout(value: unknown): value is DashboardCard[] {
    if (!Array.isArray(value) || value.length === 0) {
      return false;
    }
    const ids = new Set(value.map(card => card?.id));
    return ids.size === value.length
      && value.every(card => CARD_IDS.has(card?.id) && Number.isFinite(card?.cols) && Number.isFinite(card?.rows));
  }
}
