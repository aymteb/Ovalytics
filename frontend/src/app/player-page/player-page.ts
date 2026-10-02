import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { PlayerDetail, Transfer } from '../models';
import { PlayerNav } from '../player-nav';
import { resolveClubShort } from '../team-branding';
import { TeamLogo } from '../team-logo/team-logo';

@Component({
  selector: 'app-player-page',
  imports: [DatePipe, RouterLink, TeamLogo],
  templateUrl: './player-page.html',
  styleUrl: './player-page.css',
})
export class PlayerPage implements OnInit {
  player = signal<PlayerDetail | null>(null);
  errorMessage = signal('');
  loading = signal(true);

  private readonly playerNav = inject(PlayerNav);

  careerEntries = computed(() => {
    const history = this.player()?.careerHistory;
    if (!history) {
      return [];
    }
    return history
      .split('|')
      .map((entry) => entry.trim())
      .filter((entry) => entry.length > 0);
  });

  showTransferDuration = computed(() =>
    (this.player()?.transfers ?? []).some((transfer) => !!transfer.contractLength?.trim()),
  );

  backPath = computed(() => {
    if (this.playerNav.backTo === 'transfers') {
      return '/transfers';
    }
    if (this.playerNav.clubCode && this.playerNav.clubShortName) {
      return [
        '/clubs',
        this.playerNav.clubCode,
        this.playerNav.clubShortName,
      ];
    }
    const player = this.player();
    if (player) {
      return ['/clubs', player.competitionCode, player.team.shortName];
    }
    return '/transfers';
  });

  backLabel = computed(() =>
    this.playerNav.backTo === 'transfers' ? '← Transferts' : '← Effectif',
  );

  constructor(
    private route: ActivatedRoute,
    private api: CompetitionApi,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.errorMessage.set('Joueur introuvable.');
      this.loading.set(false);
      return;
    }

    this.api.getPlayer(id).subscribe({
      next: (player) => {
        this.player.set(player);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger ce joueur.');
        this.loading.set(false);
      },
    });
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

  clubShort(transfer: Transfer, side: 'from' | 'to'): string | null {
    const label = side === 'from' ? transfer.fromClub : transfer.toClub;
    return resolveClubShort(label);
  }

  clubFallback(transfer: Transfer, side: 'from' | 'to'): string {
    const label = side === 'from' ? transfer.fromClub : transfer.toClub;
    return label?.trim() || '—';
  }
}
