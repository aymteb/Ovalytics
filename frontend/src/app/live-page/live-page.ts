import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, interval, startWith, switchMap } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Match } from '../models';
import { competitionLabel, competitionSortRank } from '../competition-display';
import { TeamLogo } from '../team-logo/team-logo';

interface LiveGroup {
  competitionCode: string;
  competitionName: string;
  matches: Match[];
}

@Component({
  selector: 'app-live-page',
  imports: [DatePipe, RouterLink, TeamLogo],
  templateUrl: './live-page.html',
  styleUrl: './live-page.css',
})
export class LivePage implements OnInit {
  matches = signal<Match[]>([]);
  upcoming = signal<Match[]>([]);
  loading = signal(true);
  errorMessage = signal('');
  now = signal(Date.now());

  private readonly destroyRef = inject(DestroyRef);

  groups = computed(() => this.buildGroups(this.matches()));
  hasLive = computed(() => this.matches().length > 0);

  nextSlot = computed(() => {
    const list = this.upcoming();
    if (list.length === 0) {
      return [] as Match[];
    }
    const anchor = Date.parse(list[0].kickoffAt);
    if (Number.isNaN(anchor)) {
      return list.slice(0, 1);
    }
    const windowMs = 3 * 60 * 60 * 1000;
    return list
      .filter((match) => {
        const kickoff = Date.parse(match.kickoffAt);
        return !Number.isNaN(kickoff) && kickoff - anchor <= windowMs;
      })
      .slice(0, 4);
  });

  featuredMatch = computed(() => this.nextSlot()[0] ?? null);

  slotOthers = computed(() => this.nextSlot().slice(1));

  countdownLabel = computed(() => {
    const match = this.featuredMatch();
    if (!match) {
      return '';
    }
    const kickoff = Date.parse(match.kickoffAt);
    if (Number.isNaN(kickoff)) {
      return '';
    }
    const diff = kickoff - this.now();
    if (diff <= 0) {
      return 'Coup d’envoi imminent';
    }
    const totalMinutes = Math.floor(diff / 60_000);
    const days = Math.floor(totalMinutes / (60 * 24));
    const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
    const minutes = totalMinutes % 60;
    if (days > 0) {
      return `Dans ${days}j ${String(hours).padStart(2, '0')}h`;
    }
    if (hours > 0) {
      return `Dans ${String(hours).padStart(2, '0')}h ${String(minutes).padStart(2, '0')}min`;
    }
    return `Dans ${minutes} min`;
  });

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    interval(1_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.now.set(Date.now()));

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
          this.matches.set(
            [...live].sort((a, b) => {
              const byComp =
                competitionSortRank(a.competitionCode) -
                competitionSortRank(b.competitionCode);
              if (byComp !== 0) {
                return byComp;
              }
              return a.kickoffAt.localeCompare(b.kickoffAt);
            }),
          );
          this.upcoming.set(
            scheduled
              .filter((match) => this.isUpcoming(match))
              .sort((a, b) => a.kickoffAt.localeCompare(b.kickoffAt)),
          );
          this.loading.set(false);
          this.errorMessage.set('');
        },
        error: () => {
          this.errorMessage.set('Impossible de charger les matchs en direct.');
          this.loading.set(false);
        },
      });
  }

  readonly competitionLabel = competitionLabel;

  private isUpcoming(match: Match): boolean {
    const kickoff = Date.parse(match.kickoffAt);
    if (Number.isNaN(kickoff)) {
      return false;
    }
    return kickoff > Date.now();
  }

  private buildGroups(matches: Match[]): LiveGroup[] {
    const byCode = new Map<string, LiveGroup>();
    for (const match of matches) {
      let group = byCode.get(match.competitionCode);
      if (!group) {
        group = {
          competitionCode: match.competitionCode,
          competitionName: match.competitionName,
          matches: [],
        };
        byCode.set(match.competitionCode, group);
      }
      group.matches.push(match);
    }
    return [...byCode.values()].sort(
      (a, b) =>
        competitionSortRank(a.competitionCode) -
        competitionSortRank(b.competitionCode),
    );
  }
}
