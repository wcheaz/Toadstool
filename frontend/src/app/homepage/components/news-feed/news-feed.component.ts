import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface NewsItem {
  date: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-news-feed',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './news-feed.component.html',
  styleUrls: ['./news-feed.component.css', '../dashboard-module.css']
})
export class NewsFeedComponent {
  // TODO: Remove - mock news data, replace with news API feed
  newsItems: NewsItem[] = [
    {
      date: 'Oct 24, 2024',
      title: 'Federal Reserve Announces Inter-rate Policy Framework',
      description: 'Markets brace for upcoming policy shifts as rate projections consolidate.'
    },
    {
      date: 'Oct 23, 2024',
      title: 'Tech Stocks Rebound Amid Supply Chain Normalization',
      description: 'Semiconductor indices record 3.2% rise following logistic breakthroughs.'
    },
    {
      date: 'Oct 22, 2024',
      title: 'Global Energy Commodities Stabilize After Cartel Accord',
      description: 'Crude futures trading range narrow in pre-session adjustments.'
    }
  ];
}
