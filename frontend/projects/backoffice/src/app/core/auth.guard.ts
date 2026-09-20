import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionStore } from './session.store';

/**
 * Garde des écrans du club. Un droit manquant se voit aussi côté backend : la
 * garde ne fait qu'éviter d'afficher une page vide (décision 0028).
 */
export const sessionOuverte: CanActivateFn = () => {
  const session = inject(SessionStore);
  const router = inject(Router);

  if (session.authentifie() && !session.secondFacteurAttendu()) {
    return true;
  }
  return router.createUrlTree([session.secondFacteurAttendu() ? '/second-facteur' : '/connexion']);
};

/**
 * Écrans d'authentification : inutiles quand la session est déjà ouverte, et
 * trompeurs, puisque s'y reconnecter effacerait la session en cours.
 */
export const sessionFermee: CanActivateFn = () => {
  const session = inject(SessionStore);
  const router = inject(Router);

  return session.authentifie() && !session.secondFacteurAttendu()
    ? router.createUrlTree(['/club'])
    : true;
};

/** Garde d'un écran qui exige un droit précis. */
export const exigeDroit = (code: string): CanActivateFn => () => {
  const session = inject(SessionStore);
  const router = inject(Router);

  // Vers le premier écran qui lui est ouvert, et non vers un écran au hasard
   // qui le renverrait ailleurs : un rôle sans droit tournerait en rond.
  return session.permet(code) ? true : router.createUrlTree([session.premierEcran()]);
};
