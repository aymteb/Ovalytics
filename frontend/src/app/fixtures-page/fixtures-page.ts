import { Component, OnInit, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { Competition, Match } from '../models';
import { TeamLogo } from '../team-logo/team-logo';

type FixturesView = 'hub' | 'competition';

interface LeagueGroup {
  code: string;
  name: string;
  matches: Match[];
}

interface DateGroup {
  dateKey: string;
  leagues: LeagueGroup[];
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

  private loadMatches(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.api.getAllMatches('SCHEDULED').subscribe({
      next: (matches) => {
        this.allMatches.set(matches);
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
          [...matches].sort((a, b) => {
            if (a.matchday !== b.matchday) {
              return a.matchday - b.matchday;
            }
            return a.kickoffAt.localeCompare(b.kickoffAt);
          }),
        );
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les matchs à venir.');
        this.loading.set(false);
      },
    });
  }

  private buildHubGroups(matches: Match[]): DateGroup[] {
    const byDate = new Map<string, Map<string, LeagueGroup>>();
    for (const match of matches) {
      const dateKey = match.kickoffAt.slice(0, 10);
      if (!byDate.has(dateKey)) {
        byDate.set(dateKey, new Map());
      }
      const leagues = byDate.get(dateKey)!;
      if (!leagues.has(match.competitionCode)) {
        leagues.set(match.competitionCode, {
          code: match.competitionCode,
          name: match.competitionName,
          matches: [],
        });
      }
      leagues.get(match.competitionCode)!.matches.push(match);
    }

    return [...byDate.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([dateKey, leagues]) => ({
        dateKey,
        leagues: [...leagues.values()]
          .sort((a, b) => this.competitionOrder(a.code) - this.competitionOrder(b.code))
          .map((league) => ({
            ...league,
            matches: [...league.matches].sort((a, b) =>
              a.kickoffAt.localeCompare(b.kickoffAt),
            ),
          })),
      }));
  }

  private buildMatchdayGroups(matches: Match[]): MatchdayGroup[] {
    const byDay = new Map<number, Match[]>();
    for (const match of matches) {
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

  private competitionOrder(code: string): number {
    if (code === 'TOP14') {
      return 0;
    }
    if (code === 'PROD2') {
      return 1;
    }
    return 2;
  }
}
