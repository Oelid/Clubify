import { expect, test } from '@playwright/test';
import { ApiDeRecette } from './support/api';
import { configurationManquante, marqueDuPassage } from './support/environnement';
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

  test("S10 — Modifier l'identité du club", async ({ page }) => {
    const nom = `Club de recette ${Date.now() % 100000}`;

    await page.getByLabel('Nom du club').fill(nom);
    await page.getByLabel('Téléphone').fill('06 12 34 56 78');
    await page.getByRole('button', { name: 'Enregistrer' }).click();

    await expect(page.locator('.page__succes')).toHaveText(/enregistrés/i);
    // Le backend normalise : l'écran montre ce qui a réellement été retenu.
    await expect(page.getByLabel('Téléphone')).toHaveValue('+212612345678');

    await page.getByRole('link', { name: "Journal d'audit" }).click();
    await expect(page.getByText('club.updated').first()).toBeVisible();
  });

  test('S11 — Nom du club laissé vide', async ({ page }) => {
    const avant = await page.getByLabel('Nom du club').inputValue();

    await page.getByLabel('Nom du club').fill('');
    await page.getByRole('button', { name: 'Enregistrer' }).click();

    await expect(page.getByTestId('erreur')).toHaveText(/nom du club est obligatoire/i);

    await page.reload();
    await expect(page.getByLabel('Nom du club')).toHaveValue(avant);
  });

  test('S12 — ICE mal formé', async ({ page }) => {
    await page.getByRole('textbox', { name: 'ICE' }).fill('12345');
    await page.getByRole('button', { name: 'Enregistrer' }).click();

    await expect(page.getByTestId('erreur')).toHaveText(/quinze chiffres/i);

    await page.reload();
    await expect(page.getByRole('textbox', { name: 'ICE' })).not.toHaveValue('12345');
  });

  test('S13 — Changer la couleur du club', async ({ page }) => {
    const choisie = '#8a2f6b';

    await page.getByTestId('couleur-principale').fill(choisie);
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();

    // L'accent change sous les yeux : c'est la preuve que c'est enregistré.
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

    await page.reload();
    await expect(page.getByTestId('couleur-principale')).toHaveValue(choisie);

    // On remet la palette du logo pour les scénarios suivants.
    await page.getByTestId('couleur-principale').fill('#307890');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();
  });

  test('S14 — Lire les règles configurables', async ({ page }) => {
    const tableau = page.locator('.tableau--dans-carte tbody tr');
    await expect(tableau.first()).toBeVisible();

    // Une règle du ressort du club : libellé, explication, valeur, origine.
    const duree = tableau.filter({ hasText: 'files.link_ttl_minutes' });
    await expect(duree).toContainText("Durée d'un lien de fichier");
    await expect(duree).toContainText(/minutes pendant lesquelles un lien/i);
    await expect(duree).toContainText(/Défaut Clubify|Choix du club/);

    // Une règle que le club ne peut pas changer le dit, et n'a pas de champ.
    const longueur = tableau.filter({ hasText: 'security.password.min_length' });
    await expect(longueur).toContainText('Fixée par Clubify');
    await expect(longueur.getByRole('spinbutton')).toHaveCount(0);
  });

  test('S15 — Modifier une règle configurable', async ({ page }) => {
    const ligne = page.locator('.tableau--dans-carte tbody tr')
      .filter({ hasText: 'files.link_ttl_minutes' });

    await ligne.getByRole('spinbutton').fill('25');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();

    await page.reload();
    await expect(ligne.getByRole('spinbutton')).toHaveValue('25');
    await expect(ligne).toContainText('Choix du club');

    // On remet le défaut pour les scénarios suivants.
    await ligne.getByRole('spinbutton').fill('15');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();
  });

  test('S16 — Le nombre de lignes par page suit la règle du club', async ({ page }) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    await api.assurerAuMoins(35, marqueDuPassage());
    await api.fermer();

    const ligne = page.locator('.tableau--dans-carte tbody tr')
      .filter({ hasText: 'ui.page_size' });
    await ligne.getByRole('spinbutton').fill('30');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();

    // La taille est annoncée à la connexion : il faut donc en rouvrir une.
    await page.getByRole('button', { name: 'Se déconnecter' }).click();
    await entrerCommeAdministrateur(page);
    await page.getByRole('link', { name: 'Utilisateurs' }).click();

    await expect(page.getByTestId('lignes-par-page')).toHaveValue('30');
    await expect(page.getByTestId('plage')).toContainText('1–30');

    // On remet le défaut pour les scénarios suivants.
    await page.getByRole('link', { name: 'Paramètres du club' }).click();
    await ligne.getByRole('spinbutton').fill('20');
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.locator('.page__succes')).toBeVisible();
  });
});
