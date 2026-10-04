import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, NgStyle, NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, interval, switchMap } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Match, MatchEvent, TeamForm, VenueRecord } from '../models';
import { NavBack } from '../nav-back';
import { TeamLogo } from '../team-logo/team-logo';

export interface MatchScorer {
  playerName: string;
  teamSide: string;
  points: number;
  tries: number;
  kicks: number;
}

export type TimelineRow =
  | {
      kind: 'event';
      event: MatchEvent;
      homeScore: number;
      awayScore: number;
      indented: boolean;
    }
  | {
      kind: 'halftime';
      homeScore: number;
      awayScore: number;
    };

const PITCH_SPOTS: Record<number, { x: number; y: number }> = {
  // Pack étalé en profondeur pour occuper mieux la moitié (pas collé à la médiane).
  1: { x: 82, y: 76 },
  2: { x: 82, y: 50 },
  3: { x: 82, y: 24 },
  4: { x: 66, y: 66 },
  5: { x: 66, y: 34 },
  6: { x: 52, y: 82 },
  7: { x: 52, y: 18 },
  8: { x: 52, y: 50 },
  9: { x: 40, y: 62 },
  10: { x: 40, y: 38 },
  12: { x: 18, y: 62 },
  13: { x: 18, y: 38 },
  11: { x: 8, y: 84 },
  14: { x: 8, y: 16 },
  15: { x: 2, y: 50 },
};

const PITCH_NUMBERS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15];

@Component({
  selector: 'app-match-page',
  imports: [DatePipe, NgStyle, NgTemplateOutlet, RouterLink, TeamLogo],
  templateUrl: './match-page.html',
  styleUrl: './match-page.css',
})
export class MatchPage implements OnInit {
  match = signal<Match | null>(null);
  errorMessage = signal('');
  loading = signal(true);
  sheetTab = signal<'resume' | 'compositions'>('resume');
  readonly pitchNumbers = PITCH_NUMBERS;

  private readonly navBack = inject(NavBack);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private matchId = 0;

  backPath = computed(() => {
    this.navBack.revision();
    if (this.navBack.hasOrigin(this.router.url)) {
      return this.navBack.originFor(this.router.url).path;
    }
    return this.fallbackBackPath(this.match());
  });

  backLabel = computed(() => {
    this.navBack.revision();
    if (this.navBack.hasOrigin(this.router.url)) {
      return this.navBack.originFor(this.router.url).label;
    }
    const match = this.match();
    if (match?.status === 'LIVE') {
      return '← Direct';
    }
    return '← Retour';
  });

  constructor(
    private route: ActivatedRoute,
    private api: CompetitionApi,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.errorMessage.set('Match introuvable.');
      this.loading.set(false);
      return;
    }
    this.matchId = id;

