import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, NgClass, ViewportScroller } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { NewsItem } from '../models';
import {
  competitionBadgeClass,
  competitionLabel,
} from '../competition-display';
import { NavBack } from '../nav-back';

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
  private readonly navBack = inject(NavBack);
  private readonly router = inject(Router);

  backPath = computed(() => {
    this.navBack.revision();
    return this.navBack.originFor(this.router.url).path;
  });

  backLabel = computed(() => {
    this.navBack.revision();
    return this.navBack.originFor(this.router.url).label;
  });

  onBackClick(): void {
    this.navBack.prepareBack(this.router.url);
  }

  bodyParagraphs(body: string): string[] {
    return body
      .split(/\n+/)
      .map((part) => part.trim())
      .filter((part) => part.length > 0);
  }

  displayImageUrl(url: string): string {
    if (!url) {
      return url;
    }
    return url.replace(
      /\/images\/view\/([^/]+)\/standard\//i,
      '/images/view/$1/large/',
    );
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

  readonly competitionLabel = competitionLabel;
  readonly badgeClass = competitionBadgeClass;

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
