import { Routes } from '@angular/router';
import { HomePage } from './home-page/home-page';
import { FixturesPage } from './fixtures-page/fixtures-page';
import { ResultsPage } from './results-page/results-page';
import { StandingsPage } from './standings-page/standings-page';
import { MatchPage } from './match-page/match-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', component: HomePage },
  { path: 'fixtures', component: FixturesPage },
  { path: 'results', component: ResultsPage },
  { path: 'standings', component: StandingsPage },
  { path: 'matches/:id', component: MatchPage },
];
