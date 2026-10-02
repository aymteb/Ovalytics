import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class PlayerNav {
  backTo: 'club' | 'transfers' = 'club';
  clubCode = '';
  clubShortName = '';

  leaveFromClub(code: string, shortName: string): void {
    this.backTo = 'club';
    this.clubCode = code;
    this.clubShortName = shortName;
  }

  leaveFromTransfers(): void {
    this.backTo = 'transfers';
    this.clubCode = '';
    this.clubShortName = '';
  }
}
