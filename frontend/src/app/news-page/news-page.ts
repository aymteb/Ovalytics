import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, NgClass, ViewportScroller } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { NewsItem } from '../models';
import {
  competitionBadgeClass,
  competitionLabel,
} from '../competition-display';
import { NavBack } from '../nav-back';

@Component({
  selector: 'app-news-page',
  imports: [DatePipe, NgClass, RouterLink],
  templateUrl: './news-page.html',
  styleUrl: './news-page.css',
})
export class NewsPage implements OnInit {
  news = signal<NewsItem[]>([]);
  errorMessage = signal('');
  loading = signal(true);

  private readonly api = inject(CompetitionApi);
  private readonly navBack = inject(NavBack);
  private readonly viewport = inject(ViewportScroller);

  ngOnInit(): void {
    this.api.getNews(100).subscribe({
      next: (items) => {
        this.news.set(items);
        this.loading.set(false);
        this.restoreScroll();
      },
      error: () => {
        this.errorMessage.set("Impossible de charger les actualités.");
        this.loading.set(false);
      },
    });
  }

  readonly competitionLabel = competitionLabel;
  readonly badgeClass = competitionBadgeClass;

  private restoreScroll(): void {
    const y = this.navBack.consumeRestoreScroll('/news');
    if (y <= 0) {
      return;
    }
    const apply = () => this.viewport.scrollToPosition([0, y]);
    requestAnimationFrame(() => {
      apply();
      setTimeout(apply, 0);
      setTimeout(apply, 100);
    });
  }
}
