import { Routes } from '@angular/router';
import { FixturesPage } from './fixtures-page/fixtures-page';
import { ResultsPage } from './results-page/results-page';
import { StandingsPage } from './standings-page/standings-page';
import { MatchPage } from './match-page/match-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'fixtures' },
  { path: 'fixtures', component: FixturesPage },
  { path: 'results', component: ResultsPage },
  { path: 'standings', component: StandingsPage },
  { path: 'matches/:id', component: MatchPage },
];
