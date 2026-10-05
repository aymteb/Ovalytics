const LEAGUE_CODES = new Set(['TOP14', 'PROD2', 'NAT', 'PREM', 'URC']);

const CLUB_COMPETITION_BY_SHORT: Record<string, string> = {
  TOU: 'TOP14',
  UBB: 'TOP14',
  RAC: 'TOP14',
  SFP: 'TOP14',
  TOL: 'TOP14',
  LAR: 'TOP14',
  ASM: 'TOP14',
  LOU: 'TOP14',
  MHR: 'TOP14',
  CAS: 'TOP14',
  PAU: 'TOP14',
  BAY: 'TOP14',
  USAP: 'TOP14',
  VAN: 'TOP14',
  BEZ: 'PROD2',
  OYO: 'PROD2',
  COL: 'PROD2',
  NEV: 'PROD2',
  AIX: 'PROD2',
  GRE: 'PROD2',
  BIA: 'PROD2',
  AGE: 'PROD2',
  BRI: 'PROD2',
  NIC: 'PROD2',
  ANG: 'PROD2',
  DAX: 'PROD2',
  NAR: 'PROD2',
  AUR: 'PROD2',
  MTB: 'PROD2',
  VAL: 'PROD2',
  MAS: 'NAT',
  CAR: 'NAT',
  ALB: 'NAT',
  MDM: 'NAT',
  CHA: 'NAT',
  ROU: 'NAT',
  SUR: 'NAT',
  BOU: 'NAT',
  ORL: 'NAT',
  PER: 'NAT',
  REN: 'NAT',
  VIE: 'NAT',
  USB: 'NAT',
  MAR: 'NAT',
  NOR: 'PREM',
  GLO: 'PREM',
  BAT: 'PREM',
  BRS: 'PREM',
  SAR: 'PREM',
  LEIC: 'PREM',
  EXE: 'PREM',
  NEW: 'PREM',
  SAL: 'PREM',
  HAR: 'PREM',
  BUL: 'URC',
  SHA: 'URC',
  STO: 'URC',
  GLA: 'URC',
  EDI: 'URC',
  CDF: 'URC',
  LIO: 'URC',
  DRA: 'URC',
  TRE: 'URC',
  LEI: 'URC',
  ULS: 'URC',
  MUN: 'URC',
  SCA: 'URC',
  OSP: 'URC',
  CON: 'URC',
  ZEB: 'URC',
};

export function competitionSortRank(code: string): number {
  switch (code) {
    case 'TOP14':
      return 0;
    case 'PROD2':
      return 1;
    case 'NAT':
      return 2;
    case 'ERCC':
      return 3;
    case 'ERCH':
      return 4;
    case 'URC':
      return 5;
    case 'PREM':
      return 6;
    case 'INT':
      return 7;
    case 'SEVENS':
      return 8;
    default:
      return 10;
  }
}

export function isLeagueCompetition(code: string | null | undefined): boolean {
  return !!code && LEAGUE_CODES.has(code);
}

export function hasStandingsCompetition(code: string | null | undefined): boolean {
  return isLeagueCompetition(code) || code === 'ERCC' || code === 'ERCH';
}

export function resolveClubCompetition(
  competitionCode: string | null | undefined,
  shortName: string | null | undefined,
): string | null {
  if (!shortName) {
    return null;
  }
  const mapped = CLUB_COMPETITION_BY_SHORT[shortName];
  if (mapped) {
    return mapped;
  }
  if (isLeagueCompetition(competitionCode)) {
    return competitionCode ?? null;
  }
  return null;
}

export function clubRoute(
  competitionCode: string | null | undefined,
  shortName: string | null | undefined,
): string[] | null {
  const code = resolveClubCompetition(competitionCode, shortName);
  if (!code || !shortName) {
    return null;
  }
  return ['/clubs', code, shortName];
}

export function competitionLabel(code: string | null): string {
  if (code === 'TOP14') {
    return 'Top 14';
  }
  if (code === 'PROD2') {
    return 'Pro D2';
  }
  if (code === 'NAT') {
    return 'Nationale';
  }
  if (code === 'ERCC') {
    return 'Champions Cup';
  }
  if (code === 'ERCH') {
    return 'Challenge Cup';
  }
  if (code === 'URC') {
    return 'URC';
  }
  if (code === 'PREM') {
    return 'Premiership';
  }
  if (code === 'INT') {
    return 'Tests';
  }
  if (code === 'SEVENS') {
    return 'Sevens';
  }
  return '';
}

export function competitionBadgeClass(code: string | null): string {
  if (code === 'TOP14') {
    return 'bg-primary text-on-primary';
  }
  if (code === 'PROD2') {
    return 'bg-sky-700 text-white';
  }
  if (code === 'NAT') {
    return 'bg-emerald-800 text-white';
  }
  if (code === 'ERCC') {
    return 'bg-indigo-900 text-white';
  }
  if (code === 'ERCH') {
    return 'bg-indigo-700 text-white';
  }
  if (code === 'URC') {
    return 'bg-violet-800 text-white';
  }
  if (code === 'PREM') {
    return 'bg-rose-900 text-white';
  }
  if (code === 'INT') {
    return 'bg-amber-800 text-white';
  }
  return 'bg-muted text-foreground';
}
