import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, startWith, switchMap } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Match, NewsItem, StandingRow } from '../models';
import { NewsNav } from '../news-nav';
import { TeamLogo } from '../team-logo/team-logo';

@Component({
  selector: 'app-home-page',
  imports: [DatePipe, NgClass, RouterLink, TeamLogo],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage implements OnInit {
  news = signal<NewsItem[]>([]);
  liveMatches = signal<Match[]>([]);
  weekendFixtures = signal<Match[]>([]);
  standingsTop = signal<StandingRow[]>([]);
  standingsBottom = signal<StandingRow[]>([]);
  errorMessage = signal('');
  loading = signal(true);
  fixturesLoading = signal(true);

  private readonly destroyRef = inject(DestroyRef);
  private readonly newsNav = inject(NewsNav);

  constructor(private api: CompetitionApi) {}

  openArticle(): void {
    this.newsNav.leaveFromHome();
  }

  ngOnInit(): void {
    this.api.getNews(4).subscribe({
      next: (items) => {
        this.news.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set("Impossible de charger le fil d'actualités.");
        this.loading.set(false);
      },
    });

    this.api.getStandings('TOP14').subscribe({
      next: (rows) => {
        this.standingsTop.set(rows.slice(0, 3));
        this.standingsBottom.set(rows.length > 5 ? rows.slice(-2) : []);
      },
      error: () => {
        this.standingsTop.set([]);
        this.standingsBottom.set([]);
      },
    });

    this.api.getMatches('SCHEDULED', 'TOP14').subscribe({
      next: (matches) => {
        const sorted = [...matches].sort((a, b) =>
          a.kickoffAt.localeCompare(b.kickoffAt),
        );
        if (sorted.length === 0) {
          this.weekendFixtures.set([]);
        } else {
          const matchday = sorted[0].matchday;
          this.weekendFixtures.set(
            sorted.filter((m) => m.matchday === matchday).slice(0, 3),
          );
        }
        this.fixturesLoading.set(false);
      },
      error: () => {
        this.weekendFixtures.set([]);
        this.fixturesLoading.set(false);
      },
    });

    interval(30_000)
      .pipe(
        startWith(0),
        switchMap(() => this.api.getAllMatches('LIVE')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (matches) => {
          this.liveMatches.set(
            [...matches].sort((a, b) => a.kickoffAt.localeCompare(b.kickoffAt)),
          );
        },
        error: () => this.liveMatches.set([]),
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
}
