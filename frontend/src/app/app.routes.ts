import { Routes } from '@angular/router';
import { HomePage } from './home-page/home-page';
import { FixturesPage } from './fixtures-page/fixtures-page';
import { ResultsPage } from './results-page/results-page';
import { StandingsPage } from './standings-page/standings-page';
import { TransfersPage } from './transfers-page/transfers-page';
import { MatchPage } from './match-page/match-page';
import { ClubPage } from './club-page/club-page';
import { PlayerPage } from './player-page/player-page';
import { NewsPage } from './news-page/news-page';
import { NewsDetailPage } from './news-detail-page/news-detail-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', component: HomePage },
  { path: 'fixtures', component: FixturesPage },
  { path: 'results', component: ResultsPage },
  { path: 'standings', component: StandingsPage },
  { path: 'transfers', component: TransfersPage },
  { path: 'news', component: NewsPage },
  { path: 'news/:id', component: NewsDetailPage },
  { path: 'matches/:id', component: MatchPage },
  { path: 'clubs/:code/:shortName', component: ClubPage },
  { path: 'players/:id', component: PlayerPage },
];
