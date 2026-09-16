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
  BIA: { bg: '#003DA5', text: '#FFFFFF' },
  NIC: { bg: '#E30613', text: '#FFFFFF' },
  ANG: { bg: '#1A1A1A', text: '#FFFFFF' },
  DAX: { bg: '#006633', text: '#FFFFFF' },
  NAR: { bg: '#FF6600', text: '#FFFFFF' },
  GRE: { bg: '#0054A6', text: '#FFFFFF' },
  AUR: { bg: '#8B0000', text: '#FFFFFF' },
  MTB: { bg: '#FFD700', text: '#1A1A1A' },
  AGE: { bg: '#003366', text: '#FFFFFF' },
  BRI: { bg: '#FFD700', text: '#1A1A1A' },
  VAL: { bg: '#C8102E', text: '#FFFFFF' },
};

export function getTeamBrand(shortName: string): TeamBrand {
  return TEAM_BRANDS[shortName] ?? DEFAULT_BRAND;
}

export function teamLogoUrl(shortName: string): string {
  return `/clubs/${shortName}.png`;
}

const CLUB_LABEL_TO_SHORT: Record<string, string> = {
  toulouse: 'TOU',
  'stade toulousain': 'TOU',
  bordeaux: 'UBB',
  'union bordeaux': 'UBB',
  'union bordeaux begles': 'UBB',
  'racing 92': 'RAC',
  racing: 'RAC',
  'stade francais': 'SFP',
  'stade français': 'SFP',
  paris: 'SFP',
  toulon: 'TOL',
  'rc toulon': 'TOL',
  'la rochelle': 'LAR',
  rochelle: 'LAR',
  clermont: 'ASM',
  asm: 'ASM',
  lyon: 'LOU',
  lou: 'LOU',
  montpellier: 'MHR',
  castres: 'CAS',
  pau: 'PAU',
  bayonne: 'BAY',
  aviron: 'BAY',
  perpignan: 'USAP',
  usap: 'USAP',
  vannes: 'VAN',
  beziers: 'BEZ',
  béziers: 'BEZ',
  oyonnax: 'OYO',
  colomiers: 'COL',
  nevers: 'NEV',
  provence: 'AIX',
  aix: 'AIX',
  grenoble: 'GRE',
  biarritz: 'BIA',
  agen: 'AGE',
  brive: 'BRI',
  nice: 'NIC',
  nicois: 'NIC',
  niçois: 'NIC',
  angouleme: 'ANG',
  angoulême: 'ANG',
  soyaux: 'ANG',
  dax: 'DAX',
  narbonne: 'NAR',
  aurillac: 'AUR',
  montauban: 'MTB',
  valence: 'VAL',
  'valence romans': 'VAL',
};

function normalizeClubLabel(label: string): string {
  return label
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/\s+/g, ' ');
}

export function resolveClubShort(label: string | null | undefined): string | null {
  if (!label) {
    return null;
  }
  const trimmed = label.trim();
  if (!trimmed || trimmed === '—') {
    return null;
  }
  if (TEAM_BRANDS[trimmed]) {
    return trimmed;
  }
  const upper = trimmed.toUpperCase();
  if (TEAM_BRANDS[upper]) {
    return upper;
  }
  const normalized = normalizeClubLabel(trimmed);
  const direct = CLUB_LABEL_TO_SHORT[normalized] ?? CLUB_LABEL_TO_SHORT[trimmed.toLowerCase()];
  if (direct) {
    return direct;
  }
  for (const [key, short] of Object.entries(CLUB_LABEL_TO_SHORT)) {
    if (normalized.includes(normalizeClubLabel(key)) || normalizeClubLabel(key).includes(normalized)) {
      return short;
    }
  }
  return null;
}
