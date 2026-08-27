import { Component, OnInit, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CompetitionApi } from '../competition-api';
import { Competition, Team, Transfer } from '../models';
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
        this.loadJournal();
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les compétitions.');
        this.loading.set(false);
      },
    });
  }

  onCompetitionChange(code: string): void {
    this.selectedCode.set(code);
    if (this.tab() === 'clubs') {
      this.loading.set(true);
      this.errorMessage.set('');
      this.loadClubs(code);
    }
  }

  setTab(tab: TransfersTab): void {
    this.tab.set(tab);
    this.errorMessage.set('');
    this.loading.set(true);
    if (tab === 'journal') {
      this.loadJournal();
    } else {
      this.loadClubs(this.selectedCode());
    }
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

  private loadJournal(): void {
    this.api.getTransferJournal().subscribe({
      next: (transfers) => {
        this.journalTransfers.set(transfers);
        this.loading.set(false);
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
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les transferts du championnat.');
        this.loading.set(false);
      },
    });
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

  private buildClubBoards(teams: Team[], transfers: Transfer[]): ClubTransferBoard[] {
    return teams
      .map((team) => {
        const arrivals = transfers.filter(
          (t) =>
            t.toTeamId === team.id &&
            (t.type === 'JOIN' || t.type === 'LOAN'),
        );
        const departures = transfers.filter(
          (t) =>
            t.fromTeamId === team.id &&
            (t.type === 'LEAVE' || t.type === 'LOAN' || t.type === 'CONTRACT_END'),
        );
        const extensions = transfers.filter(
          (t) =>
            t.type === 'EXTENSION' &&
            (t.toTeamId === team.id || t.fromTeamId === team.id),
        );
        return { team, arrivals, departures, extensions };
      })
      .filter(
        (board) =>
          board.arrivals.length > 0 ||
          board.departures.length > 0 ||
          board.extensions.length > 0,
      );
  }
}
