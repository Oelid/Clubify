import { expect, test } from '@playwright/test';
import { administrateur, configurationManquante, MOT_DE_PASSE_JETABLE } from './support/environnement';
import { ApiDeRecette } from './support/api';
import { marqueDuPassage } from './support/environnement';
import { entrerCommeAdministrateur, franchirAvec, seConnecter } from './support/pages';

/**
 * Recette fonctionnelle de F01 — entrer dans l'application.
 *
 * Tout passe par les écrans : ce qu'on vérifie, c'est ce que l'accueil et le
 * gérant verront. Les identifiants des scénarios (S01…) renvoient à
 * docs/suivi-tests.md, et de là au classeur de suivi.
 */
test.beforeAll(() => {
  const manque = configurationManquante();
  test.skip(manque !== null, manque ?? '');
});

test.describe('F01 — Connexion', () => {
  test("S01 — Première connexion de l'administrateur", async ({ page }) => {
    // Depuis la décision 0031, rien ne bloque : le mot de passe suffit à entrer,
    // et le second facteur se franchit si ce compte en a un.
    await entrerCommeAdministrateur(page);

    // L'en-tête nomme la personne connectée : elle sait sous quel compte elle agit.
    await expect(page.locator('.shell__compte-nom')).not.toBeEmpty();
    // Et la navigation lui ouvre ce que son rôle permet.
    await expect(page.getByRole('link', { name: 'Utilisateurs' })).toBeVisible();
  });

  test('S02 — Mot de passe erroné', async ({ page }) => {
    await seConnecter(page, administrateur.email, 'mot-de-passe-qui-ne-vaut-rien');

    // Le message vient du code du backend, traduit : jamais un libellé en dur.
    await expect(page.getByTestId('erreur')).toHaveText(/incorrect/i);
    await expect(page).toHaveURL(/connexion/);
    // Rien du mot de passe saisi ne doit rester à l'écran.
    await expect(page.locator('body')).not.toContainText('mot-de-passe-qui-ne-vaut-rien');
  });

  test('S03 — Cinq échecs de suite verrouillent le compte', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const victime = await api.creerUtilisateur('FRONT_DESK', marqueDuPassage());
    await api.fermer();

    for (let essai = 0; essai < 5; essai += 1) {
      await seConnecter(page, victime, 'mauvais-mot-de-passe');
      await expect(page.getByTestId('erreur')).toBeVisible();
    }

    // Le bon mot de passe ne rouvre rien tant que le verrou tient (C10b).
    await seConnecter(page, victime, MOT_DE_PASSE_JETABLE);
    await expect(page.getByTestId('erreur')).toHaveText(/bloqué|verrouill/i);
    await expect(page).toHaveURL(/connexion/);
  });

  test('S04 — Connexion d’un compte déjà inscrit au second facteur', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const inscrit = await api.creerUtilisateurInscrit('MANAGER', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, inscrit.email, MOT_DE_PASSE_JETABLE);

    // Ni QR ni nouveaux codes : le compte est déjà inscrit, on lui demande un code.
    await expect(page.getByTestId('qr-code')).toHaveCount(0);
    await expect(page.getByTestId('codes-de-secours')).toHaveCount(0);
    await expect(page.getByLabel('Code à six chiffres')).toBeVisible();

    await franchirAvec(page, inscrit.secret);
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
    // Le rappel disparaît : ce compte est protégé.
    await expect(page.getByTestId('rappel-second-facteur')).toHaveCount(0);
  });

  test('S10 — Se déconnecter', async ({ page }) => {
    await entrerCommeAdministrateur(page);

    await page.getByRole('button', { name: 'Se déconnecter' }).click();
    await expect(page).toHaveURL(/connexion/);

    // Revenir en arrière ne rouvre pas une session fermée.
    await page.goBack();
    await expect(page).toHaveURL(/connexion/);
    await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
  });

  test('S11 — Recharger la page', async ({ page }) => {
    await entrerCommeAdministrateur(page);

    await page.reload();

    // Le jeton d'accès n'a pas survécu ; le cookie de renouvellement, si.
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Connexion' })).toHaveCount(0);
  });
});

/**
 * Rappel du second facteur (décision 0031) : le gérant entre sans l'avoir
 * activé, et l'application le lui redit sur chaque écran.
 */
test.describe('F01 — Rappel du second facteur', () => {
  test("S17 — Le gérant entre sans second facteur et voit un rappel permanent", async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const gerant = await api.creerUtilisateur('MANAGER', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, gerant, MOT_DE_PASSE_JETABLE);

    // Rien ne bloque : la session est pleine dès la première connexion.
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();

    const rappel = page.getByTestId('rappel-second-facteur');
    await expect(rappel).toBeVisible();
    await expect(rappel).toContainText(/second facteur/i);
    // Aucun bouton pour le fermer : un avertissement masquable n'avertit qu'une fois.
    await expect(rappel.getByRole('button')).toHaveCount(1);
    await expect(rappel.getByRole('button', { name: /activer/i })).toBeVisible();

    // Et il suit d'écran en écran.
    await page.getByRole('link', { name: "Journal d'audit" }).click();
    await expect(page.getByRole('heading', { name: "Journal d'audit" })).toBeVisible();
    await expect(page.getByTestId('rappel-second-facteur')).toBeVisible();
  });

  test("S18 — L'accueil n'est jamais invitée à activer un second facteur", async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const accueil = await api.creerUtilisateur('FRONT_DESK', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, accueil, MOT_DE_PASSE_JETABLE);
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();

    // Le rappel ne s'adresse qu'aux rôles qui ouvrent l'argent et le sensible.
    await expect(page.getByTestId('rappel-second-facteur')).toHaveCount(0);
  });
});
