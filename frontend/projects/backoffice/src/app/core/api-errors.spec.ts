import { HttpErrorResponse } from '@angular/common/http';
import { codeDErreur, detailDErreur } from './api-errors';

/**
 * Le code du backend fait foi ; son message ne sert que de repli. C'est ce qui
 * permet de traduire une erreur sans dépendre de la langue du serveur.
 */
describe('Erreurs de l\u2019API', () => {
  it('retient le code stable du ProblemDetail', () => {
    const echec = new HttpErrorResponse({
      status: 422,
      error: { code: 'club.name.required', detail: 'Le nom du club est obligatoire.' },
    });

    expect(codeDErreur(echec)).toBe('club.name.required');
    expect(detailDErreur(echec)).toBe('Le nom du club est obligatoire.');
  });

  it('retombe sur le statut quand la réponse ne porte aucun code', () => {
    expect(codeDErreur(new HttpErrorResponse({ status: 403 }))).toBe('http.403');
  });

  it('distingue un serveur injoignable d\u2019une erreur du serveur', () => {
    expect(codeDErreur(new HttpErrorResponse({ status: 0 }))).toBe('network.unreachable');
  });

  it('nomme l\u2019inattendu plutôt que de laisser une chaîne vide', () => {
    expect(codeDErreur(new Error('boum'))).toBe('error.unexpected');
  });
});
