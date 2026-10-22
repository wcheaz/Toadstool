import { Directive, ElementRef, EventEmitter, HostListener, Input, OnDestroy, Output } from '@angular/core';
import { CardSize, clampCardSize } from '../../dashboard-layout.service';

interface GridMetrics {
  columns: number;
  colWidth: number;
  colGap: number;
  rowHeight: number;
  rowGap: number;
}

/**
 * Resize handle for a dashboard card. Dragging it (mouse, pen or touch) snaps the card to whole grid
 * tracks and emits the new size live, so the other cards reflow while dragging. On a single-column
 * (mobile) grid only the height changes. Arrow keys resize by one track for keyboard users.
 */
@Directive({
  selector: '[appCardResizeHandle]',
  standalone: true,
  host: {
    role: 'button',
    tabindex: '0',
    '[attr.aria-label]': '"Resize card. Drag, or use the arrow keys."',
    title: 'Drag to resize (or use arrow keys)'
  }
})
export class CardResizeHandleDirective implements OnDestroy {
  /** The card's current size. */
  @Input({ required: true }) appCardResizeHandle!: CardSize;
  /** The dashboard grid, used to measure track sizes. */
  @Input({ required: true }) grid!: HTMLElement;
  @Output() resized = new EventEmitter<CardSize>();
  @Output() resizeEnd = new EventEmitter<void>();

  private drag: { pointerId: number; x: number; y: number; width: number; height: number; metrics: GridMetrics } | null = null;

  constructor(private host: ElementRef<HTMLElement>) {}

  ngOnDestroy(): void {
    this.endDrag();
  }

  @HostListener('pointerdown', ['$event'])
  onPointerDown(event: PointerEvent): void {
    if (event.button !== 0) {
      return;
    }
    const card = this.host.nativeElement.closest<HTMLElement>('.dashboard-card');
    if (!card) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    const rect = card.getBoundingClientRect();
    this.drag = { pointerId: event.pointerId, x: event.clientX, y: event.clientY, width: rect.width, height: rect.height, metrics: this.measureGrid() };
    this.host.nativeElement.setPointerCapture(event.pointerId);
    // Stop text selection and keep the resize cursor while the pointer is outside the handle
    document.body.style.userSelect = 'none';
    document.body.style.cursor = 'nwse-resize';
  }

  @HostListener('pointermove', ['$event'])
  onPointerMove(event: PointerEvent): void {
    if (!this.drag || event.pointerId !== this.drag.pointerId) {
      return;
    }
    const { metrics } = this.drag;
    const width = this.drag.width + (event.clientX - this.drag.x);
    const height = this.drag.height + (event.clientY - this.drag.y);
    // A span of n tracks is n * track + (n - 1) * gap wide
    const cols = metrics.columns > 1
      ? Math.min(metrics.columns, (width + metrics.colGap) / (metrics.colWidth + metrics.colGap))
      : this.appCardResizeHandle.cols;
    const rows = (height + metrics.rowGap) / (metrics.rowHeight + metrics.rowGap);
    this.emitIfChanged(clampCardSize({ cols, rows }));
  }

  @HostListener('pointerup', ['$event'])
  @HostListener('pointercancel', ['$event'])
  onPointerUp(event: PointerEvent): void {
    if (this.drag && event.pointerId === this.drag.pointerId) {
      this.endDrag();
      this.resizeEnd.emit();
    }
  }

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    const singleColumn = this.measureGrid().columns <= 1;
    const step: Record<string, CardSize> = {
      ArrowLeft: { cols: singleColumn ? 0 : -1, rows: 0 },
      ArrowRight: { cols: singleColumn ? 0 : 1, rows: 0 },
      ArrowUp: { cols: 0, rows: -1 },
      ArrowDown: { cols: 0, rows: 1 }
    };
    const delta = step[event.key];
    if (!delta) {
      return;
    }
    event.preventDefault();
    const size = this.appCardResizeHandle;
    if (this.emitIfChanged(clampCardSize({ cols: size.cols + delta.cols, rows: size.rows + delta.rows }))) {
      this.resizeEnd.emit();
    }
  }

  private emitIfChanged(size: CardSize): boolean {
    const current = this.appCardResizeHandle;
    if (size.cols === current.cols && size.rows === current.rows) {
      return false;
    }
    this.resized.emit(size);
    return true;
  }

  private endDrag(): void {
    if (this.drag && this.host.nativeElement.hasPointerCapture(this.drag.pointerId)) {
      this.host.nativeElement.releasePointerCapture(this.drag.pointerId);
    }
    this.drag = null;
    document.body.style.userSelect = '';
    document.body.style.cursor = '';
  }

  /** Reads the grid's resolved track sizes, which differ between desktop and mobile layouts. */
  private measureGrid(): GridMetrics {
    const style = getComputedStyle(this.grid);
    const tracks = style.gridTemplateColumns.split(' ').map(parseFloat).filter(Number.isFinite);
    return {
      columns: Math.max(1, tracks.length),
      colWidth: tracks[0] ?? this.grid.clientWidth,
      colGap: parseFloat(style.columnGap) || 0,
      rowHeight: parseFloat(style.gridAutoRows) || 20,
      rowGap: parseFloat(style.rowGap) || 0
    };
  }
}
