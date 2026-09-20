import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import libelles from '../../../public/i18n/fr.json';

/**
 * Chaque code d'erreur du backend a sa traduction.
 *
 * <p>Sans ce contrôle, un code non traduit s'affiche tel quel à l'accueil —
 * « error.auth.mfa.codeInvalid » au lieu d'une phrase. C'est arrivé, et c'est
 * la recette fonctionnelle qui l'a vu (scénario S01).
 */
describe('Traductions des erreurs', () => {
  const codesDuBackend = (): string[] => {
    const chemin = resolve(
      process.cwd(),
      '../backend/src/main/resources/messages_fr.properties',
    );
    return [...readFileSync(chemin, 'utf-8').matchAll(/^errors\.([A-Za-z0-9.]+)\s*=/gm)].map(
      (trouve) => trouve[1],
    );
  };

  const cles = (noeud: unknown, prefixe = ''): string[] =>
    Object.entries(noeud as Record<string, unknown>).flatMap(([cle, valeur]) =>
      typeof valeur === 'object' && valeur !== null
        ? cles(valeur, `${prefixe}${cle}.`)
        : [`${prefixe}${cle}`],
    );

  it('traduit tous les codes que le backend peut émettre', () => {
    const traduits = new Set(cles((libelles as Record<string, unknown>)['error']));
    const manquants = codesDuBackend().filter((code) => !traduits.has(code));

    expect(manquants, `codes sans traduction : ${manquants.join(', ')}`).toEqual([]);
  });

  it("n'emploie aucun libellé vide", () => {
    const vides = cles(libelles).filter((chemin) => {
      const valeur = chemin
        .split('.')
        .reduce<unknown>((noeud, cle) => (noeud as Record<string, unknown>)[cle], libelles);
      return typeof valeur !== 'string' || valeur.trim() === '';
    });

    expect(vides).toEqual([]);
  });
});
