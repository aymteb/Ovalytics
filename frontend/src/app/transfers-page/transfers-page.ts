import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, ViewportScroller } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Competition, Team, Transfer } from '../models';
import { resolveClubShort } from '../team-branding';
import { competitionSortRank } from '../competition-display';
import { NavBack } from '../nav-back';
import { TeamLogo } from '../team-logo/team-logo';

type TransfersTab = 'journal' | 'clubs';

interface ClubTransferBoard {
  team: Team;
  arrivals: Transfer[];
  departures: Transfer[];
  extensions: Transfer[];
}

@Component({
  selector: 'app-transfers-page',
  imports: [DatePipe, RouterLink, TeamLogo],
  templateUrl: './transfers-page.html',
  styleUrl: './transfers-page.css',
})
export class TransfersPage implements OnInit {
  competitions = signal<Competition[]>([]);
  selectedCode = signal('TOP14');
  journalTransfers = signal<Transfer[]>([]);
  clubTransfers = signal<Transfer[]>([]);
  teams = signal<Team[]>([]);
  tab = signal<TransfersTab>('journal');
  errorMessage = signal('');
  loading = signal(true);

  clubBoards = computed(() => this.buildClubBoards(this.teams(), this.clubTransfers()));
  showDurationColumn = computed(() =>
    this.journalTransfers().some((transfer) => !!transfer.contractLength?.trim()),
  );

  private readonly destroyRef = inject(DestroyRef);
  private readonly navBack = inject(NavBack);
  private readonly viewport = inject(ViewportScroller);
  private booted = false;

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
        this.route.queryParamMap
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((params) => {
            const tab = params.get('tab') === 'clubs' ? 'clubs' : 'journal';
            const fromQuery = params.get('competition');
            const preferred =
              ordered.find((c) => c.code === fromQuery) ??
              ordered.find((c) => c.code === 'TOP14') ??
              ordered[0];
            const code = preferred?.code ?? 'TOP14';
            const tabChanged = this.tab() !== tab;
            const codeChanged = this.selectedCode() !== code;
            this.tab.set(tab);
            this.selectedCode.set(code);
            if (!this.booted || tabChanged || (tab === 'clubs' && codeChanged)) {
              this.booted = true;
              this.errorMessage.set('');
              this.loading.set(true);
              if (tab === 'journal') {
                this.loadJournal();
              } else {
                this.loadClubs(code);
              }
            }
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
    this.syncQuery('clubs', code);
  }

  setTab(tab: TransfersTab): void {
    this.syncQuery(tab, this.selectedCode());
  }

  typeLabel(type: string): string {
    switch (type) {
      case 'JOIN':
        return 'Arrivée';
      case 'LEAVE':
        return 'Départ';
      case 'LOAN':
        return 'Prêt';
      case 'EXTENSION':
        return 'Prolongation';
      case 'CONTRACT_END':
        return 'Fin de contrat';
      default:
        return type;
    }
  }

  clubLink(team: Team): string[] {
    return ['/clubs', this.selectedCode(), team.shortName];
  }

  playerLink(transfer: Transfer): string[] | null {
    return transfer.playerId ? ['/players', String(transfer.playerId)] : null;
  }

  clubShort(transfer: Transfer, side: 'from' | 'to'): string | null {
    const label = side === 'from' ? transfer.fromClub : transfer.toClub;
    return resolveClubShort(label);
  }

  clubFallback(transfer: Transfer, side: 'from' | 'to'): string {
    const label = side === 'from' ? transfer.fromClub : transfer.toClub;
    return label?.trim() || '—';
  }

  private syncQuery(tab: TransfersTab, competition: string): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams:
        tab === 'clubs'
          ? { tab: 'clubs', competition }
          : { tab: 'journal', competition: null },
      replaceUrl: true,
    });
  }

  private loadJournal(): void {
    this.api.getTransferJournal().subscribe({
      next: (transfers) => {
        this.journalTransfers.set(transfers);
        this.loading.set(false);
        this.restoreScroll();
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le journal des transferts.');
        this.loading.set(false);
      },
    });
  }

  private loadClubs(code: string): void {
    forkJoin({
      transfers: this.api.getTransfers(code),
      teams: this.api.getTeams(code),
    }).subscribe({
      next: ({ transfers, teams }) => {
        this.clubTransfers.set(transfers);
        this.teams.set(teams);
        this.loading.set(false);
        this.restoreScroll();
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les transferts du championnat.');
        this.loading.set(false);
      },
    });
  }

  private restoreScroll(): void {
    const y = this.navBack.consumeRestoreScroll('/transfers');
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

  private orderCompetitions(competitions: Competition[]): Competition[] {
    return [...competitions].sort(
      (a, b) =>
        competitionSortRank(a.code) - competitionSortRank(b.code) ||
        a.name.localeCompare(b.name, 'fr'),
    );
  }

  private buildClubBoards(teams: Team[], transfers: Transfer[]): ClubTransferBoard[] {
    return teams.map((team) => {
      const arrivals = transfers.filter(
        (t) =>
          t.toTeamId === team.id && (t.type === 'JOIN' || t.type === 'LOAN'),
      );
      const departures = transfers.filter(
        (t) =>
          t.fromTeamId === team.id && (t.type === 'LEAVE' || t.type === 'LOAN'),
      );
      const extensions = transfers.filter(
        (t) =>
          t.type === 'EXTENSION' &&
          (t.toTeamId === team.id || t.fromTeamId === team.id),
      );
      return { team, arrivals, departures, extensions };
    });
  }
}
