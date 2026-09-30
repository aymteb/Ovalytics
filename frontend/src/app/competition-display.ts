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
