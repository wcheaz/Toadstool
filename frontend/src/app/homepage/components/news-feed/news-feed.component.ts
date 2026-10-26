import { Component, OnInit, ChangeDetectorRef, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NewsService } from '../../../services/news.service';
import { NewsCardComponent } from '../../../shared/components/news-card/news-card.component';
import { NewsDetailModalComponent } from '../../../shared/components/news-detail-modal/news-detail-modal.component';
import { NewsNoticeComponent } from '../../../shared/components/news-notice/news-notice.component';
import { NewsArticle, toNewsArticle } from '../../../shared/models/news-article.model';
import { SymbolLinkService } from '../../symbol-link.service';

const MAX_DASHBOARD_NEWS = 5;
const MAX_LINKED_NEWS = 10;

@Component({
  selector: 'app-news-feed',
  standalone: true,
  imports: [CommonModule, NewsCardComponent, NewsDetailModalComponent, NewsNoticeComponent],
  templateUrl: './news-feed.component.html',
  styleUrls: ['./news-feed.component.css', '../dashboard-module.css']
})
export class NewsFeedComponent implements OnInit {
  /** Symbols that can be picked from article ticker tags (tradable instruments). */
  @Input() selectableSymbols: string[] | null = null;
  /** A ticker tag was clicked; the dashboard links the other cards to it. */
  @Output() symbolSelected = new EventEmitter<string>();

  /** All loaded articles, newest first. */
  private allArticles: NewsArticle[] = [];
  selectedNewsArticle: NewsArticle | null = null;
  loading = false;
  error = '';
  /** Whether this card follows the dashboard's selected symbol; the user can switch back to all news. */
  linked = true;

  private readonly link = inject(SymbolLinkService, { optional: true });

  constructor(private newsService: NewsService, private cdr: ChangeDetectorRef) {}

  /** Symbol the card is currently filtered to, or null when showing all news. */
  get linkedSymbol(): string | null {
    return this.linked ? this.link?.selected()?.symbol ?? null : null;
  }

  /** Symbol available to link back to after the user chose "Show all news". */
  get unlinkedSymbol(): string | null {
    return this.linked ? null : this.link?.selected()?.symbol ?? null;
  }

  get newsArticles(): NewsArticle[] {
    const symbol = this.linkedSymbol;
    return symbol
      ? this.allArticles.filter(article => article.assets.includes(symbol)).slice(0, MAX_LINKED_NEWS)
      : this.allArticles.slice(0, MAX_DASHBOARD_NEWS);
  }

  ngOnInit() {
    this.loading = true;
    this.newsService.getLatestNews().subscribe({
      next: (news) => {
        this.allArticles = [...news]
          .sort((a, b) => (b.publishedTimestamp ?? 0) - (a.publishedTimestamp ?? 0))
          .map(toNewsArticle);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (e) => {
        console.error('Failed to load dashboard news:', e);
        this.error = 'Failed to load news.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  setLinked(linked: boolean) {
    this.linked = linked;
  }

  onSymbolSelected(symbol: string) {
    this.linked = true;
    this.symbolSelected.emit(symbol);
  }

  openNewsDetail(article: NewsArticle) {
    this.selectedNewsArticle = article;
  }

  closeNewsDetail() {
    this.selectedNewsArticle = null;
  }
}
