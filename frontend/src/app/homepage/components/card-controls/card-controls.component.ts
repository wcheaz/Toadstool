import { Component, EventEmitter, Output } from '@angular/core';
import { CdkDragHandle } from '@angular/cdk/drag-drop';

/**
 * Drag handle projected into each dashboard card header. It must sit inside a cdkDrag element;
 * the arrow keys move the card for keyboard users.
 */
@Component({
  selector: 'app-card-controls',
  standalone: true,
  imports: [CdkDragHandle],
  templateUrl: './card-controls.component.html',
  styleUrl: './card-controls.component.css'
})
export class CardControlsComponent {
  /** -1 = move earlier, +1 = move later. */
  @Output() move = new EventEmitter<number>();

  onHandleKeydown(event: KeyboardEvent): void {
    const delta = event.key === 'ArrowLeft' || event.key === 'ArrowUp' ? -1
      : event.key === 'ArrowRight' || event.key === 'ArrowDown' ? 1 : 0;
    if (delta !== 0) {
      event.preventDefault();
      this.move.emit(delta);
    }
  }
}
