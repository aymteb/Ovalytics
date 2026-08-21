import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { Match } from '../models';

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
}
