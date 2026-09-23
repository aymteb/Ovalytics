import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class NewsNav {
  backTo: 'home' | 'news' = 'news';
  listScrollY = 0;
  private pendingRestore = false;

  leaveFromHome(): void {
    this.backTo = 'home';
    this.listScrollY = 0;
    this.pendingRestore = false;
  }

  leaveFromList(): void {
    this.backTo = 'news';
    this.listScrollY = window.scrollY;
    this.pendingRestore = false;
  }

  prepareBackToList(): void {
    this.pendingRestore = true;
  }

  consumeRestoreScroll(): number {
    if (!this.pendingRestore) {
      this.listScrollY = 0;
      return 0;
    }
    this.pendingRestore = false;
    const y = this.listScrollY;
    this.listScrollY = 0;
    return y;
  }
}
