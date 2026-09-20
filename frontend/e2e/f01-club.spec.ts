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

    await expect(page.locator('.page__succes')).toHaveText(/enregistrés/i);
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

  test('S19 — Le gérant change la couleur du club', async ({ page }) => {
    const choisie = '#8a2f6b';

    await page.getByTestId('couleur-principale').fill(choisie);
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();

    // L'accent change sous les yeux : c'est la preuve que c'est enregistré.
    // Mesuré par sondage : la confirmation s'affiche avant que le jeton de
    // marque ne soit reposé, et comparer une seule fois rendrait le test
    // capricieux.
    await expect
      .poll(async () =>
        (
          await page.evaluate(() =>
            getComputedStyle(document.documentElement).getPropertyValue('--brand-primary'),
          )
        )
          .trim()
          .toLowerCase(),
      )
      .toBe(choisie);

    // Et la couleur survit au rechargement : elle vient bien du club.
    await page.reload();
    await expect(page.getByTestId('couleur-principale')).toHaveValue(choisie);

    // On remet la palette du logo pour les scénarios suivants.
    await page.getByTestId('couleur-principale').fill('#307890');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();
  });

  test('S20 — Le gérant modifie une règle configurable', async ({ page }) => {
    const ligne = page.locator('.tableau--dans-carte tbody tr')
      .filter({ hasText: 'files.link_ttl_minutes' });
    const champ = ligne.getByRole('spinbutton');

    await champ.fill('25');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();

    // La valeur est retenue, et l'origine dit désormais que c'est un choix du club.
    await page.reload();
    await expect(ligne.getByRole('spinbutton')).toHaveValue('25');
    await expect(ligne).toContainText('Choix du club');

    // On remet le défaut pour les scénarios suivants.
    await ligne.getByRole('spinbutton').fill('15');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();
  });

  test('S12 — Lire les règles configurables du club', async ({ page }) => {
    const tableau = page.locator('.tableau--dans-carte tbody tr');
    await expect(tableau.first()).toBeVisible();

    const duree = tableau.filter({ hasText: 'files.link_ttl_minutes' });
    await expect(duree).toContainText('15');
    // L'origine dit si la valeur vient de Clubify ou d'un choix du club (C31).
    await expect(duree).toContainText(/Défaut Clubify|Choix du club|Fixée par Clubify/);
    // Et la règle s'explique en français, sans jargon.
    await expect(duree).toContainText(/Durée d'un lien de fichier/);
    await expect(duree).toContainText(/minutes pendant lesquelles un lien/i);

    // Une règle que le club ne peut pas changer le dit franchement.
    const longueur = tableau.filter({ hasText: 'security.password.min_length' });
    await expect(longueur).toContainText('Fixée par Clubify');
  });
});
