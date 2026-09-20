import { expect, test } from '@playwright/test';

/**
 * Parcours critique de F01 : première connexion de l'administrateur, activation
 * imposée du second facteur, puis accès au journal d'audit (plan, étape 4).
 *
 * Données fictives : aucun nom réel (CLAUDE.md §7).
 */
test.describe('Première connexion de l’administrateur', () => {
  test('du mot de passe au journal, en passant par le second facteur', async ({ page }) => {
    await page.goto('/');

    await page.getByLabel('Adresse électronique').fill('admin.a@example.test');
    await page.getByLabel('Mot de passe').fill('Motdepasse12');
    await page.getByRole('button', { name: 'Se connecter' }).click();

    // Un administrateur sans second facteur doit l'activer avant tout accès (C6b).
    await expect(page.getByRole('heading', { name: /second facteur/i })).toBeVisible();
    await expect(page.getByTestId('qr-code')).toBeVisible();
    await expect(page.getByTestId('codes-de-secours')).toBeVisible();

    await page.getByLabel('Code à six chiffres').fill(process.env['TOTP_CODE'] ?? '000000');
    await page.getByRole('button', { name: /activer/i }).click();

    await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();

    await page.getByRole('link', { name: "Journal d'audit" }).click();
    await expect(page.getByRole('heading', { name: "Journal d'audit" })).toBeVisible();
    // La connexion qui vient d'avoir lieu y figure (C18).
    await expect(page.getByText('auth.login.succeeded')).toBeVisible();
  });

  test('cinq échecs verrouillent le compte avec un message lisible', async ({ page }) => {
    await page.goto('/');

    for (let i = 0; i < 5; i++) {
      await page.getByLabel('Adresse électronique').fill('accueil.a@example.test');
      await page.getByLabel('Mot de passe').fill('mauvais');
      await page.getByRole('button', { name: 'Se connecter' }).click();
    }

    // Le message est traduit depuis le code du backend, jamais écrit en dur (C36).
    await expect(page.getByText(/compte verrouillé/i)).toBeVisible();
  });
});
