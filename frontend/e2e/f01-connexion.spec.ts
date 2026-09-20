import { expect, test } from '@playwright/test';
import { ApiDeRecette } from './support/api';
import {
  administrateur,
  configurationManquante,
  marqueDuPassage,
  MOT_DE_PASSE_JETABLE,
} from './support/environnement';
import { entrerCommeAdministrateur, franchirAvec, seConnecter } from './support/pages';
import { codeTotp, secondesRestantes } from './support/totp';

/**
 * Recette fonctionnelle de F01 — entrer dans l'application.
 *
 * Tout passe par les écrans : ce qu'on vérifie, c'est ce que l'accueil et le
 * gérant verront. Les identifiants renvoient à docs/suivi-tests.md, qui décrit
 * le préalable, les étapes et le résultat attendu de chacun.
 */
test.beforeAll(() => {
  const manque = configurationManquante();
  test.skip(manque !== null, manque ?? '');
});

test.describe('F01 — Connexion et second facteur', () => {
  test('S01 — Première connexion', async ({ page }) => {
    await entrerCommeAdministrateur(page);

    await expect(page.locator('.shell__compte-nom')).not.toBeEmpty();
    await expect(page.getByRole('link', { name: 'Utilisateurs' })).toBeVisible();
  });

  test('S02 — Mot de passe erroné', async ({ page }) => {
    await seConnecter(page, administrateur.email, 'mot-de-passe-qui-ne-vaut-rien');

    await expect(page.getByTestId('erreur')).toHaveText(/incorrect/i);
    await expect(page).toHaveURL(/connexion/);
    await expect(page.locator('body')).not.toContainText('mot-de-passe-qui-ne-vaut-rien');
  });

  test('S03 — Cinq échecs verrouillent le compte', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const victime = await api.creerUtilisateur('FRONT_DESK', marqueDuPassage());
    await api.fermer();

    for (let essai = 0; essai < 5; essai += 1) {
      await seConnecter(page, victime, 'mauvais-mot-de-passe');
      await expect(page.getByTestId('erreur')).toBeVisible();
    }

    await seConnecter(page, victime, MOT_DE_PASSE_JETABLE);
    await expect(page.getByTestId('erreur')).toHaveText(/bloqué|verrouill/i);
    await expect(page).toHaveURL(/connexion/);
  });

  test('S04 — Connexion d’un compte déjà inscrit au second facteur', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const inscrit = await api.creerUtilisateurInscrit('MANAGER', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, inscrit.email, MOT_DE_PASSE_JETABLE);

    await expect(page.getByTestId('qr-code')).toHaveCount(0);
    await expect(page.getByTestId('codes-de-secours')).toHaveCount(0);
    await expect(page.getByLabel('Code à six chiffres')).toBeVisible();

    await franchirAvec(page, inscrit.secret);
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
    await expect(page.getByTestId('rappel-second-facteur')).toHaveCount(0);
  });

  test('S05 — Activer le second facteur depuis le bandeau', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const gerant = await api.creerUtilisateur('MANAGER', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, gerant, MOT_DE_PASSE_JETABLE);
    await expect(page.getByTestId('rappel-second-facteur')).toBeVisible();

    // Le secret est lu dans la réponse du backend, comme un téléphone le lirait
    // dans le QR : la recette ne s'ouvre aucune porte dérobée.
    const preparation = page.waitForResponse(
      (reponse) => reponse.url().includes('/profile/mfa/setup') && reponse.ok(),
    );
    await page.getByRole('button', { name: /activer maintenant/i }).click();
    const secret = /secret=([^&]+)/.exec((await (await preparation).json()).otpauthUri)?.[1] ?? '';

    await expect(page.getByTestId('qr-code')).toBeVisible();
    await expect(page.getByTestId('codes-de-secours').locator('li')).toHaveCount(8);

    if (secondesRestantes() < 3) {
      await page.waitForTimeout(3_500);
    }
    await page.getByLabel('Code à six chiffres').fill(codeTotp(secret));
    await page.getByRole('button', { name: /activer et continuer/i }).click();

    // La session se poursuit sans ressaisir le mot de passe, et le rappel s'efface.
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
    await expect(page.getByTestId('rappel-second-facteur')).toHaveCount(0);
  });

  test('S06 — Code de second facteur erroné', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const inscrit = await api.creerUtilisateurInscrit('MANAGER', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, inscrit.email, MOT_DE_PASSE_JETABLE);
    await page.getByLabel('Code à six chiffres').fill('000000');
    await page.getByRole('button', { name: /vérifier/i }).click();

    await expect(page.getByTestId('erreur')).toHaveText(/code incorrect/i);
    await expect(page.getByLabel('Code à six chiffres')).toHaveValue('');
  });

  test('S07 — Se déconnecter', async ({ page }) => {
    await entrerCommeAdministrateur(page);

    await page.getByRole('button', { name: 'Se déconnecter' }).click();
    await expect(page).toHaveURL(/connexion/);

    await page.goBack();
    await expect(page).toHaveURL(/connexion/);
    await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
  });

  test('S08 — Recharger la page', async ({ page }) => {
    await entrerCommeAdministrateur(page);

    await page.reload();

    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Connexion' })).toHaveCount(0);
  });

  test("S09 — L'accueil n'est jamais invitée à activer un second facteur", async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const accueil = await api.creerUtilisateur('FRONT_DESK', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, accueil, MOT_DE_PASSE_JETABLE);
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();

    await expect(page.getByTestId('rappel-second-facteur')).toHaveCount(0);
  });
});
