import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { defineConfig, devices } from '@playwright/test';

/**
 * Accès de l'instance de développement, lus dans `.env.dev` à la racine.
 *
 * <p>Sans cela, il faudrait exporter trois variables à la main avant chaque
 * passage, et le secret du second facteur finirait dans un historique de
 * commandes. Le fichier n'est pas versionné ; ce qui est déjà dans
 * l'environnement gagne, pour qu'une autre machine puisse l'emporter.
 */
const chargerLesAcces = (): void => {
  try {
    const contenu = readFileSync(resolve(__dirname, '..', '.env.dev'), 'utf-8');
    for (const ligne of contenu.split(/\r?\n/)) {
      const trouve = /^([A-Z0-9_]+)=(.*)$/.exec(ligne.trim());
      if (trouve && !process.env[trouve[1]]) {
        process.env[trouve[1]] = trouve[2];
      }
    }
  } catch {
    // Absent : la suite s'arrêtera d'elle-même, en disant ce qui manque.
  }
};

chargerLesAcces();

/**
 * Recette fonctionnelle : ce que l'accueil et le gérant font devant l'écran.
 *
 * <p>Les scénarios portent les identifiants de `docs/suivi-tests.md` (S01…), et
 * le rapport JUnit alimente `docs/suivi-tests.xlsx` : un résultat du classeur
 * vient toujours d'une exécution, jamais d'une saisie.
 *
 * <p>La suite a besoin d'un backend et d'un club amorcé ; sans les variables
 * d'environnement du compte de recette, elle s'arrête en le disant.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: [
    [process.env['CI'] ? 'github' : 'list'],
    ['junit', { outputFile: '../reports/e2e-junit.xml' }],
  ],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'on-first-retry',
    // Une capture d'un échec vaut mieux qu'un message : on relit l'écran tel
    // qu'il était. Aucune donnée d'enfant n'y figure (CLAUDE.md §7).
    screenshot: 'only-on-failure',
    locale: 'fr-FR',
    timezoneId: 'Africa/Casablanca',
  },
  projects: [
    {
      name: 'chrome',
      // Le Chrome installé sur la machine, plutôt qu'une copie téléchargée :
      // c'est le navigateur du comptoir, et la recette n'exige aucun
      // téléchargement de 150 Mo pour tourner.
      use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    },
  ],
  webServer: {
    command: 'npm start',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env['CI'],
    timeout: 120_000,
  },
});
