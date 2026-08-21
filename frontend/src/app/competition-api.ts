import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Match, StandingRow, Team } from './models';

@Injectable({
  providedIn: 'root',
})
export class CompetitionApi {
  private readonly baseUrl = 'http://localhost:8080/api/competitions/TOP14';

  constructor(private http: HttpClient) {}

  getTeams(): Observable<Team[]> {
    return this.http.get<Team[]>(`${this.baseUrl}/teams`);
  }

  getMatches(status?: string): Observable<Match[]> {
    if (status) {
      return this.http.get<Match[]>(`${this.baseUrl}/matches`, {
        params: { status },
      });
    }
    return this.http.get<Match[]>(`${this.baseUrl}/matches`);
  }

  getStandings(): Observable<StandingRow[]> {
    return this.http.get<StandingRow[]>(`${this.baseUrl}/standings`);
  }
}
