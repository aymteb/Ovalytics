import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, interval, switchMap } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Competition, Match, StandingRow } from '../models';
import {
  clubRoute,
  competitionSortRank,
  hasStandingsCompetition,
} from '../competition-display';
import { TeamLogo } from '../team-logo/team-logo';

interface StandingPool {
  name: string | null;
  rows: StandingRow[];
}

@Component({
  selector: 'app-standings-page',
  imports: [RouterLink, TeamLogo],
  templateUrl: './standings-page.html',
  styleUrl: './standings-page.css',
})
export class StandingsPage implements OnInit {
  competitions = signal<Competition[]>([]);
  selectedCode = signal('TOP14');
  rows = signal<StandingRow[]>([]);
  errorMessage = signal('');
  loading = signal(true);
  hasLiveMatches = signal(false);
  readonly clubRoute = clubRoute;

  pools = computed(() => this.buildPools(this.rows()));

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private api: CompetitionApi,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.api.getCompetitions().subscribe({
      next: (competitions) => {
        const ordered = this.orderCompetitions(competitions);
        this.competitions.set(ordered);
        this.startPolling();
        this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
          const fromQuery = params.get('competition');
          const preferred =
            ordered.find((c) => c.code === fromQuery) ??
            ordered.find((c) => c.code === 'TOP14') ??
            ordered[0];
          if (!preferred) {
            return;
          }
          if (this.selectedCode() === preferred.code && this.rows().length > 0) {
            return;
          }
          this.selectedCode.set(preferred.code);
          this.loading.set(true);
          this.errorMessage.set('');
          this.loadStandings(preferred.code);
        });
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
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { competition: code },
      replaceUrl: true,
    });
    this.loadStandings(code);
  }

  private startPolling(): void {
    interval(45_000)
      .pipe(
        switchMap(() => this.fetchStandings(this.selectedCode())),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (data) => this.applyStandings(data),
        error: () => {
          this.errorMessage.set('Impossible de charger le classement.');
          this.loading.set(false);
        },
      });
  }

  private loadStandings(code: string): void {
    this.fetchStandings(code).subscribe({
      next: (data) => this.applyStandings(data),
      error: () => {
        this.errorMessage.set('Impossible de charger le classement.');
        this.loading.set(false);
      },
    });
  }

  private fetchStandings(code: string) {
    return forkJoin({
      standings: this.api.getStandings(code),
      live: this.api.getMatches('LIVE', code),
    });
  }

  private applyStandings(data: {
    standings: StandingRow[];
    live: Match[];
  }): void {
    this.rows.set(data.standings);
    this.hasLiveMatches.set(data.live.length > 0);
    this.loading.set(false);
  }

  private buildPools(rows: StandingRow[]): StandingPool[] {
    const named = rows.some((row) => !!row.pool?.trim());
    if (!named) {
      return [{ name: null, rows }];
    }
    const byPool = new Map<string, StandingRow[]>();
    for (const row of rows) {
      const name = row.pool?.trim() || 'Poule';
      const list = byPool.get(name) ?? [];
      list.push(row);
      byPool.set(name, list);
    }
    return [...byPool.entries()].map(([name, poolRows]) => ({
      name,
      rows: poolRows,
    }));
  }

  private orderCompetitions(competitions: Competition[]): Competition[] {
    return [...competitions]
      .filter((c) => hasStandingsCompetition(c.code))
      .sort(
        (a, b) =>
          competitionSortRank(a.code) - competitionSortRank(b.code) ||
          a.name.localeCompare(b.name, 'fr'),
      );
  }
}
