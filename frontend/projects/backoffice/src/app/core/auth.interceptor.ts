import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { SessionStore } from './session.store';

/** Points où le cookie de renouvellement doit accompagner la demande. */
const CHEMINS_AVEC_COOKIE = '/api/v1/auth/';

/**
 * Intercepteur unique : jeton d'accès, langue, cookie de renouvellement
 * (frontend/CLAUDE.md).
 *
 * <p>Il n'essaie pas de renouveler tout seul à la première erreur 401 : une
 * boucle de renouvellement silencieuse masque les vraies pertes de session. La
 * reprise se fait une fois, au chargement de l'application.
 */
export const authInterceptor: HttpInterceptorFn = (demande, suivant) => {
  const session = inject(SessionStore);
  const jeton = session.accessToken();
  const langue = session.current()?.language ?? 'fr';

  const enrichie = demande.clone({
    setHeaders: {
      ...(jeton ? { Authorization: `Bearer ${jeton}` } : {}),
      'Accept-Language': langue,
    },
    // Le cookie est borné au chemin d'authentification côté serveur.
    withCredentials: demande.url.includes(CHEMINS_AVEC_COOKIE),
  });

  return suivant(enrichie).pipe(
    catchError((echec: HttpErrorResponse) => {
      if (echec.status === 401 && jeton) {
        // La session est perdue : l'utilisateur la rouvre lui-même.
        session.vider();
      }
      return throwError(() => echec);
    }),
  );
};
