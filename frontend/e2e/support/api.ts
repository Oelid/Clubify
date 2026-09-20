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

  /**
   * Crée un compte et lui active un second facteur, par le parcours réel.
   *
   * <p>Fabriquer le compte plutôt que d'en emprunter un rend le scénario
   * autonome : il ne dépend d'aucun secret rangé quelque part, et le compte
   * d'administration reste ouvrable au clavier seul.
   */
  async creerUtilisateurInscrit(role: string, marque: string):
      Promise<{ email: string; secret: string }> {
    const email = await this.creerUtilisateur(role, marque);

    const connexion = await this.contexte.post('/api/v1/auth/login', {
      data: { email, password: MOT_DE_PASSE_JETABLE },
    });
    const provisoire = (await connexion.json()).tokens.accessToken;

    const preparation = await this.contexte.post('/api/v1/profile/mfa/setup', {
      headers: { Authorization: `Bearer ${provisoire}` },
    });
    const uri = (await preparation.json()).otpauthUri as string;
    const secret = /secret=([^&]+)/.exec(uri)?.[1] ?? '';

    if (secondesRestantes() < 3) {
      await new Promise((suite) => setTimeout(suite, 3_500));
    }
    const confirmation = await this.contexte.post('/api/v1/profile/mfa/confirm', {
      headers: { Authorization: `Bearer ${provisoire}` },
      data: { code: codeTotp(secret) },
    });
    if (!confirmation.ok()) {
      throw new Error(`Activation du second facteur refusée : ${confirmation.status()}`);
    }
    return { email, secret };
  }

  /**
   * Garantit que le club compte au moins tant de personnes.
   *
   * <p>Les scénarios de pagination ont besoin d'une population ; les recréer à
   * chaque passage la ferait enfler sans fin. On ne crée que ce qui manque.
   */
  async assurerAuMoins(nombre: number, marque: string): Promise<void> {
    const reponse = await this.contexte.get('/api/v1/users?page=0&size=1', {
      headers: { Authorization: `Bearer ${this.jeton}` },
    });
    const total = (await reponse.json()).page.totalElements as number;

    for (let rang = total; rang < nombre; rang += 1) {
      await this.creerUtilisateur('COACH', `${marque}p${rang}`);
    }
  }

  /** Ferme un compte, pour éprouver le filtre par statut. */
  async desactiver(email: string): Promise<void> {
    const reponse = await this.contexte.get(
      `/api/v1/users?page=0&size=1&search=${encodeURIComponent(email)}`,
      { headers: { Authorization: `Bearer ${this.jeton}` } },
    );
    const trouve = (await reponse.json()).content[0];
    await this.contexte.put(`/api/v1/users/${trouve.id}/status`, {
      headers: { Authorization: `Bearer ${this.jeton}` },
      data: { active: false },
    });
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
