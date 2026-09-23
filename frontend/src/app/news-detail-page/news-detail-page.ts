import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, NgClass, ViewportScroller } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { NewsItem } from '../models';
import { NewsNav } from '../news-nav';

@Component({
  selector: 'app-news-detail-page',
  imports: [DatePipe, NgClass, RouterLink],
  templateUrl: './news-detail-page.html',
  styleUrl: './news-detail-page.css',
})
export class NewsDetailPage implements OnInit {
  item = signal<NewsItem | null>(null);
  related = signal<NewsItem[]>([]);
  errorMessage = signal('');
  loading = signal(true);

  private readonly viewport = inject(ViewportScroller);
  private readonly newsNav = inject(NewsNav);

  readonly backPath = this.newsNav.backTo === 'home' ? '/' : '/news';
  readonly backLabel =
    this.newsNav.backTo === 'home' ? '← Accueil' : '← Revenir aux actualités';

  onBackClick(): void {
    if (this.newsNav.backTo === 'news') {
      this.newsNav.prepareBackToList();
    }
  }

  constructor(
    private route: ActivatedRoute,
    private api: CompetitionApi,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      this.viewport.scrollToPosition([0, 0]);
      if (!id) {
        this.item.set(null);
        this.related.set([]);
        this.errorMessage.set('Actualité introuvable.');
        this.loading.set(false);
        return;
      }
      this.loadArticle(id);
    });
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

  private loadArticle(id: number): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.item.set(null);
    this.related.set([]);

    this.api.getNewsItem(id).subscribe({
      next: (item) => {
        this.item.set(item);
        this.loading.set(false);
        this.loadRelated(item);
      },
      error: () => this.loadFromList(id),
    });
  }

  private loadFromList(id: number): void {
    this.api.getNews(100).subscribe({
      next: (items) => {
        const found = items.find((item) => item.id === id);
        if (found) {
          this.item.set(found);
          this.related.set(this.pickRelated(found, items));
        } else {
          this.errorMessage.set('Actualité introuvable.');
        }
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger cette actualité.');
        this.loading.set(false);
      },
    });
  }

  private loadRelated(current: NewsItem): void {
    this.api.getNews(100).subscribe({
      next: (items) => this.related.set(this.pickRelated(current, items)),
      error: () => this.related.set([]),
    });
  }

  private pickRelated(current: NewsItem, items: NewsItem[]): NewsItem[] {
    const others = items.filter((item) => item.id !== current.id);
    const sameCompetition = current.competitionCode
      ? others.filter((item) => item.competitionCode === current.competitionCode)
      : [];
    const rest = others.filter(
      (item) => !sameCompetition.some((same) => same.id === item.id),
    );
    return [...sameCompetition, ...rest].slice(0, 4);
  }
}
