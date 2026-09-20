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

  it('décrit chaque règle configurable dans les mots du club', () => {
    const chemin = resolve(
      process.cwd(),
      '../backend/src/main/java/ma/clubify/platform/service/SettingDefinitions.java',
    );
    const regles = [...readFileSync(chemin, 'utf-8').matchAll(/declarer\(\s*"([^"]+)"/g)].map(
      (trouve) => trouve[1],
    );
    expect(regles.length).toBeGreaterThan(0);

    const decrites = (libelles as Record<string, Record<string, unknown>>)['setting'] ?? {};
    const manquantes = regles.filter((cle) => {
      const entree = decrites[cle] as { label?: string; description?: string } | undefined;
      return !entree?.label || !entree.description;
    });

    // Sans description, le gérant lit une clé technique et ne sait pas ce qu'il
    // change. Une feature qui ajoute une règle la décrit dans la foulée.
    expect(manquantes, `règles à décrire : ${manquantes.join(', ')}`).toEqual([]);
  });

  it("n'emploie aucun libellé vide", () => {
    // La valeur est relevée pendant le parcours : certaines clés contiennent
    // elles-mêmes des points (« setting.club.timezone.label »), et les
    // reparcourir en découpant sur le point mènerait nulle part.
    const feuilles = (noeud: unknown, prefixe = ''): [string, unknown][] =>
      Object.entries(noeud as Record<string, unknown>).flatMap(([cle, valeur]) =>
        typeof valeur === 'object' && valeur !== null
          ? feuilles(valeur, `${prefixe}${cle}.`)
          : [[`${prefixe}${cle}`, valeur] as [string, unknown]],
      );

    const vides = feuilles(libelles)
      .filter(([, valeur]) => typeof valeur !== 'string' || valeur.trim() === '')
      .map(([chemin]) => chemin);

    expect(vides).toEqual([]);
  });
});
