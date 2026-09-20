import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { AuthApi, LoginResponse, MfaSetupResponse, ProfileApi, TokenPair } from 'api-client';
import { catchError, firstValueFrom, of, tap } from 'rxjs';
import { LangueService } from './langue.service';
import { SessionStore } from './session.store';

/** Issue d'une tentative de connexion, telle que l'écran la lit. */
export type Connexion = 'AUTHENTICATED' | 'MFA_REQUIRED' | 'MFA_ENROLLMENT_REQUIRED';

/**
 * Parcours d'authentification (SEC-01, décision 0027).
 *
 * <p>Le service ne décide d'aucune règle : c'est le backend qui dit si le second
 * facteur est exigé. L'écran ne fait que suivre l'issue qu'il reçoit.
 */
@Injectable({ providedIn: 'root' })
export class AuthSession {
  private readonly api = inject(AuthApi);
  private readonly profil = inject(ProfileApi);
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  private readonly langue = inject(LangueService);

  /** Défi de second facteur en cours, le temps de saisir le code. */
  private defi: string | null = null;

  async connecter(email: string, motDePasse: string): Promise<Connexion> {
    const reponse = await firstValueFrom(
      this.api.login({ loginRequest: { email, password: motDePasse } }),
    );
    return this.suivre(reponse);
  }

  /** Vérifie le code à six chiffres et ouvre la session. */
  async verifierSecondFacteur(code: string, appareilDeConfiance = false): Promise<void> {
    if (!this.defi) {
      throw new Error('challenge.missing');
    }
    const jetons = await firstValueFrom(
      this.api.verifyMfa({
        mfaVerifyRequest: {
          mfaChallengeId: this.defi,
          code,
          trustDevice: appareilDeConfiance,
        },
      }),
    );
    this.defi = null;
    await this.ouvrir(jetons);
  }

  /** Secret à scanner et codes de secours, affichés une seule fois (B5). */
  async preparerSecondFacteur(): Promise<MfaSetupResponse> {
    return firstValueFrom(this.profil.setupMfa());
  }

  /**
   * Active le second facteur. Le code saisi valant preuve, le backend ouvre la
   * session : l'interface n'a jamais à retenir le mot de passe.
   */
  async activerSecondFacteur(code: string): Promise<void> {
    const jetons = await firstValueFrom(
      this.profil.confirmMfa({ mfaConfirmRequest: { code } }),
    );
    await this.ouvrir(jetons);
  }

  /**
   * Reprend la session au chargement de la page, à partir du seul cookie de
   * renouvellement. Sans lui, l'écran de connexion s'affiche.
   */
  async reprendre(): Promise<boolean> {
    const jetons = await firstValueFrom(
      this.api.refreshToken({ refreshRequest: {} }).pipe(catchError(() => of(null))),
    );
    if (!jetons) {
      return false;
    }
    await this.ouvrir(jetons);
    return true;
  }

  async deconnecter(): Promise<void> {
    await firstValueFrom(
      this.api.logout().pipe(
        // Une déconnexion aboutit toujours côté écran : le cookie et le jeton
        // partent, même si l'appel échoue.
        catchError(() => of(null)),
        tap(() => undefined),
      ),
    );
    this.session.vider();
    await this.router.navigate(['/connexion']);
  }

  private async suivre(reponse: LoginResponse): Promise<Connexion> {
    if (reponse.outcome === 'AUTHENTICATED' && reponse.tokens) {
      await this.ouvrir(reponse.tokens);
      return 'AUTHENTICATED';
    }
    if (reponse.outcome === 'MFA_ENROLLMENT_REQUIRED' && reponse.tokens) {
      // Jeton provisoire : il ne sert qu'à activer le second facteur (C6b).
      this.session.poserJeton(reponse.tokens.accessToken);
      this.session.attendreSecondFacteur(true);
      return 'MFA_ENROLLMENT_REQUIRED';
    }
    this.defi = reponse.mfaChallengeId ?? null;
    this.session.attendreSecondFacteur(true);
    return 'MFA_REQUIRED';
  }

  private async ouvrir(jetons: TokenPair): Promise<void> {
    this.session.poserJeton(jetons.accessToken);
    this.session.attendreSecondFacteur(false);
    const courant = await firstValueFrom(this.api.getCurrentUser());
    this.session.poserUtilisateur(courant);
    // La langue de l'utilisateur emporte la direction du document (PLT-08).
    this.langue.appliquer(courant.language ?? 'fr');
  }
}
