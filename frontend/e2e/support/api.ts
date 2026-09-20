import { APIRequestContext, request } from '@playwright/test';
import { administrateur, baseApi, MOT_DE_PASSE_JETABLE } from './environnement';
import { codeTotp, secondesRestantes } from './totp';

/**
 * Accès à l'API pour ce que la recette doit préparer ou constater, et que les
 * écrans ne permettent pas encore : créer un compte d'accueil, relire le
 * journal. Les scénarios eux-mêmes passent par les écrans, jamais par ici.
 */
export class ApiDeRecette {
  private constructor(
    private readonly contexte: APIRequestContext,
    private readonly jeton: string,
  ) {}

  /** Ouvre une session d'administrateur, second facteur compris. */
  static async enTantQuAdministrateur(): Promise<ApiDeRecette> {
    const contexte = await request.newContext({ baseURL: baseApi });

    const connexion = await contexte.post('/api/v1/auth/login', {
      data: { email: administrateur.email, password: administrateur.motDePasse },
    });
    if (!connexion.ok()) {
      throw new Error(
        `Connexion de recette refusée (${connexion.status()}). `
        + 'Le club est-il amorcé et le mot de passe à jour ?',
      );
    }

    const issue = await connexion.json();
    if (issue.outcome === 'AUTHENTICATED') {
      return new ApiDeRecette(contexte, issue.tokens.accessToken);
    }
    if (issue.outcome === 'MFA_ENROLLMENT_REQUIRED') {
      throw new Error(
        "Le compte de recette n'a pas encore activé son second facteur. "
        + 'Activez-le une fois à la main et notez son secret : '
        + 'voir docs/suivi-tests.md.',
      );
    }

    const jetons = await this.verifier(contexte, issue.mfaChallengeId);
    return new ApiDeRecette(contexte, jetons.accessToken);
  }

  /** Crée un utilisateur jetable et retourne son adresse. */
  async creerUtilisateur(role: string, marque: string): Promise<string> {
    const email = `${role.toLowerCase()}.${marque}@exemple.test`;
    const reponse = await this.contexte.post('/api/v1/users', {
      headers: { Authorization: `Bearer ${this.jeton}` },
      data: {
        email,
        firstName: 'Recette',
        lastName: marque,
        role,
        password: MOT_DE_PASSE_JETABLE,
      },
    });
    if (!reponse.ok()) {
      throw new Error(`Création de ${email} refusée : ${reponse.status()} ${await reponse.text()}`);
    }
    return email;
  }

  async fermer(): Promise<void> {
    await this.contexte.dispose();
  }

  /**
   * Répond au défi de second facteur. Attend le créneau suivant quand le code
   * courant est sur le point d'expirer : un test qui échoue une fois sur dix
   * n'est plus un test.
   */
  private static async verifier(contexte: APIRequestContext, defi: string) {
    if (secondesRestantes() < 3) {
      await new Promise((suite) => setTimeout(suite, 3_500));
    }
    const reponse = await contexte.post('/api/v1/auth/mfa/verify', {
      data: { mfaChallengeId: defi, code: codeTotp(administrateur.secretTotp) },
    });
    if (!reponse.ok()) {
      throw new Error(
        `Second facteur refusé (${reponse.status()}). Le secret E2E_ADMIN_TOTP_SECRET `
        + "correspond-il au compte, et l'horloge de la machine est-elle à l'heure ?",
      );
    }
    return reponse.json();
  }
}
