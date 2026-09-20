import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

/**
 * Le droite-à-gauche se tient dès le premier jour (CLAUDE.md, PLT-08) : il ne
 * se rattrape pas écran par écran. Ces contrôles lisent les feuilles de style
 * de la bibliothèque et des applications.
 */
describe('Système de design', () => {
  const racines = ['projects/ui/src', 'projects/backoffice/src'];

  function feuilles(): string[] {
    const trouvees: string[] = [];
    const parcourir = (dossier: string) => {
      for (const entree of readdirSync(dossier)) {
        const chemin = join(dossier, entree);
        if (statSync(chemin).isDirectory()) {
          parcourir(chemin);
        } else if (chemin.endsWith('.css')) {
          trouvees.push(chemin);
        }
      }
    };
    racines.forEach(parcourir);
    return trouvees;
  }

  it('n’emploie aucune propriété physique gauche ou droite', () => {
    const interdites =
      /(^|[\s;{])(margin-left|margin-right|padding-left|padding-right|border-left|border-right|text-align:\s*(left|right)|left:|right:)/;
    const fautifs = feuilles().filter((f) => interdites.test(readFileSync(f, 'utf8')));

    expect(fautifs).toEqual([]);
  });

  it('définit les jetons sémantiques attendus', () => {
    const tokens = readFileSync('projects/ui/src/lib/styles/tokens.css', 'utf8');

    for (const jeton of [
      '--color-surface',
      '--color-text',
      '--color-danger',
      '--brand-primary',
      '--font-sans',
      '--motion-fast',
      '--motion-base',
    ]) {
      expect(tokens).toContain(jeton);
    }
  });

  it('prévoit la marque du club par-dessus les jetons Clubify', () => {
    const tokens = readFileSync('projects/ui/src/lib/styles/tokens.css', 'utf8');

    // La marque part de l'accent produit et se surcharge à la connexion.
    expect(tokens).toMatch(/--brand-primary:\s*var\(--color-accent\)/);
  });

  it('coupe le mouvement quand le système le demande', () => {
    const tokens = readFileSync('projects/ui/src/lib/styles/tokens.css', 'utf8');

    expect(tokens).toContain('prefers-reduced-motion');
    expect(tokens).toMatch(/--motion-base:\s*0ms/);
  });

  it('aligne les chiffres des montants', () => {
    const tokens = readFileSync('projects/ui/src/lib/styles/tokens.css', 'utf8');

    expect(tokens).toContain('tabular-nums');
  });
});
