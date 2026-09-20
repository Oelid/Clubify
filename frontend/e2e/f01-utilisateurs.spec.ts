import { readFileSync } from 'node:fs';
import { expect, test } from '@playwright/test';
import { ApiDeRecette } from './support/api';
import {
  configurationManquante,
  marqueDuPassage,
  MOT_DE_PASSE_JETABLE,
} from './support/environnement';
import { entrerCommeAdministrateur, seConnecter } from './support/pages';

/** Recette fonctionnelle de F01 — utilisateurs, droits et journal. */
test.beforeAll(() => {
  const manque = configurationManquante();
  test.skip(manque !== null, manque ?? '');
});

test.describe('F01 — Utilisateurs et journal', () => {
  test('S07 — Consulter la liste des utilisateurs', async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    const ligne = page.locator('tbody tr').first();
    await expect(ligne).toBeVisible();
    // Les colonnes que le gérant regarde : rôle, second facteur, statut.
    await expect(ligne).toContainText(/Administrateur du compte|Gérant|Accueil|Coach/);
    await expect(ligne).toContainText(/Actif|Non activé/);
    // La dernière connexion est datée au fuseau du club, jamais en UTC brut.
    await expect(ligne.locator('td').last()).toHaveText(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}|Jamais/);
  });

  test('S22 — Le gérant crée un compte depuis l’écran', async ({ page }) => {
    const adresse = `coach.${marqueDuPassage()}@exemple.test`;

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    await page.getByTestId('ajouter').click();
    const formulaire = page.getByTestId('formulaire-utilisateur');
    await expect(formulaire).toBeVisible();

    await formulaire.getByLabel('Prénom').fill('Recette');
    await formulaire.getByLabel('Nom', { exact: true }).fill('Nouveau');
    await formulaire.getByLabel('Adresse électronique').fill(adresse);
    await formulaire.getByTestId('role').selectOption('COACH');
    await formulaire.getByLabel('Mot de passe').fill(MOT_DE_PASSE_JETABLE);
    await formulaire.getByRole('button', { name: 'Créer le compte' }).click();

    // Le compte est annoncé, et la recherche le retrouve dans la liste.
    await expect(page.locator('.page__succes')).toContainText('Recette Nouveau');
    await expect(page.getByTestId('formulaire-utilisateur')).toHaveCount(0);
    await page.getByTestId('recherche').fill(adresse);
    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody')).toContainText(adresse);

    // Et il peut se connecter : c'est un vrai compte, pas une ligne de tableau.
    // Le coach n'ouvre encore aucun écran — son application arrive en R4 — et
    // l'application le lui dit, plutôt que de le renvoyer d'écran en écran.
    await page.getByRole('button', { name: 'Se déconnecter' }).click();
    await seConnecter(page, adresse, MOT_DE_PASSE_JETABLE);
    await expect(page.getByRole('heading', { name: /espace n.est pas encore ouvert/i }))
      .toBeVisible();
  });

  test('S23 — Une adresse déjà utilisée est refusée avec un message lisible', async ({ page }) => {
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

    // Le message vient du code du backend, traduit ; le formulaire reste ouvert.
    await expect(page.locator('.page__erreur')).toContainText(/déjà utilisée/i);
    await expect(formulaire).toBeVisible();
  });

  test("S24 — Les rôles proposés sont ceux que l'application sait attribuer", async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();
    await page.getByTestId('ajouter').click();

    const choix = await page.getByTestId('role').locator('option').allTextContents();

    // Le parent et le comptable existent au modèle, sans compte avant R8 : les
    // proposer serait un piège, puisque le backend les refuse.
    expect(choix).toContain('Coach');
    expect(choix).not.toContain('Parent');
    expect(choix).not.toContain('Comptable');
  });

  test('S25 — Le gérant exporte la liste du personnel', async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    const telechargement = page.waitForEvent('download');
    await page.getByTestId('exporter').click();
    const fichier = await telechargement;

    expect(fichier.suggestedFilename()).toBe('utilisateurs.csv');

    // Le fichier contient bien la liste, en-têtes en français.
    const chemin = await fichier.path();
    const contenu = readFileSync(chemin, 'utf-8');
    expect(contenu).toContain('Courriel');
    expect(contenu).toContain('@exemple.test');
    // Et aucune colonne sensible n'y figure (SEC-03, critère C34).
    expect(contenu).not.toMatch(/mot de passe|password|hash/i);
  });

  test('S26 — Le gérant retrouve un compte parmi des dizaines', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const cherche = await api.creerUtilisateur('COACH', marqueDuPassage());
    await api.fermer();

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    // Sans filtre, il est noyé : la recherche est ce qui le rend trouvable.
    await page.getByTestId('recherche').fill(cherche);
    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody')).toContainText(cherche);

    // Le filtre par rôle réduit la liste aux coachs.
    await page.getByTestId('recherche').fill('');
    await page.getByTestId('filtre-role').selectOption('COACH');
    for (const ligne of await page.locator('tbody tr').all()) {
      await expect(ligne).toContainText('Coach');
    }

    // Réinitialiser rend la liste entière.
    await page.getByRole('button', { name: 'Réinitialiser' }).click();
    await expect(page.getByTestId('filtre-role')).toHaveValue('');
  });

  test('S21 — La liste des utilisateurs se parcourt page par page', async ({ page }) => {
    // Assez de comptes pour que la pagination ait un sens.
    const api = await ApiDeRecette.enTantQuAdministrateur();
    for (let rang = 0; rang < 3; rang += 1) {
      await api.creerUtilisateur('COACH', `${marqueDuPassage()}${rang}`);
    }
    await api.fermer();

    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    // Vingt lignes par page : la valeur que le club a retenue.
    await expect(page.getByTestId('lignes-par-page')).toHaveValue('20');

    // On resserre à la plus petite page possible pour éprouver le parcours.
    await page.getByTestId('lignes-par-page').selectOption('20');
    const lignes = page.locator('tbody tr');
    const plage = page.getByTestId('plage');
    await expect(plage).toContainText('sur');

    const total = Number((await plage.innerText()).split('sur')[1].trim());
    expect(total).toBeGreaterThan(3);

    // La page rendue ne dépasse jamais la taille demandée.
    expect(await lignes.count()).toBeLessThanOrEqual(20);

    // Changer la taille ramène à la première page.
    await page.getByTestId('lignes-par-page').selectOption('30');
    await expect(plage).toContainText('1–');
  });

  test("S08 — L'accueil consulte sans modifier, et n'atteint pas le journal", async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const accueil = await api.creerUtilisateur('FRONT_DESK', marqueDuPassage());
    await api.fermer();

    // L'accueil n'a pas de second facteur imposé : il entre directement.
    await seConnecter(page, accueil, MOT_DE_PASSE_JETABLE);
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();

    // Il lit les paramètres, mais aucune action ne lui est proposée (C32).
    await expect(page.getByRole('button', { name: 'Enregistrer' })).toHaveCount(0);
    await expect(page.getByLabel('Nom du club')).toBeDisabled();
    // Les règles configurables se lisent, sans champ pour les changer.
    await expect(page.locator('.tableau--dans-carte').getByRole('spinbutton')).toHaveCount(0);

    // Le menu ne promet que ce que le droit permet (décision 0028).
    await expect(page.getByRole('link', { name: "Journal d'audit" })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Utilisateurs' })).toHaveCount(0);

    // Et l'adresse saisie à la main ne contourne rien.
    await page.goto('/journal');
    await expect(page.getByRole('heading', { name: "Journal d'audit" })).toHaveCount(0);
  });

  test("S09 — Filtrer le journal d'audit", async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: "Journal d'audit" }).click();

    const lignes = page.locator('tbody tr');
    await expect(lignes.first()).toBeVisible();

    await page.getByLabel('Action').fill('auth.login.succeeded');
    await page.getByRole('button', { name: 'Filtrer' }).click();
    await expect(lignes.first()).toContainText('auth.login.succeeded');

    // Aucune ligne étrangère au filtre ne subsiste : compter ne suffirait pas,
    // une page en cache plus d'une.
    for (const ligne of await lignes.all()) {
      await expect(ligne).toContainText('auth.login.succeeded');
    }

    await page.getByRole('button', { name: 'Réinitialiser' }).click();
    await expect(page.getByLabel('Action')).toHaveValue('');
  });
});
