import { defineConfig, devices } from '@playwright/test';

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
