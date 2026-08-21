import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { CompetitionApi } from '../competition-api';
import { Match } from '../models';

@Component({
  selector: 'app-fixtures-page',
  imports: [DatePipe],
  templateUrl: './fixtures-page.html',
  styleUrl: './fixtures-page.css',
})
export class FixturesPage implements OnInit {
  matches: Match[] = [];
  errorMessage = '';
  loading = true;

  constructor(private api: CompetitionApi) {}

  ngOnInit(): void {
    this.api.getMatches('SCHEDULED').subscribe({
      next: (matches) => {
        this.matches = matches;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les matchs à venir.';
        this.loading = false;
      },
    });
  }
}
