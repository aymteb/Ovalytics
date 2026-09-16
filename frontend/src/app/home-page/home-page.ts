import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, startWith, switchMap } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Match, NewsItem } from '../models';
import { TeamLogo } from '../team-logo/team-logo';

@Component({
  selector: 'app-home-page',
  imports: [DatePipe, RouterLink, TeamLogo],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage implements OnInit {
  news = signal<NewsItem[]>([]);
  liveMatches = signal<Match[]>([]);
  errorMessage = signal('');
  liveErrorMessage = signal('');
  loading = signal(true);
  liveLoading = signal(true);

  private readonly destroyRef = inject(DestroyRef);

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getNews(12).subscribe({
      next: (items) => {
        this.news.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set("Impossible de charger le fil d'actualités.");
        this.loading.set(false);
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
          this.liveLoading.set(false);
        },
        error: () => {
          this.liveErrorMessage.set('Impossible de charger les matchs en direct.');
          this.liveLoading.set(false);
        },
      });
  }

  competitionLabel(code: string | null): string {
    if (code === 'TOP14') {
      return 'Top 14';
    }
    if (code === 'PROD2') {
      return 'Pro D2';
    }
    return '';
  }
}
