import { readFileSync } from 'node:fs';
import { expect, test } from '@playwright/test';
import { ApiDeRecette } from './support/api';
import { configurationManquante, marqueDuPassage, MOT_DE_PASSE_JETABLE } from './support/environnement';
import { entrerCommeAdministrateur, seConnecter } from './support/pages';

/** Recette fonctionnelle de F01 — la liste du personnel. */
test.beforeAll(() => {
  const manque = configurationManquante();
  test.skip(manque !== null, manque ?? '');
});

/** Le total affiché par la plage : « 1–20 sur 93 » donne 93. */
const totalAffiche = async (texte: string): Promise<number> =>
  Number(texte.split('sur')[1].trim());

test.describe('F01 — Utilisateurs', () => {
  test('S20 — Consulter la liste', async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    const ligne = page.locator('tbody tr').first();
    await expect(ligne).toBeVisible();
    await expect(ligne).toContainText(/Administrateur du compte|Gérant|Accueil|Coach/);
    await expect(ligne).toContainText(/Actif|Non activé/);
    // La dernière connexion est datée au fuseau du club, jamais en heure brute.
    await expect(ligne.locator('td').last()).toHaveText(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}|Jamais/);
  });

  test('S21 — Parcourir la liste page par page', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    await api.assurerAuMoins(25, marqueDuPassage());
    await api.fermer();

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    const plage = page.getByTestId('plage');
    const premiereAdresse = page.locator('tbody tr').first().locator('td').nth(1);

    await expect(plage).toContainText('1–20');
    const adresse = await premiereAdresse.innerText();

    await page.getByRole('button', { name: 'Suivante' }).click();
    await expect(plage).toContainText('21–');
    await expect(premiereAdresse).not.toHaveText(adresse);

    await page.getByRole('button', { name: 'Précédente' }).click();
    await expect(plage).toContainText('1–20');
    await expect(premiereAdresse).toHaveText(adresse);
  });

  test('S22 — Changer le nombre de lignes par page', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    await api.assurerAuMoins(35, marqueDuPassage());
    await api.fermer();

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    await page.getByRole('button', { name: 'Suivante' }).click();
    await expect(page.getByTestId('plage')).toContainText('21–');

    await page.getByTestId('lignes-par-page').selectOption('50');

    // Changer la taille ramène au début : on ne reste jamais sur une page qui
    // n'existe plus.
    await expect(page.getByTestId('plage')).toContainText('1–');
    await expect(page.locator('tbody tr')).toHaveCount(50);
  });

  test('S23 — Chercher après avoir paginé', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    await api.assurerAuMoins(35, marqueDuPassage());
    const cherche = await api.creerUtilisateur('COACH', marqueDuPassage());
    await api.fermer();

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    await page.getByTestId('lignes-par-page').selectOption('30');
    await page.getByRole('button', { name: 'Suivante' }).click();
    await expect(page.getByTestId('plage')).toContainText('31–');

    await page.getByTestId('recherche').fill(cherche);

    // La recherche ramène au début et ne laisse qu'une ligne : celle cherchée.
    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody')).toContainText(cherche);
    await expect(page.getByTestId('plage')).toContainText('1–1 sur 1');
    await expect(page.getByRole('button', { name: 'Suivante' })).toBeDisabled();
    await expect(page.getByRole('button', { name: 'Précédente' })).toBeDisabled();
  });

  test('S24 — Filtrer par rôle', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    await api.creerUtilisateur('COACH', marqueDuPassage());
    await api.fermer();

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();
    const tout = await totalAffiche(await page.getByTestId('plage').innerText());

    await page.getByTestId('filtre-role').selectOption('COACH');

    for (const ligne of await page.locator('tbody tr').all()) {
      await expect(ligne).toContainText('Coach');
    }
    expect(await totalAffiche(await page.getByTestId('plage').innerText()))
      .toBeLessThanOrEqual(tout);
  });

  test('S25 — Filtrer par statut, puis réinitialiser', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const ferme = await api.creerUtilisateur('COACH', marqueDuPassage());
    await api.desactiver(ferme);
    await api.fermer();

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    await page.getByTestId('filtre-statut').selectOption('false');
    for (const ligne of await page.locator('tbody tr').all()) {
      await expect(ligne).toContainText('Désactivé');
    }

    await page.getByRole('button', { name: 'Réinitialiser' }).click();
    await expect(page.getByTestId('filtre-statut')).toHaveValue('');
    await expect(page.getByTestId('filtre-role')).toHaveValue('');
  });

  test('S26 — Créer un compte', async ({ page }) => {
    const adresse = `accueil.${marqueDuPassage()}@exemple.test`;

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();
    await page.getByTestId('ajouter').click();

    const formulaire = page.getByTestId('formulaire-utilisateur');
    await formulaire.getByLabel('Prénom').fill('Recette');
    await formulaire.getByLabel('Nom', { exact: true }).fill('Nouveau');
    await formulaire.getByLabel('Adresse électronique').fill(adresse);
    await formulaire.getByTestId('role').selectOption('FRONT_DESK');
    await formulaire.getByLabel('Mot de passe').fill(MOT_DE_PASSE_JETABLE);
    await formulaire.getByRole('button', { name: 'Créer le compte' }).click();

    await expect(page.locator('.page__succes')).toContainText('Recette Nouveau');
    await expect(page.getByTestId('formulaire-utilisateur')).toHaveCount(0);

    await page.getByTestId('recherche').fill(adresse);
    await expect(page.locator('tbody tr')).toHaveCount(1);

    // C'est un vrai compte, pas une ligne de tableau : il se connecte.
    await page.getByRole('button', { name: 'Se déconnecter' }).click();
    await seConnecter(page, adresse, MOT_DE_PASSE_JETABLE);
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
  });

  test('S27 — Adresse déjà utilisée', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const existant = await api.creerUtilisateur('COACH', marqueDuPassage());
    await api.fermer();

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();
    await page.getByTestId('ajouter').click();

    const formulaire = page.getByTestId('formulaire-utilisateur');
    await formulaire.getByLabel('Prénom').fill('Recette');
    await formulaire.getByLabel('Nom', { exact: true }).fill('Doublon');
    await formulaire.getByLabel('Adresse électronique').fill(existant);
    await formulaire.getByLabel('Mot de passe').fill(MOT_DE_PASSE_JETABLE);
    await formulaire.getByRole('button', { name: 'Créer le compte' }).click();

    await expect(page.locator('.page__erreur')).toContainText(/déjà utilisée/i);
    await expect(formulaire).toBeVisible();
  });

  test('S28 — Mot de passe trop court', async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();
    await page.getByTestId('ajouter').click();

    const formulaire = page.getByTestId('formulaire-utilisateur');
    await formulaire.getByLabel('Prénom').fill('Recette');
    await formulaire.getByLabel('Nom', { exact: true }).fill('Court');
    await formulaire.getByLabel('Adresse électronique')
      .fill(`court.${marqueDuPassage()}@exemple.test`);
    // Onze caractères : un de moins que le minimum.
    await formulaire.getByLabel('Mot de passe').fill('Onze1234567');
    await formulaire.getByRole('button', { name: 'Créer le compte' }).click();

    await expect(page.locator('.page__erreur')).toContainText(/trop court/i);
    await expect(formulaire).toBeVisible();
  });

  test("S29 — Les rôles proposés sont ceux que l'application sait attribuer", async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();
    await page.getByTestId('ajouter').click();

    const choix = await page.getByTestId('role').locator('option').allTextContents();

    expect(choix).toContain('Coach');
    expect(choix).toContain('Accueil');
    expect(choix).not.toContain('Parent');
    expect(choix).not.toContain('Comptable');
  });

  test('S30 — Exporter la liste du personnel', async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    const telechargement = page.waitForEvent('download');
    await page.getByTestId('exporter').click();
    const fichier = await telechargement;

    expect(fichier.suggestedFilename()).toBe('utilisateurs.csv');

    const contenu = readFileSync(await fichier.path(), 'utf-8');
    expect(contenu).toContain('Courriel');
    expect(contenu).toContain('@exemple.test');
    // Aucune colonne sensible n'y figure (SEC-03, critère C34).
    expect(contenu).not.toMatch(/mot de passe|password|hash/i);
  });

  test('S31 — Un rôle sans écran le sait', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const coach = await api.creerUtilisateur('COACH', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, coach, MOT_DE_PASSE_JETABLE);

    await expect(page.getByRole('heading', { name: /espace n.est pas encore ouvert/i }))
      .toBeVisible();
  });
});
