import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CompetitionApi } from '../competition-api';
import { ClubMercato, SquadPlayer, Transfer } from '../models';
import { resolveClubCompetition } from '../competition-display';
import { NavBack } from '../nav-back';
import { TeamLogo } from '../team-logo/team-logo';

interface SquadGroup {
  position: string;
  players: SquadPlayer[];
}

const POSITION_ORDER = [
  'Pilier',
  'Talonneur',
  '2ème ligne',
  '3ème ligne',
  'Mêlée',
  'Ouverture',
  'Centre',
  'Ailier',
  'Arrière',
];

const POSITION_ALIASES: Record<string, string> = {
  pilier: 'Pilier',
  talonneur: 'Talonneur',
  '2ème ligne': '2ème ligne',
  '2eme ligne': '2ème ligne',
  'deuxième ligne': '2ème ligne',
  'deuxieme ligne': '2ème ligne',
  '3ème ligne': '3ème ligne',
  '3eme ligne': '3ème ligne',
  'troisième ligne': '3ème ligne',
  'troisieme ligne': '3ème ligne',
  mêlée: 'Mêlée',
  melee: 'Mêlée',
  'demi de mêlée': 'Mêlée',
  'demi de melee': 'Mêlée',
  ouverture: 'Ouverture',
  "demi d'ouverture": 'Ouverture',
  centre: 'Centre',
  ailier: 'Ailier',
  arrière: 'Arrière',
  arriere: 'Arrière',
};

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

  private readonly navBack = inject(NavBack);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  backPath = computed(() => {
    this.navBack.revision();
    return this.navBack.originFor(this.router.url).path;
  });

  backLabel = computed(() => {
    this.navBack.revision();
    return this.navBack.originFor(this.router.url).label;
  });

  squadGroups = computed(() => this.buildSquadGroups(this.mercato()?.squad ?? []));

  showsJiff = computed(() => this.mercato()?.jiffSummary != null);

  squadHint = computed(() =>
    this.showsJiff()
      ? 'Effectif projeté. Couleurs = statut JIFF. Tri par âge dans chaque poste.'
      : 'Effectif projeté. Tri par âge dans chaque poste.',
  );

  constructor(
    private route: ActivatedRoute,
    private api: CompetitionApi,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const code = params.get('code');
      const shortName = params.get('shortName');
      if (!code || !shortName) {
        this.errorMessage.set('Club introuvable.');
        this.loading.set(false);
        return;
      }

      const clubCode = resolveClubCompetition(code, shortName);
      if (!clubCode) {
        this.mercato.set(null);
        this.errorMessage.set('Pas de fiche club pour cette équipe.');
        this.loading.set(false);
        return;
      }
      if (clubCode !== code) {
        void this.router.navigate(['/clubs', clubCode, shortName], { replaceUrl: true });
        return;
      }

      this.loading.set(true);
      this.errorMessage.set('');
      this.api.getClubMercato(clubCode, shortName).subscribe({
        next: (mercato) => {
          this.mercato.set(mercato);
          this.loading.set(false);
        },
        error: () => {
          this.mercato.set(null);
          this.errorMessage.set('Impossible de charger ce club.');
          this.loading.set(false);
        },
      });
    });
  }

  goBack(event: Event): void {
    event.preventDefault();
    this.navBack.prepareBack(this.router.url);
    void this.router.navigateByUrl(this.backPath());
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

  playerStatusClass(player: SquadPlayer): string {
    if (!this.showsJiff()) {
      return 'plain';
    }
    if (player.jiffStatus === 'ESPOIR_NON_JIFF') {
      return 'espoir-non-jiff';
    }
    if (player.contractType === 'ESPOIR') {
      return 'espoir';
    }
    if (player.jiffStatus === 'NON_JIFF') {
      return 'non-jiff';
    }
    if (player.jiffStatus === 'JIFF') {
      return 'jiff';
    }
    return 'plain';
  }

  private buildSquadGroups(squad: SquadPlayer[]): SquadGroup[] {
    const byPosition = new Map<string, SquadPlayer[]>();

    for (const player of squad) {
      const position = this.normalizePosition(player.position) || 'Sans poste';
      const list = byPosition.get(position) ?? [];
      list.push(player);
      byPosition.set(position, list);
    }

    const groups: SquadGroup[] = [];
    for (const position of POSITION_ORDER) {
      const players = byPosition.get(position);
      if (players && players.length > 0) {
        groups.push({ position, players: this.sortByAge(players) });
        byPosition.delete(position);
      }
    }
    for (const [position, players] of byPosition) {
      groups.push({ position, players: this.sortByAge(players) });
    }
    return groups;
  }

  private sortByAge(players: SquadPlayer[]): SquadPlayer[] {
    return [...players].sort((a, b) => {
      const ageA = a.age ?? 0;
      const ageB = b.age ?? 0;
      if (ageA !== ageB) {
        return ageB - ageA;
      }
      const espoirA = a.contractType === 'ESPOIR' ? 1 : 0;
      const espoirB = b.contractType === 'ESPOIR' ? 1 : 0;
      if (espoirA !== espoirB) {
        return espoirA - espoirB;
      }
      return a.name.localeCompare(b.name, 'fr');
    });
  }

  private normalizePosition(position: string | null): string {
    if (!position?.trim()) {
      return '';
    }
    const key = position.trim().toLowerCase();
    return POSITION_ALIASES[key] ?? position.trim();
  }
}
