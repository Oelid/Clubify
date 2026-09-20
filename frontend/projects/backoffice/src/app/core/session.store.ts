import { computed, inject, Injectable, signal } from '@angular/core';
import { CurrentUser } from 'api-client';
import { ClubContext, ClubTheme } from 'ui';

/** Ce que le backend attend avant d'ouvrir la session, ou rien. */
export type EtapeSecondFacteur = 'ACTIVATION' | 'VERIFICATION' | null;

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

  /**
   * Étape de second facteur attendue, telle que le backend l'a nommée. Jamais
   * devinée : l'activer alors qu'une simple vérification était demandée
   * régénérerait le secret et les codes de secours d'un compte déjà inscrit.
   */
  private readonly etapeSecondFacteur = signal<EtapeSecondFacteur>(null);

  readonly accessToken = this.jeton.asReadonly();
  readonly current = this.utilisateur.asReadonly();
  readonly authentifie = computed(() => this.jeton() !== null);
  readonly etape = this.etapeSecondFacteur.asReadonly();
  readonly secondFacteurAttendu = computed(() => this.etapeSecondFacteur() !== null);

  readonly nomComplet = computed(() => {
    const courant = this.utilisateur();
    return courant ? `${courant.firstName} ${courant.lastName}` : '';
  });

  /**
   * Faut-il rappeler d'activer le second facteur ? Le backend le décide : c'est
   * lui qui connaît la règle du club (décision 0031).
   */
  readonly rappelSecondFacteur = computed(() => {
    const mfa = this.utilisateur()?.mfa;
    return mfa?.expected === true && mfa.enabled !== true;
  });

  /** Date à laquelle il deviendra obligatoire, quand le club l'impose. */
  readonly secondFacteurExigeLe = computed(() => this.utilisateur()?.mfa?.requiredFrom ?? null);

  readonly initiales = computed(() => {
    const courant = this.utilisateur();
    return courant ? `${courant.firstName.charAt(0)}${courant.lastName.charAt(0)}` : '';
  });

  poserJeton(jeton: string | null): void {
    this.jeton.set(jeton);
  }

  attendreSecondFacteur(etape: EtapeSecondFacteur): void {
    this.etapeSecondFacteur.set(etape);
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

  /**
   * Premier écran que cet utilisateur peut ouvrir.
   *
   * <p>Un rôle sans aucun droit — le coach, tant que son application n'existe
   * pas — n'est pas renvoyé d'écran en écran : on le lui dit.
   */
  readonly premierEcran = computed(() => {
    const ouverts: [string, string][] = [
      ['club.settings.consulter', '/club'],
      ['users.consulter', '/utilisateurs'],
      ['audit.consulter', '/journal'],
    ];
    return ouverts.find(([droit]) => this.permet(droit))?.[1] ?? '/sans-acces';
  });

  /** L'utilisateur détient-il ce droit ? Le backend le vérifie de son côté. */
  permet(code: string): boolean {
    return this.utilisateur()?.permissions.includes(code) ?? false;
  }

  vider(): void {
    this.jeton.set(null);
    this.utilisateur.set(null);
    this.etapeSecondFacteur.set(null);
    this.theme.reset();
  }
}
