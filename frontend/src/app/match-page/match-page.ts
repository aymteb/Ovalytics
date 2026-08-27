import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { Match, TeamForm, VenueRecord } from '../models';

@Component({
  selector: 'app-match-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './match-page.html',
  styleUrl: './match-page.css',
})
export class MatchPage implements OnInit {
  match = signal<Match | null>(null);
  errorMessage = signal('');
  loading = signal(true);

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

    this.api.getMatch(id).subscribe({
      next: (match) => {
        this.match.set(match);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger ce match.');
        this.loading.set(false);
      },
    });
  }

  absenceLabel(type: string): string {
    if (type === 'INJURED') {
      return 'Blessé';
    }
    if (type === 'SUSPENDED') {
      return 'Suspendu';
    }
    return type;
  }

  recordLabel(record: TeamForm | VenueRecord): string {
    return `${record.played}J · ${record.won}V · ${record.drawn}N · ${record.lost}D`;
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
}
