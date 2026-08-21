import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { Match } from '../models';

@Component({
  selector: 'app-results-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './results-page.html',
  styleUrl: './results-page.css',
})
export class ResultsPage implements OnInit {
  matches = signal<Match[]>([]);
  errorMessage = signal('');
  loading = signal(true);

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getMatches('FINISHED').subscribe({
      next: (matches) => {
        this.matches.set(matches);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les résultats.');
        this.loading.set(false);
      },
    });
  }
}
