import { Injectable, signal } from '@angular/core';

/** Devise, fuseau et langue du club connecté (ADM-01), lus à la connexion. */
export interface ClubDisplaySettings {
  readonly currency: string;
  readonly timezone: string;
  readonly language: string;
}

@Injectable({ providedIn: 'root' })
export class ClubContext {
  private readonly settings = signal<ClubDisplaySettings | null>(null);

  set(settings: ClubDisplaySettings): void {
    this.settings.set(settings);
  }

  get(): ClubDisplaySettings | null {
    return this.settings();
  }
}
