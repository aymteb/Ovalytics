import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, ViewportScroller } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { Competition, Match } from '../models';
import { competitionSortRank } from '../competition-display';
import { NavBack } from '../nav-back';
import { TeamLogo } from '../team-logo/team-logo';

type FixturesView = 'hub' | 'competition';

interface DateGroup {
  dateKey: string;
  matches: Match[];
}

interface MatchdayGroup {
  matchday: number;
  matches: Match[];
}

@Component({
  selector: 'app-fixtures-page',
  imports: [DatePipe, RouterLink, TeamLogo],
  templateUrl: './fixtures-page.html',
  styleUrl: './fixtures-page.css',
})
export class FixturesPage implements OnInit {
  view = signal<FixturesView>('hub');
  competitions = signal<Competition[]>([]);
  selectedCode = signal('TOP14');
  allMatches = signal<Match[]>([]);
  competitionMatches = signal<Match[]>([]);
  errorMessage = signal('');
  loading = signal(true);

  hubGroups = computed(() => this.buildHubGroups(this.allMatches()));
  matchdayGroups = computed(() =>
    this.buildMatchdayGroups(this.competitionMatches()),
  );

  private readonly navBack = inject(NavBack);
  private readonly viewport = inject(ViewportScroller);

  constructor(
    private api: CompetitionApi,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.api.getCompetitions().subscribe({
      next: (competitions) => {
        const ordered = this.orderCompetitions(competitions);
        this.competitions.set(ordered);
        const fromQuery = this.route.snapshot.queryParamMap.get('competition');
        const preferred =
          ordered.find((c) => c.code === fromQuery) ??
          ordered.find((c) => c.code === 'TOP14') ??
          ordered[0];
        if (preferred) {
          this.selectedCode.set(preferred.code);
          if (fromQuery && ordered.some((c) => c.code === fromQuery)) {
            this.view.set('competition');
          }
        }
        this.loadMatches();
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les compétitions.');
        this.loading.set(false);
      },
    });
  }

  setView(view: FixturesView): void {
    this.view.set(view);
  }

  onCompetitionChange(code: string): void {
    this.selectedCode.set(code);
    this.loadCompetitionMatches(code);
  }

  roundLabel(match: Match): string {
    return `${match.competitionName} · J${match.matchday}`;
  }

  private loadMatches(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.api.getAllMatches('SCHEDULED').subscribe({
      next: (matches) => {
        this.allMatches.set(this.onlyUpcoming(matches));
        this.loadCompetitionMatches(this.selectedCode());
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les matchs à venir.');
        this.loading.set(false);
      },
    });
  }

  private loadCompetitionMatches(code: string): void {
    this.api.getMatches('SCHEDULED', code).subscribe({
      next: (matches) => {
        this.competitionMatches.set(
          this.onlyUpcoming([...matches]).sort((a, b) => {
            if (a.matchday !== b.matchday) {
              return a.matchday - b.matchday;
            }
            return a.kickoffAt.localeCompare(b.kickoffAt);
          }),
        );
        this.loading.set(false);
        this.restoreScroll();
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les matchs à venir.');
        this.loading.set(false);
      },
    });
  }

  private restoreScroll(): void {
    const y = this.navBack.consumeRestoreScroll('/fixtures');
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

  private onlyUpcoming(matches: Match[]): Match[] {
    const now = Date.now();
    return matches.filter((match) => Date.parse(match.kickoffAt) >= now);
  }

  private buildHubGroups(matches: Match[]): DateGroup[] {
    const byDate = new Map<string, Match[]>();
    for (const match of matches) {
      const dateKey = match.kickoffAt.slice(0, 10);
      const list = byDate.get(dateKey) ?? [];
      list.push(match);
      byDate.set(dateKey, list);
    }

    return [...byDate.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([dateKey, dayMatches]) => ({
        dateKey,
        matches: [...dayMatches].sort((a, b) => {
          const byTime = a.kickoffAt.localeCompare(b.kickoffAt);
          if (byTime !== 0) {
            return byTime;
          }
          return (
            competitionSortRank(a.competitionCode) -
            competitionSortRank(b.competitionCode)
          );
        }),
      }));
  }

  private buildMatchdayGroups(matches: Match[]): MatchdayGroup[] {
    const byDay = new Map<number, Match[]>();
    for (const match of matches) {
      if (match.matchday > 35) {
        continue;
      }
      const list = byDay.get(match.matchday) ?? [];
      list.push(match);
      byDay.set(match.matchday, list);
    }
    return [...byDay.entries()]
      .sort(([a], [b]) => a - b)
      .map(([matchday, dayMatches]) => ({
        matchday,
        matches: [...dayMatches].sort((a, b) =>
          a.kickoffAt.localeCompare(b.kickoffAt),
        ),
      }));
  }

  private orderCompetitions(competitions: Competition[]): Competition[] {
    return [...competitions].sort(
      (a, b) =>
        competitionSortRank(a.code) - competitionSortRank(b.code) ||
        a.name.localeCompare(b.name, 'fr'),
    );
  }
}
