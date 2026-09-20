import { computed, inject, Injectable, signal } from '@angular/core';
import { CurrentUser } from 'api-client';
import { ClubContext, ClubTheme } from 'ui';

/**
 * Session en cours : jeton d'accès, utilisateur, club.
 *
 * <p>Le jeton d'accès ne vit qu'en mémoire (frontend/CLAUDE.md) : rechargez la
 * page et il disparaît. Le cookie HttpOnly de renouvellement, lui, survit, ce
 * qui permet de reprendre la session sans ressaisir le mot de passe, sans
 * jamais qu'un script de la page puisse lire un jeton.
 */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly clubContext = inject(ClubContext);
  private readonly theme = inject(ClubTheme);

  private readonly jeton = signal<string | null>(null);
  private readonly utilisateur = signal<CurrentUser | null>(null);

  /** Vrai dès que le second facteur reste à franchir ou à activer. */
  private readonly attenteSecondFacteur = signal(false);

  readonly accessToken = this.jeton.asReadonly();
  readonly current = this.utilisateur.asReadonly();
  readonly authentifie = computed(() => this.jeton() !== null);
  readonly secondFacteurAttendu = this.attenteSecondFacteur.asReadonly();

  readonly nomComplet = computed(() => {
    const courant = this.utilisateur();
    return courant ? `${courant.firstName} ${courant.lastName}` : '';
  });

  readonly initiales = computed(() => {
    const courant = this.utilisateur();
    return courant ? `${courant.firstName.charAt(0)}${courant.lastName.charAt(0)}` : '';
  });

  poserJeton(jeton: string | null): void {
    this.jeton.set(jeton);
  }

  attendreSecondFacteur(attendu: boolean): void {
    this.attenteSecondFacteur.set(attendu);
  }

  /**
   * Installe l'utilisateur connecté : le fuseau et la devise du club servent
   * ensuite à tout affichage de date et de montant, et sa marque à l'accent.
   */
  poserUtilisateur(courant: CurrentUser): void {
    this.utilisateur.set(courant);
    this.clubContext.set({
      currency: courant.club.currency,
      timezone: courant.club.timezone,
      language: courant.language ?? 'fr',
    });
    if (courant.club.brandPrimary) {
      this.theme.apply({
        primary: courant.club.brandPrimary,
        secondary: courant.club.brandSecondary,
      });
    } else {
      // Aucune couleur choisie : l'interface garde les jetons Clubify (0025).
      this.theme.reset();
    }
  }

  /** L'utilisateur détient-il ce droit ? Le backend le vérifie de son côté. */
  permet(code: string): boolean {
    return this.utilisateur()?.permissions.includes(code) ?? false;
  }

  vider(): void {
    this.jeton.set(null);
    this.utilisateur.set(null);
    this.attenteSecondFacteur.set(false);
    this.theme.reset();
  }
}
