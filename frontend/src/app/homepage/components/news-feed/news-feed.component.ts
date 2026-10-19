import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NewsService } from '../../../services/news.service';
import { NewsCardComponent } from '../../../shared/components/news-card/news-card.component';
import { NewsDetailModalComponent } from '../../../shared/components/news-detail-modal/news-detail-modal.component';
import { NewsArticle, toNewsArticle } from '../../../shared/models/news-article.model';

const MAX_DASHBOARD_NEWS = 5;

@Component({
  selector: 'app-news-feed',
  standalone: true,
  imports: [CommonModule, NewsCardComponent, NewsDetailModalComponent],
  templateUrl: './news-feed.component.html',
  styleUrls: ['./news-feed.component.css', '../dashboard-module.css']
})
export class NewsFeedComponent implements OnInit {
  newsArticles: NewsArticle[] = [];
  selectedNewsArticle: NewsArticle | null = null;
  loading = false;
  error = '';

  constructor(private newsService: NewsService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loading = true;
    this.newsService.getLatestNews().subscribe({
      next: (news) => {
        this.newsArticles = [...news]
          .sort((a, b) => (b.publishedTimestamp ?? 0) - (a.publishedTimestamp ?? 0))
          .slice(0, MAX_DASHBOARD_NEWS)
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

  openNewsDetail(article: NewsArticle) {
    this.selectedNewsArticle = article;
  }

  closeNewsDetail() {
    this.selectedNewsArticle = null;
  }
}
