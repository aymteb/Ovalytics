import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Competition, ClubMercato, Match, PlayerDetail, StandingRow, Team, Transfer } from './models';

@Injectable({
  providedIn: 'root',
})
export class CompetitionApi {
  private readonly apiBase = '/api/competitions';

  constructor(private http: HttpClient) {}

  getCompetitions(): Observable<Competition[]> {
    return this.http.get<Competition[]>(this.apiBase);
  }

  getTeams(code = 'TOP14'): Observable<Team[]> {
    return this.http.get<Team[]>(`${this.apiBase}/${code}/teams`);
  }

  getMatches(status?: string, code = 'TOP14'): Observable<Match[]> {
    if (status) {
      return this.http.get<Match[]>(`${this.apiBase}/${code}/matches`, {
        params: { status },
      });
    }
    return this.http.get<Match[]>(`${this.apiBase}/${code}/matches`);
  }

  getMatch(id: number, code = 'TOP14'): Observable<Match> {
    return this.http.get<Match>(`${this.apiBase}/${code}/matches/${id}`);
  }

  getStandings(code = 'TOP14'): Observable<StandingRow[]> {
    return this.http.get<StandingRow[]>(`${this.apiBase}/${code}/standings`);
  }

  getTransfers(code = 'TOP14'): Observable<Transfer[]> {
    return this.http.get<Transfer[]>(`${this.apiBase}/${code}/transfers`);
  }

  getTransferJournal(): Observable<Transfer[]> {
    return this.http.get<Transfer[]>('/api/transfers');
  }

  getClubMercato(code: string, shortName: string): Observable<ClubMercato> {
    return this.http.get<ClubMercato>(
      `${this.apiBase}/${code}/teams/${shortName}/mercato`,
    );
  }

  getPlayer(id: number): Observable<PlayerDetail> {
    return this.http.get<PlayerDetail>(`/api/players/${id}`);
  }
}
