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
