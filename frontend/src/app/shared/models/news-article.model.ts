import { NewsResponse } from '../../services/news.service';

export interface NewsArticle {
  id: string;
  category: string;
  title: string;
  description: string;
  timestamp: string;
  assets: string[];
  market?: string;
  sourceUrl?: string;
  imageUrl?: string;
}

export function toNewsArticle(news: NewsResponse): NewsArticle {
  const timestamp = formatNewsTimestamp(news.publishedTimestamp);
  return {
    id: news.newsId,
    category: news.category === 'company' ? 'Company News' : 'Market News',
    title: news.headline,
    description: news.summary || '',
    timestamp,
    assets: [news.symbol],
    market: `${news.source} • ${timestamp}`,
    sourceUrl: news.sourceUrl,
    imageUrl: news.imageUrl
  };
}

/** Finnhub timestamps are in seconds. */
export function formatNewsTimestamp(timestamp: number): string {
  if (!timestamp) return 'Recently';
  const date = new Date(timestamp * 1000);
  const diffMs = Date.now() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
  if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  return date.toLocaleDateString();
}
