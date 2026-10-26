import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NewsArticle } from '../../models/news-article.model';

/** Articles older than this count as "nothing published recently". */
const RECENT_NEWS_WINDOW_MS = 24 * 60 * 60 * 1000;

/**
 * Status shown alongside a news list: loading, error, an empty state when there are no articles,
 * or a banner when the newest article is older than 24 hours.
 */
@Component({
  selector: 'app-news-notice',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './news-notice.component.html',
  styleUrl: './news-notice.component.css'
})
export class NewsNoticeComponent {
  @Input() articles: NewsArticle[] = [];
  @Input() loading = false;
  @Input() error = '';
  /** Overrides the empty-state headline, e.g. "No recent AAPL news". */
  @Input() emptyTitle = 'No news available';

  get isEmpty(): boolean {
    return !this.loading && !this.error && this.articles.length === 0;
  }

  /** Newest publish time, or null when no article has one. */
  get latestPublishedAt(): number | null {
    const times = this.articles.map(article => article.publishedAt).filter((time): time is number => time !== null);
    return times.length > 0 ? Math.max(...times) : null;
  }

  get isStale(): boolean {
    const latest = this.latestPublishedAt;
    return !this.loading && !this.error && latest !== null && Date.now() - latest > RECENT_NEWS_WINDOW_MS;
  }
}
