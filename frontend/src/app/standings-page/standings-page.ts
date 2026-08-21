import { Component, OnInit, signal } from '@angular/core';
import { CompetitionApi } from '../competition-api';
import { StandingRow } from '../models';

@Component({
  selector: 'app-standings-page',
  imports: [],
  templateUrl: './standings-page.html',
  styleUrl: './standings-page.css',
})
export class StandingsPage implements OnInit {
  rows = signal<StandingRow[]>([]);
  errorMessage = signal('');
  loading = signal(true);

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getStandings().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le classement.');
        this.loading.set(false);
      },
    });
  }
}
