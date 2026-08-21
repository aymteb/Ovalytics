import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { CompetitionApi } from '../competition-api';
import { Match } from '../models';

@Component({
  selector: 'app-results-page',
  imports: [DatePipe],
  templateUrl: './results-page.html',
  styleUrl: './results-page.css',
})
export class ResultsPage implements OnInit {
  matches: Match[] = [];
  errorMessage = '';
  loading = true;

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getMatches('FINISHED').subscribe({
      next: (matches) => {
        this.matches = matches;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les résultats.';
        this.loading = false;
      },
    });
  }
}
