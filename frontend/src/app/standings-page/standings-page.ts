import { Component, OnInit } from '@angular/core';
import { CompetitionApi } from '../competition-api';
import { StandingRow } from '../models';

@Component({
  selector: 'app-standings-page',
  imports: [],
  templateUrl: './standings-page.html',
  styleUrl: './standings-page.css',
})
export class StandingsPage implements OnInit {
  rows: StandingRow[] = [];
  errorMessage = '';
  loading = true;

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getStandings().subscribe({
      next: (rows) => {
        this.rows = rows;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le classement.';
        this.loading = false;
      },
    });
  }
}
