import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { PlayerDetail } from '../models';
import { NavBack } from '../nav-back';

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
  photoBroken = signal(false);

  private readonly navBack = inject(NavBack);
  private readonly router = inject(Router);

  initials = computed(() => {
    const name = this.player()?.name?.trim() ?? '';
    if (!name) {
      return '?';
    }
    const parts = name.split(/\s+/).filter((part) => part.length > 0);
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    const first = parts[0][0] ?? '';
    const last = parts[parts.length - 1][0] ?? '';
    return (first + last).toUpperCase();
  });

  showsPhoto = computed(() => {
    const url = this.player()?.photoUrl?.trim();
    return Boolean(url) && !this.photoBroken();
  });

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

  showsJiff = computed(() => {
    const player = this.player();
    if (!player?.jiffStatus) {
      return false;
    }
    return player.competitionCode === 'TOP14' || player.competitionCode === 'PROD2';
  });

  jiffLabel = computed(() => {
    const status = this.player()?.jiffStatus;
    if (status === 'JIFF') {
      return 'JIFF';
    }
    if (status === 'NON_JIFF') {
      return 'NON-JIFF';
    }
    if (status === 'ESPOIR_NON_JIFF') {
      return 'Espoir non-JIFF';
    }
    return status ?? '';
  });

  seasonLabel = computed(() => {
    const season = this.player()?.season?.trim();
    return season ? `Saison ${season}` : 'Saison en cours';
  });

  backPath = computed(() => {
    this.navBack.revision();
    if (this.navBack.hasOrigin(this.router.url)) {
      return this.navBack.originFor(this.router.url).path;
    }
    const player = this.player();
    if (player) {
      return `/clubs/${player.competitionCode}/${player.team.shortName}`;
    }
    return '/';
  });

  backLabel = computed(() => {
    this.navBack.revision();
    if (this.navBack.hasOrigin(this.router.url)) {
      return this.navBack.originFor(this.router.url).label;
    }
    return this.player() ? '← Effectif' : '← Accueil';
  });

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
        this.photoBroken.set(false);
        this.player.set(player);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger ce joueur.');
        this.loading.set(false);
      },
    });
  }

  onPhotoError(): void {
    this.photoBroken.set(true);
  }

  goBack(event: Event): void {
    event.preventDefault();
    this.navBack.prepareBack(this.router.url);
    void this.router.navigateByUrl(this.backPath());
  }
}
