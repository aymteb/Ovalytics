export interface Team {
  id: number;
  name: string;
  shortName: string;
  city: string;
}

export interface Absence {
  playerName: string;
  type: string;
  note: string | null;
}

export interface TeamForm {
  results: string[];
  played: number;
  won: number;
  drawn: number;
  lost: number;
  fromPreviousSeason: number;
}

export interface VenueRecord {
  played: number;
  won: number;
  drawn: number;
  lost: number;
}

export interface HeadToHeadMatch {
  id: number;
  kickoffAt: string;
  homeShortName: string;
  awayShortName: string;
  homeScore: number;
  awayScore: number;
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
  homeAbsences: Absence[];
  awayAbsences: Absence[];
  homeForm: TeamForm | null;
  awayForm: TeamForm | null;
  homeHomeRecord: VenueRecord | null;
  awayAwayRecord: VenueRecord | null;
  headToHead: HeadToHeadMatch[];
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
