export interface Team {
  id: number;
  name: string;
  shortName: string;
  city: string;
}

export interface Match {
  id: number;
  matchday: number;
  kickoffAt: string;
  status: string;
  homeTeam: Team;
  awayTeam: Team;
  homeScore: number | null;
  awayScore: number | null;
  analysis: string | null;
}

export interface StandingRow {
  position: number;
  teamId: number;
  teamName: string;
  teamShortName: string;
  played: number;
  won: number;
  drawn: number;
  lost: number;
  pointsFor: number;
  pointsAgainst: number;
  pointsDifference: number;
  bonus: number;
  points: number;
}
