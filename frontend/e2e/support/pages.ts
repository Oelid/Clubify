import { expect, Page } from '@playwright/test';
import { administrateur } from './environnement';
import { codeTotp, secondesRestantes } from './totp';

/**
 * Gestes que plusieurs scénarios refont : se connecter, franchir le second
 * facteur. Écrits une fois, pour que chaque scénario ne raconte que ce qui lui
 * est propre.
 */

export const seConnecter = async (page: Page, email: string, motDePasse: string) => {
  await page.goto('/connexion');
  await page.getByLabel('Adresse électronique').fill(email);
  await page.getByLabel('Mot de passe').fill(motDePasse);
  await page.getByRole('button', { name: 'Se connecter' }).click();
};

/** Saisit le code du second facteur pour le compte d'administration. */
export const franchirLeSecondFacteur = async (page: Page) => {
  if (secondesRestantes() < 3) {
    await page.waitForTimeout(3_500);
  }
  await page.getByLabel('Code à six chiffres').fill(codeTotp(administrateur.secretTotp));
  await page.getByRole('button', { name: /vérifier|activer/i }).click();
};

/** Connexion complète de l'administrateur, jusqu'à l'intérieur de l'application. */
export const entrerCommeAdministrateur = async (page: Page) => {
  await seConnecter(page, administrateur.email, administrateur.motDePasse);
  await expect(page.getByRole('heading', { name: /second facteur|vérification/i })).toBeVisible();
  await franchirLeSecondFacteur(page);
  await expect(page.getByRole('heading', { name: 'Paramètres du club' })).toBeVisible();
};

export const ouvrir = async (page: Page, chemin: string) => {
  await page.goto(chemin);
};
