import { Injectable, inject, signal } from '@angular/core';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

export interface NavOrigin {
  path: string;
  label: string;
  scrollY: number;
}

@Injectable({
  providedIn: 'root',
})
export class NavBack {
  readonly revision = signal(0);

  private currentFullUrl = '/';
  private currentPath = '/';
  private scrollYBeforeNav = 0;
  private readonly origins = new Map<string, NavOrigin>();
  private pendingRestorePath: string | null = null;
  private pendingScrollY = 0;

  private readonly router = inject(Router);

  constructor() {
    this.currentFullUrl = this.stripHash(this.router.url);
    this.currentPath = this.normalize(this.currentFullUrl);
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.scrollYBeforeNav =
          typeof window !== 'undefined' ? window.scrollY : 0;
        return;
      }
      if (!(event instanceof NavigationEnd)) {
        return;
      }
      const nextFull = this.stripHash(event.urlAfterRedirects);
      const nextPath = this.normalize(nextFull);
      if (nextPath !== this.currentPath) {
        this.remember(nextPath, this.currentFullUrl, this.scrollYBeforeNav);
        this.currentFullUrl = nextFull;
        this.currentPath = nextPath;
      } else {
        this.currentFullUrl = nextFull;
      }
    });
  }

  originFor(url: string): NavOrigin {
    const key = this.normalize(url);
    return (
      this.origins.get(key) ?? {
        path: '/',
        label: '← Accueil',
        scrollY: 0,
      }
    );
  }

  hasOrigin(url: string): boolean {
    return this.origins.has(this.normalize(url));
  }

  prepareBack(url: string): void {
    const origin = this.origins.get(this.normalize(url));
    if (origin && origin.scrollY > 0) {
      this.pendingRestorePath = this.normalize(origin.path);
      this.pendingScrollY = origin.scrollY;
    } else {
      this.pendingRestorePath = null;
      this.pendingScrollY = 0;
    }
  }

  consumeRestoreScroll(path: string): number {
    if (this.pendingRestorePath !== this.normalize(path)) {
      return 0;
    }
    const y = this.pendingScrollY;
    this.pendingRestorePath = null;
    this.pendingScrollY = 0;
    return y;
  }

  private remember(destination: string, fromFull: string, scrollY: number): void {
    const fromPath = this.normalize(fromFull);
    if (!this.isDetail(destination) || !fromPath || fromPath === destination) {
      return;
    }
    const fromOrigin = this.origins.get(fromPath);
    if (fromOrigin && this.normalize(fromOrigin.path) === destination) {
      return;
    }
    this.origins.set(destination, {
      path: fromFull || '/',
      label: this.labelFor(fromPath),
      scrollY,
    });
    this.revision.update((value) => value + 1);
  }

  private isDetail(path: string): boolean {
    if (/^\/clubs\/[^/]+\/[^/]+$/.test(path)) {
      return true;
    }
    if (/^\/players\/\d+$/.test(path)) {
      return true;
    }
    if (/^\/news\/\d+$/.test(path)) {
      return true;
    }
    if (/^\/matches\/\d+$/.test(path)) {
      return true;
    }
    return false;
  }

  private labelFor(path: string): string {
    if (path === '/' || path === '') {
      return '← Accueil';
    }
    if (path.startsWith('/transfers')) {
      return '← Transferts';
    }
    if (path === '/news') {
      return '← Revenir aux actualités';
    }
    if (/^\/news\/\d+$/.test(path)) {
      return '← Article';
    }
    if (path.startsWith('/clubs/')) {
      return '← Effectif';
    }
    if (path.startsWith('/standings')) {
      return '← Classement';
    }
    if (path.startsWith('/fixtures')) {
      return '← Matchs';
    }
    if (path.startsWith('/results')) {
      return '← Résultats';
    }
    if (path.startsWith('/matches/')) {
      return '← Match';
    }
    return '← Accueil';
  }

  private stripHash(url: string): string {
    const path = (url || '/').split('#')[0];
    return path || '/';
  }

  private normalize(url: string): string {
    const path = this.stripHash(url).split('?')[0];
    if (!path || path === '') {
      return '/';
    }
    return path.length > 1 && path.endsWith('/') ? path.slice(0, -1) : path;
  }
}
