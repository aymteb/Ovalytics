export interface Team {
  id: number;
  name: string;
  shortName: string;
  city: string;
}

export interface Competition {
  id: number;
  name: string;
  code: string;
  season: string;
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

export interface Transfer {
  id: number;
  transferDate: string;
  playerName: string;
  playerId: number | null;
  type: string;
  fromClub: string;
  toClub: string;
  fromTeamId: number | null;
  toTeamId: number | null;
  contractLength: string | null;
  competitionCode: string;
  competitionName: string;
}

export interface ClubMercato {
  team: Team;
  competitionCode: string;
  competitionName: string;
  arrivals: Transfer[];
  departures: Transfer[];
  extensions: Transfer[];
  contractEndWatchYear: number;
  contractEndsNextYear: SquadPlayer[];
  squad: SquadPlayer[];
}

export interface SquadPlayer {
  id: number;
  name: string;
  position: string | null;
  age: number | null;
  heightCm: number | null;
  weightKg: number | null;
  nationality: string | null;
  contractEndDate: string | null;
}

export interface PlayerDetail {
  id: number;
  name: string;
  team: Team;
  competitionCode: string;
  competitionName: string;
  position: string | null;
  age: number | null;
  heightCm: number | null;
  weightKg: number | null;
  nationality: string | null;
  totals: PlayerTotals;
  appearances: PlayerAppearance[];
  transfers: Transfer[];
}

export interface PlayerTotals {
  matches: number;
  starts: number;
  minutes: number;
  tries: number;
  yellowCards: number;
  redCards: number;
}

export interface PlayerAppearance {
  matchId: number;
  kickoffAt: string;
  matchday: number;
  competitionCode: string;
  opponentShortName: string;
  venue: string;
  result: string;
  homeScore: number | null;
  awayScore: number | null;
  jerseyNumber: number;
  starter: boolean;
  minutesPlayed: number;
  tries: number;
  yellowCards: number;
  redCards: number;
}
