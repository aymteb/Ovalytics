export interface TeamBrand {
  bg: string;
  text: string;
}

const DEFAULT_BRAND: TeamBrand = { bg: '#374151', text: '#FFFFFF' };

const TEAM_BRANDS: Record<string, TeamBrand> = {
  TOU: { bg: '#E30613', text: '#FFFFFF' },
  RAC: { bg: '#001489', text: '#FFFFFF' },
  SFP: { bg: '#E40046', text: '#FFFFFF' },
  TOL: { bg: '#1A1A1A', text: '#FFD700' },
  LAR: { bg: '#FFD700', text: '#1A1A1A' },
  UBB: { bg: '#002B5C', text: '#FFFFFF' },
  ASM: { bg: '#FFD700', text: '#1A1A1A' },
  LOU: { bg: '#C8102E', text: '#FFFFFF' },
  MHR: { bg: '#003366', text: '#FFFFFF' },
  CAS: { bg: '#0038A8', text: '#FFFFFF' },
  PAU: { bg: '#006633', text: '#FFFFFF' },
  BAY: { bg: '#228B22', text: '#FFFFFF' },
  USAP: { bg: '#FFD700', text: '#8B0000' },
  VAN: { bg: '#FF6600', text: '#FFFFFF' },
  BEZ: { bg: '#003DA5', text: '#FFFFFF' },
  OYO: { bg: '#1A1A1A', text: '#FFFFFF' },
  COL: { bg: '#0054A6', text: '#FFFFFF' },
  NEV: { bg: '#006633', text: '#FFFFFF' },
  AIX: { bg: '#E30613', text: '#FFFFFF' },
  CHA: { bg: '#2D5016', text: '#FFFFFF' },
};

export function getTeamBrand(shortName: string): TeamBrand {
  return TEAM_BRANDS[shortName] ?? DEFAULT_BRAND;
}

export function teamLogoUrl(shortName: string): string {
  return `/clubs/${shortName}.svg`;
}
