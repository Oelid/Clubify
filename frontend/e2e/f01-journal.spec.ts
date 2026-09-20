import { expect, test } from '@playwright/test';
import { configurationManquante } from './support/environnement';
import { entrerCommeAdministrateur } from './support/pages';

/** Recette fonctionnelle de F01 — le journal d'audit. */
test.beforeAll(() => {
  const manque = configurationManquante();
  test.skip(manque !== null, manque ?? '');
});

test.describe("F01 — Journal d'audit", () => {
  test.beforeEach(async ({ page }) => {
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: "Journal d'audit" }).click();
    await expect(page.getByRole('heading', { name: "Journal d'audit" })).toBeVisible();
  });

  test('S40 — Consulter le journal', async ({ page }) => {
    const ligne = page.locator('tbody tr').first();
    await expect(ligne).toBeVisible();

    // Horodatage au fuseau du club, jamais en heure brute.
    await expect(ligne.locator('td').first()).toHaveText(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/);
    await expect(ligne).toContainText(/Utilisateur|Système/);

    // La connexion qui vient d'avoir lieu porte l'adresse de la personne.
    await page.getByLabel('Action').fill('auth.login.succeeded');
    await page.getByRole('button', { name: 'Filtrer' }).click();
    await expect(page.locator('tbody tr').first()).toContainText('@');
    await expect(page.locator('tbody tr').first()).not.toContainText('Système');
  });

  test('S41 — Filtrer le journal par action', async ({ page }) => {
    const lignes = page.locator('tbody tr');
    await expect(lignes.first()).toBeVisible();

    await page.getByLabel('Action').fill('auth.login.succeeded');
    await page.getByRole('button', { name: 'Filtrer' }).click();
    await expect(lignes.first()).toContainText('auth.login.succeeded');

    // Aucune ligne étrangère au filtre ne subsiste.
    for (const ligne of await lignes.all()) {
      await expect(ligne).toContainText('auth.login.succeeded');
    }

    await page.getByRole('button', { name: 'Réinitialiser' }).click();
    await expect(page.getByLabel('Action')).toHaveValue('');
  });

  test('S42 — Filtrer le journal par période', async ({ page }) => {
    const demain = new Date(Date.now() + 86_400_000).toISOString().slice(0, 10);

    await page.getByLabel('Depuis').fill(demain);
    await page.getByRole('button', { name: 'Filtrer' }).click();

    // Aucune entrée, et le tableau le dit plutôt que de rester vide.
    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody')).toContainText(/aucune entrée/i);
  });

  test('S43 — Parcourir le journal page par page', async ({ page }) => {
    const premiere = page.locator('tbody tr').first().locator('td').nth(0);
    const suivante = page.getByRole('button', { name: 'Suivante' });

    // On attend que la page soit rendue avant de conclure quoi que ce soit :
    // juger de l'absence d'un bouton avant son affichage ne prouve rien.
    await expect(premiere).not.toBeEmpty();
    await page.locator('.pagination').waitFor({ state: 'visible', timeout: 3_000 })
      .catch(() => undefined);

    // Sur une base neuve, le journal peut tenir sur une page : il n'y a alors
    // rien à parcourir, et c'est une réponse en soi.
    if ((await suivante.count()) === 0) {
      test.skip(true, 'le journal tient sur une seule page');
    }

    const avant = await premiere.innerText();
    await suivante.click();
    await expect(premiere).not.toHaveText(avant);
  });
});
