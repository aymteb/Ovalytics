import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CompetitionApi } from '../competition-api';
import { Match } from '../models';

@Component({
  selector: 'app-home-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage implements OnInit {
  featured = signal<Match[]>([]);
  errorMessage = signal('');
  loading = signal(true);

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getMatches('SCHEDULED').subscribe({
      next: (matches) => {
        const next = [...matches]
          .sort((a, b) => a.kickoffAt.localeCompare(b.kickoffAt))
          .slice(0, 3);
        this.featured.set(next);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les prochaines affiches.');
        this.loading.set(false);
      },
    });
  }
}
