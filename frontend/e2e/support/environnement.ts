/**
 * Ce dont la recette a besoin pour jouer l'application réelle.
 *
 * <p>Aucune valeur par défaut : une suite qui se rabat sur un compte deviné
 * échoue sans dire pourquoi, ou pire, agit sur le mauvais environnement.
 */
export interface CompteDeRecette {
  readonly email: string;
  readonly motDePasse: string;
  readonly secretTotp: string;
}

const MANQUANTES: string[] = [];

const lire = (nom: string): string => {
  const valeur = process.env[nom];
  if (!valeur) {
    MANQUANTES.push(nom);
    return '';
  }
  return valeur;
};

export const administrateur: CompteDeRecette = {
  email: lire('E2E_ADMIN_EMAIL'),
  motDePasse: lire('E2E_ADMIN_PASSWORD'),
  secretTotp: lire('E2E_ADMIN_TOTP_SECRET'),
};

export const baseApi = process.env['E2E_BASE_URL'] ?? 'http://localhost:4200';

/** Message d'arrêt, ou `null` si tout est en place. */
export const configurationManquante = (): string | null =>
  MANQUANTES.length === 0
    ? null
    : `Recette non configurée : ${MANQUANTES.join(', ')}. `
      + 'Voir « Comment lancer la recette » dans docs/suivi-tests.md.';

/** Mot de passe des comptes créés pour la recette : jamais celui d'une personne. */
export const MOT_DE_PASSE_JETABLE = 'RecetteClubify2027';

/**
 * Suffixe unique par scénario.
 *
 * <p>L'horodatage seul ne suffit pas : deux scénarios joués en parallèle dans la
 * même seconde créeraient la même adresse, et l'un des deux échouerait sur un
 * conflit qui n'a rien à voir avec ce qu'il vérifie.
 */
export const marqueDuPassage = (): string =>
  new Date().toISOString().replace(/\D/g, '').slice(2, 14)
  + Math.random().toString(36).slice(2, 6);
