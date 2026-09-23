import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, NgClass, ViewportScroller } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { NewsItem } from '../models';
import { NewsNav } from '../news-nav';

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
  private readonly newsNav = inject(NewsNav);
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

  openArticle(): void {
    this.newsNav.leaveFromList();
  }

  competitionLabel(code: string | null): string {
    if (code === 'TOP14') {
      return 'Top 14';
    }
    if (code === 'PROD2') {
      return 'Pro D2';
    }
    if (code === 'SEVENS') {
      return 'Sevens';
    }
    return '';
  }

  badgeClass(code: string | null): string {
    if (code === 'TOP14') {
      return 'bg-primary text-on-primary';
    }
    if (code === 'PROD2') {
      return 'bg-sky-700 text-white';
    }
    return 'bg-muted text-foreground';
  }

  private restoreScroll(): void {
    const y = this.newsNav.consumeRestoreScroll();
    if (y <= 0) {
      return;
    }
    requestAnimationFrame(() => {
      this.viewport.scrollToPosition([0, y]);
    });
  }
}