    this.api.getMatch(id).subscribe({
      next: (match) => {
        this.applyMatch(match);
        this.loading.set(false);
        this.startLivePolling();
      },
      error: () => {
        this.errorMessage.set('Impossible de charger ce match.');
        this.loading.set(false);
      },
    });
  }

  private startLivePolling(): void {
    interval(30_000)
      .pipe(
        filter(() => this.shouldRefreshMatch()),
        switchMap(() => this.api.getMatch(this.matchId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (match) => this.applyMatch(match),
      });
  }

  private shouldRefreshMatch(): boolean {
    const match = this.match();
    if (!match || !this.matchId) {
      return false;
    }
    if (match.status === 'LIVE') {
      return true;
    }
    if (match.status === 'SCHEDULED') {
      return Date.parse(match.kickoffAt) <= Date.now();
    }
    return false;
  }

  private applyMatch(match: Match): void {
    this.match.set({
      ...match,
      events: match.events ?? [],
      homeTries: match.homeTries ?? null,
      awayTries: match.awayTries ?? null,
      lineups: match.lineups ?? [],
    });
  }

  goBack(event: Event): void {
    event.preventDefault();
    this.navBack.prepareBack(this.router.url);
    void this.router.navigateByUrl(this.backPath());
  }

  selectSheetTab(tab: 'resume' | 'compositions'): void {
    this.sheetTab.set(tab);
  }

  private fallbackBackPath(match: Match | null): string {
    if (!match) {
      return '/fixtures';
    }
    if (match.status === 'LIVE') {
      return '/live';
    }
    if (match.status === 'FINISHED') {
      return `/results?competition=${match.competitionCode}`;
    }
    return `/fixtures?competition=${match.competitionCode}`;
  }

  hasLineups(match: Match): boolean {
    return (match.lineups ?? []).length > 0;
  }

  hasResume(match: Match): boolean {
    return (
      match.homeTries != null ||
      match.awayTries != null ||
      (match.events ?? []).length > 0
    );
  }

  absenceLabel(type: string): string {
    if (type === 'INJURED') {
      return 'Blessé';
    }
    if (type === 'SUSPENDED') {
      return 'Suspendu';
    }
    if (type === 'INTERNATIONAL') {
      return 'Sélection';
    }
    return type;
  }

  recordLabel(record: TeamForm | VenueRecord): string {
    return [
      this.countLabel(record.played, 'joué', 'joués'),
      this.countLabel(record.won, 'gagné', 'gagnés'),
      this.countLabel(record.drawn, 'nul', 'nuls'),
      this.countLabel(record.lost, 'perdu', 'perdus'),
    ].join(' · ');
  }

  private countLabel(value: number, singular: string, plural: string): string {
    return `${value} ${value <= 1 ? singular : plural}`;
  }

  formSeasonNote(form: TeamForm): string {
    if (!form.fromPreviousSeason || form.fromPreviousSeason <= 0) {
      return '';
    }
    if (form.fromPreviousSeason === 1) {
      return 'dont 1 saison dernière';
    }
    return `dont ${form.fromPreviousSeason} saison dernière`;
  }

  isMatchDay(match: Match): boolean {
    const kickoff = new Date(match.kickoffAt);
    const today = new Date();
    return (
      kickoff.getFullYear() === today.getFullYear() &&
      kickoff.getMonth() === today.getMonth() &&
      kickoff.getDate() === today.getDate()
    );
  }

  showAnalysisText(match: Match): boolean {
    return this.isMatchDay(match) && !!match.analysis;
  }

  eventTypeLabel(type: string): string {
    if (type === 'TRY') {
      return 'Essai';
    }
    if (type === 'CONVERSION') {
      return 'Transformation';
    }
    if (type === 'PENALTY') {
      return 'Pénalité';
    }
    if (type === 'DROP') {
      return 'Drop';
    }
    if (type === 'YELLOW') {
      return 'Carton jaune';
    }
    if (type === 'RED') {
      return 'Carton rouge';
    }
    return type;
  }

  isKick(type: string): boolean {
    return type === 'CONVERSION' || type === 'PENALTY' || type === 'DROP';
  }

  eventPoints(type: string): number {
    if (type === 'TRY') {
      return 5;
    }
    if (type === 'CONVERSION') {
      return 2;
    }
    if (type === 'PENALTY' || type === 'DROP') {
      return 3;
    }
    return 0;
  }

  pointsLabel(type: string): string {
    const points = this.eventPoints(type);
    return points > 0 ? `+${points}` : '';
  }

  topScorers(events: MatchEvent[]): MatchScorer[] {
    const byKey = new Map<string, MatchScorer>();
    for (const event of events) {
      const points = this.eventPoints(event.eventType);
      if (points <= 0 || !event.playerName) {
        continue;
      }
      const key = `${event.teamSide}|${event.playerName}`;
      const current = byKey.get(key) ?? {
        playerName: event.playerName,
        teamSide: event.teamSide,
        points: 0,
        tries: 0,
        kicks: 0,
      };
      current.points += points;
      if (event.eventType === 'TRY') {
        current.tries += 1;
      }
      if (this.isKick(event.eventType)) {
        current.kicks += 1;
      }
      byKey.set(key, current);
    }
    return [...byKey.values()].sort((a, b) => b.points - a.points).slice(0, 6);
  }

  timelineRows(events: MatchEvent[]): TimelineRow[] {
    const rows: TimelineRow[] = [];
    let homeScore = 0;
    let awayScore = 0;
    let previousPeriod = '';

    for (const event of events) {
      if (event.eventType === 'OTHER') {
        continue;
      }

      const period = event.periodLabel || '';
      if (
        previousPeriod &&
        period &&
        previousPeriod !== period &&
        /2nd|2e|second/i.test(period)
      ) {
        rows.push({ kind: 'halftime', homeScore, awayScore });
      }

      const points = this.eventPoints(event.eventType);
      if (event.teamSide === 'HOME') {
        homeScore += points;
      } else if (event.teamSide === 'AWAY') {
        awayScore += points;
      }

      rows.push({
        kind: 'event',
        event,
        homeScore,
        awayScore,
        indented: event.eventType === 'CONVERSION',
      });
      previousPeriod = period || previousPeriod;
    }
    return rows;
  }

  bench(side: string, match: Match): Match['lineups'] {
    return (match.lineups ?? [])
      .filter((row) => row.teamSide === side && !row.starter)
      .sort((a, b) => a.jerseyNumber - b.jerseyNumber);
  }

  pitchPlayer(side: string, jersey: number, match: Match): Match['lineups'][number] | null {
    return (
      (match.lineups ?? []).find(
        (row) => row.teamSide === side && row.jerseyNumber === jersey && row.starter,
      ) ?? null
    );
  }

  pitchStyle(jersey: number, side: string): Record<string, string> {
    const spot = PITCH_SPOTS[jersey] ?? { x: 50, y: 50 };
    const halfWidth = 43;
    const left =
      side === 'HOME' ? 5 + (spot.x / 100) * halfWidth : 95 - (spot.x / 100) * halfWidth;
    return {
      left: `${left}%`,
      top: `${10 + (spot.y / 100) * 82}%`,
    };
  }

  isFinished(status: string): boolean {
    return status === 'FINISHED';
  }

  isLiveOrFinished(status: string): boolean {
    return status === 'LIVE' || status === 'FINISHED';
  }
}
