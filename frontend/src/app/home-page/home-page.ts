import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, interval, startWith, switchMap } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Match, NewsItem, StandingRow } from '../models';
import {
  competitionBadgeClass,
  competitionLabel,
} from '../competition-display';
import { TeamLogo } from '../team-logo/team-logo';

@Component({
  selector: 'app-home-page',
  imports: [DatePipe, NgClass, RouterLink, TeamLogo],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage implements OnInit {
  readonly standingsCodes = ['TOP14', 'PROD2', 'NAT'] as const;

  news = signal<NewsItem[]>([]);
  liveMatches = signal<Match[]>([]);
  upcomingMatches = signal<Match[]>([]);
  selectedStandingsCode = signal('TOP14');
  standingsTop = signal<StandingRow[]>([]);
  standingsBottom = signal<StandingRow[]>([]);
  standingsLoading = signal(true);
  errorMessage = signal('');
  loading = signal(true);
  fixturesLoading = signal(true);

  private readonly destroyRef = inject(DestroyRef);

  hasLive = computed(() => this.liveMatches().length > 0);

  spotlightMatches = computed(() => {
    const live = this.liveMatches();
    const liveIds = new Set(live.map((match) => match.id));
    const upcoming = this.upcomingMatches().filter((match) => !liveIds.has(match.id));
    return [...live, ...upcoming].slice(0, 3);
  });

  constructor(private api: CompetitionApi) {}

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

    this.loadStandings(this.selectedStandingsCode());

    interval(30_000)
      .pipe(
        startWith(0),
        switchMap(() =>
          forkJoin({
            live: this.api.getAllMatches('LIVE'),
            scheduled: this.api.getAllMatches('SCHEDULED'),
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ live, scheduled }) => {
          this.liveMatches.set(
            [...live].sort((a, b) => a.kickoffAt.localeCompare(b.kickoffAt)),
          );
          this.upcomingMatches.set(
            scheduled
              .filter((match) => this.isUpcoming(match))
              .sort((a, b) => a.kickoffAt.localeCompare(b.kickoffAt)),
          );
          this.fixturesLoading.set(false);
        },
        error: () => {
          this.liveMatches.set([]);
          this.upcomingMatches.set([]);
          this.fixturesLoading.set(false);
        },
      });
  }

  isLive(match: Match): boolean {
    return match.status === 'LIVE';
  }

  selectStandings(code: string): void {
    if (this.selectedStandingsCode() === code) {
      return;
    }
    this.selectedStandingsCode.set(code);
    this.loadStandings(code);
  }

  private loadStandings(code: string): void {
    this.standingsLoading.set(true);
    this.api.getStandings(code).subscribe({
      next: (rows) => {
        if (this.selectedStandingsCode() !== code) {
          return;
        }
        this.standingsTop.set(rows.slice(0, 3));
        this.standingsBottom.set(rows.length > 5 ? rows.slice(-2) : []);
        this.standingsLoading.set(false);
      },
      error: () => {
        if (this.selectedStandingsCode() !== code) {
          return;
        }
        this.standingsTop.set([]);
        this.standingsBottom.set([]);
        this.standingsLoading.set(false);
      },
    });
  }

  private isUpcoming(match: Match): boolean {
    const kickoff = Date.parse(match.kickoffAt);
    if (Number.isNaN(kickoff)) {
      return false;
    }
    return kickoff > Date.now();
  }

  readonly competitionLabel = competitionLabel;
  readonly badgeClass = competitionBadgeClass;
}
