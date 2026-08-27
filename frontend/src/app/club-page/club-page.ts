import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { ClubMercato, SquadPlayer, Transfer } from '../models';
import { TeamLogo } from '../team-logo/team-logo';

interface SquadGroup {
  position: string;
  players: SquadPlayer[];
}

const POSITION_ORDER = [
  'Pilier',
  'Talonneur',
  'Deuxième ligne',
  'Troisième ligne',
  'Demi de mêlée',
  "Demi d'ouverture",
  'Centre',
  'Ailier',
  'Arrière',
];

@Component({
  selector: 'app-club-page',
  imports: [RouterLink, TeamLogo],
  templateUrl: './club-page.html',
  styleUrl: './club-page.css',
})
export class ClubPage implements OnInit {
  mercato = signal<ClubMercato | null>(null);
  errorMessage = signal('');
  loading = signal(true);

  squadGroups = computed(() => this.buildSquadGroups(this.mercato()?.squad ?? []));

  constructor(
    private route: ActivatedRoute,
    private api: CompetitionApi,
  ) {}

  ngOnInit(): void {
    const code = this.route.snapshot.paramMap.get('code');
    const shortName = this.route.snapshot.paramMap.get('shortName');
    if (!code || !shortName) {
      this.errorMessage.set('Club introuvable.');
      this.loading.set(false);
      return;
    }

    this.api.getClubMercato(code, shortName).subscribe({
      next: (mercato) => {
        this.mercato.set(mercato);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger ce club.');
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

  playerLink(transfer: Transfer): string[] | null {
    return transfer.playerId ? ['/players', String(transfer.playerId)] : null;
  }

  contractLabel(contractEndDate: string): string {
    const year = contractEndDate.slice(0, 4);
    return `jusqu'en ${year}`;
  }

  private buildSquadGroups(squad: SquadPlayer[]): SquadGroup[] {
    const byPosition = new Map<string, SquadPlayer[]>();
    for (const player of squad) {
      const position = player.position?.trim() || 'Autre';
      const list = byPosition.get(position) ?? [];
      list.push(player);
      byPosition.set(position, list);
    }

    const groups: SquadGroup[] = [];
    for (const position of POSITION_ORDER) {
      const players = byPosition.get(position);
      if (players && players.length > 0) {
        groups.push({ position, players });
        byPosition.delete(position);
      }
    }
    for (const [position, players] of byPosition) {
      groups.push({ position, players });
    }
    return groups;
  }
}
