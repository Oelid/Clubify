import { expect, test } from '@playwright/test';
import { configurationManquante } from './support/environnement';
import { entrerCommeAdministrateur } from './support/pages';

/** Recette fonctionnelle de F01 — paramètres du club et règles configurables. */
test.beforeAll(() => {
  const manque = configurationManquante();
  test.skip(manque !== null, manque ?? '');
});

test.describe('F01 — Paramètres du club', () => {
  test.beforeEach(async ({ page }) => {
    await entrerCommeAdministrateur(page);
  });

  test("S05 — Le gérant modifie l'identité du club", async ({ page }) => {
    const nom = `Club de recette ${Date.now() % 100000}`;

    await page.getByLabel('Nom du club').fill(nom);
    // Saisi comme au comptoir, avec des espaces et sans indicatif.
    await page.getByLabel('Téléphone').fill('06 12 34 56 78');
    await page.getByRole('button', { name: 'Enregistrer' }).click();

    await expect(page.getByRole('status')).toHaveText(/enregistrés/i);
    // Le backend normalise : l'écran montre ce qui a réellement été retenu.
    await expect(page.getByLabel('Téléphone')).toHaveValue('+212612345678');

    await page.getByRole('link', { name: "Journal d'audit" }).click();
    await expect(page.getByRole('heading', { name: "Journal d'audit" })).toBeVisible();
    // La modification laisse une trace, avec son auteur (SEC-04).
    await expect(page.getByText('club.updated').first()).toBeVisible();
  });

  test('S06 — Nom du club laissé vide', async ({ page }) => {
    const avant = await page.getByLabel('Nom du club').inputValue();

    await page.getByLabel('Nom du club').fill('');
    await page.getByRole('button', { name: 'Enregistrer' }).click();

    await expect(page.getByTestId('erreur')).toHaveText(/nom du club est obligatoire/i);

    // Rien n'a été enregistré : le nom d'origine revient au rechargement.
    await page.reload();
    await expect(page.getByLabel('Nom du club')).toHaveValue(avant);
  });

  test('S12 — Lire les règles configurables du club', async ({ page }) => {
    const tableau = page.locator('.tableau--dans-carte tbody tr');
    await expect(tableau.first()).toBeVisible();

    const duree = tableau.filter({ hasText: 'files.link_ttl_minutes' });
    await expect(duree).toContainText('15');
    // L'origine dit si la valeur vient de Clubify ou d'un choix du club (C31).
    await expect(duree).toContainText(/Défaut Clubify|Choix du club/);
    // Et la source documentée dit d'où la règle sort.
    await expect(duree).toContainText('F01');
  });
});
