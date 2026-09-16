import { Component, computed, input, signal } from '@angular/core';
import { getTeamBrand, teamLogoUrl } from '../team-branding';

@Component({
  selector: 'app-team-logo',
  templateUrl: './team-logo.html',
  styleUrl: './team-logo.css',
})
export class TeamLogo {
  shortName = input.required<string>();
  size = input<'sm' | 'md' | 'lg'>('md');
  showLabel = input(false);

  private imgFailed = signal(false);

  logoSrc = computed(() => teamLogoUrl(this.shortName()));

  showBadge = computed(() => this.imgFailed());

  brand(): { bg: string; text: string } {
    return getTeamBrand(this.shortName());
  }

  onImgError(): void {
    this.imgFailed.set(true);
  }
}
