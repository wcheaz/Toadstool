import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NewsArticle } from '../../models/news-article.model';

@Component({
  selector: 'app-news-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './news-card.component.html',
  styleUrl: './news-card.component.css'
})
export class NewsCardComponent {
  @Input({ required: true }) article!: NewsArticle;
  @Output() open = new EventEmitter<NewsArticle>();
  /** When set, ticker tags in this list become buttons that emit assetSelected (e.g. to link dashboard cards). */
  @Input() selectableAssets: string[] | null = null;
  @Output() assetSelected = new EventEmitter<string>();

  isSelectable(asset: string): boolean {
    return !!this.selectableAssets?.includes(asset);
  }

  selectAsset(event: Event, asset: string): void {
    event.stopPropagation();
    this.assetSelected.emit(asset);
  }
}
