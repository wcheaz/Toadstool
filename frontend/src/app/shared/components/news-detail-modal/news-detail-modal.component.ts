import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NewsArticle } from '../../models/news-article.model';

@Component({
  selector: 'app-news-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './news-detail-modal.component.html',
  styleUrl: './news-detail-modal.component.css'
})
export class NewsDetailModalComponent {
  /** Modal is shown while an article is set. */
  @Input() article: NewsArticle | null = null;
  @Output() close = new EventEmitter<void>();
}
