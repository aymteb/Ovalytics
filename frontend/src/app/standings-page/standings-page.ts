import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, interval, switchMap } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Competition, Match, StandingRow } from '../models';
import { TeamLogo } from '../team-logo/team-logo';

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

  private readonly destroyRef = inject(DestroyRef);

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
          this.loadStandings(preferred.code);
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
