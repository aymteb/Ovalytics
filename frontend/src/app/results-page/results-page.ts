import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, interval, switchMap } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Competition, Match } from '../models';
import { TeamLogo } from '../team-logo/team-logo';

interface MatchdayGroup {
  matchday: number;
  matches: Match[];
}

@Component({
  selector: 'app-results-page',
  imports: [DatePipe, RouterLink, TeamLogo],
  templateUrl: './results-page.html',
  styleUrl: './results-page.css',
})
export class ResultsPage implements OnInit {
  competitions = signal<Competition[]>([]);
  selectedCode = signal('TOP14');
  errorMessage = signal('');
  loading = signal(true);
  liveMatches = signal<Match[]>([]);

  private matches = signal<Match[]>([]);
  private readonly destroyRef = inject(DestroyRef);

  matchdayGroups = computed(() => this.buildMatchdayGroups(this.matches()));

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getCompetitions().subscribe({
      next: (competitions) => {
        const ordered = this.orderCompetitions(competitions);
        this.competitions.set(ordered);
        const preferred =
          ordered.find((c) => c.code === 'TOP14') ?? ordered[0];
        if (preferred) {
          this.selectedCode.set(preferred.code);
          this.startPolling();
          this.loadResults(preferred.code);
        }
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les compétitions.');
        this.loading.set(false);
      },
    });
  }

  onCompetitionChange(code: string): void {
    this.selectedCode.set(code);
    this.loading.set(true);
    this.errorMessage.set('');
    this.loadResults(code);
  }

  private startPolling(): void {
    interval(30_000)
      .pipe(
        switchMap(() => this.fetchResults(this.selectedCode())),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (data) => this.applyResults(data),
        error: () => {
          this.errorMessage.set('Impossible de charger les résultats.');
          this.loading.set(false);
        },
      });
  }

  private loadResults(code: string): void {
    this.fetchResults(code).subscribe({
      next: (data) => this.applyResults(data),
      error: () => {
        this.errorMessage.set('Impossible de charger les résultats.');
        this.loading.set(false);
      },
    });
  }

  private fetchResults(code: string) {
    return forkJoin({
      live: this.api.getMatches('LIVE', code),
      finished: this.api.getMatches('FINISHED', code),
    });
  }

  private applyResults(data: { live: Match[]; finished: Match[] }): void {
    this.liveMatches.set(
      [...data.live].sort((a, b) => a.kickoffAt.localeCompare(b.kickoffAt)),
    );
    this.matches.set(data.finished);
    this.loading.set(false);
  }

  private buildMatchdayGroups(matches: Match[]): MatchdayGroup[] {
    const byDay = new Map<number, Match[]>();
    for (const match of matches) {
      const list = byDay.get(match.matchday) ?? [];
      list.push(match);
      byDay.set(match.matchday, list);
    }
    return [...byDay.entries()]
      .sort(([a], [b]) => b - a)
      .map(([matchday, dayMatches]) => ({
        matchday,
        matches: [...dayMatches].sort((a, b) =>
          b.kickoffAt.localeCompare(a.kickoffAt),
        ),
      }));
  }

  private orderCompetitions(competitions: Competition[]): Competition[] {
    return [...competitions].sort((a, b) => {
      if (a.code === 'TOP14') {
        return -1;
      }
      if (b.code === 'TOP14') {
        return 1;
      }
      return a.name.localeCompare(b.name, 'fr');
    });
  }
}
