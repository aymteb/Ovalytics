import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { PlayerDetail } from '../models';

@Component({
  selector: 'app-player-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './player-page.html',
  styleUrl: './player-page.css',
})
export class PlayerPage implements OnInit {
  player = signal<PlayerDetail | null>(null);
  errorMessage = signal('');
  loading = signal(true);

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
}
