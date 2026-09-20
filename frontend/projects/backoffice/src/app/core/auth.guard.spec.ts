import { TestBed } from '@angular/core/testing';
import { provideRouter, UrlTree } from '@angular/router';
import { exigeDroit, sessionOuverte } from './auth.guard';
import { SessionStore } from './session.store';

/**
 * La garde n'est qu'une commodité d'écran : le backend refuse de son côté
 * (décision 0028). Elle évite d'ouvrir une page que l'utilisateur ne peut pas
 * remplir.
 */
describe('Gardes de routes', () => {
  let session: SessionStore;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    session = TestBed.inject(SessionStore);
  });

  const executer = (garde: typeof sessionOuverte) =>
    TestBed.runInInjectionContext(() => garde(null!, null!));

  it('renvoie à la connexion sans session', () => {
    const issue = executer(sessionOuverte) as UrlTree;

    expect(issue.toString()).toBe('/connexion');
  });

  it('renvoie au second facteur tant qu\u2019il reste à franchir', () => {
    session.poserJeton('jeton-provisoire');
    session.attendreSecondFacteur(true);

    expect((executer(sessionOuverte) as UrlTree).toString()).toBe('/second-facteur');
  });

  it('laisse passer une session complète', () => {
    session.poserJeton('jeton-d-acces');

    expect(executer(sessionOuverte)).toBe(true);
  });

  it("détourne l'écran dont l'utilisateur n'a pas le droit", () => {
    const refus = TestBed.runInInjectionContext(() =>
      exigeDroit('audit.consulter')(null!, null!),
    ) as UrlTree;

    expect(refus.toString()).toBe('/utilisateurs');
  });
});
