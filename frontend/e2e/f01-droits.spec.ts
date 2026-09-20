import { expect, test } from '@playwright/test';
import { ApiDeRecette } from './support/api';
import {
  configurationManquante,
  marqueDuPassage,
  MOT_DE_PASSE_JETABLE,
} from './support/environnement';
import { seConnecter } from './support/pages';

/**
 * Recette fonctionnelle de F01 — ce que chaque rôle peut atteindre.
 *
 * Ces scénarios se jouent avec un compte d'accueil : c'est le rôle le plus
 * employé au quotidien, et celui dont les limites comptent le plus.
 */
test.beforeAll(() => {
  const manque = configurationManquante();
  test.skip(manque !== null, manque ?? '');
});

test.describe('F01 — Droits et accès', () => {
  const entrerCommeAccueil = async (page: import('@playwright/test').Page) => {
    const api = await ApiDeRecette.enTantQuAdministrateur();
    const accueil = await api.creerUtilisateur('FRONT_DESK', marqueDuPassage());
    await api.fermer();

    await seConnecter(page, accueil, MOT_DE_PASSE_JETABLE);
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
  };

  test("S50 — L'accueil consulte les paramètres sans pouvoir les modifier", async ({ page }) => {
    await entrerCommeAccueil(page);

    await expect(page.getByLabel('Nom du club')).toBeDisabled();
    await expect(page.getByRole('button', { name: 'Enregistrer' })).toHaveCount(0);
    // Les règles configurables se lisent, sans champ pour les changer.
    await expect(page.locator('.tableau--dans-carte').getByRole('spinbutton')).toHaveCount(0);
  });

  test("S51 — L'accueil n'atteint pas le journal, même par l'adresse", async ({ page }) => {
    await entrerCommeAccueil(page);

    await expect(page.getByRole('link', { name: "Journal d'audit" })).toHaveCount(0);

    // L'adresse saisie à la main ne contourne rien.
    await page.goto('/journal');
    await expect(page.getByRole('heading', { name: "Journal d'audit" })).toHaveCount(0);
    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
  });

  test("S52 — L'accueil ne voit pas la liste du personnel", async ({ page }) => {
    await entrerCommeAccueil(page);

    await expect(page.getByRole('link', { name: 'Utilisateurs' })).toHaveCount(0);
  });
});
