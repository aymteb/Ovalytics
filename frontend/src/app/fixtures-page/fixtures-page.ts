import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { Match } from '../models';

@Component({
  selector: 'app-fixtures-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './fixtures-page.html',
  styleUrl: './fixtures-page.css',
})
export class FixturesPage implements OnInit {
  matches = signal<Match[]>([]);
  errorMessage = signal('');
  loading = signal(true);

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getMatches('SCHEDULED').subscribe({
      next: (matches) => {
        this.matches.set(matches);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les matchs à venir.');
        this.loading.set(false);
      },
    });
  }
}
