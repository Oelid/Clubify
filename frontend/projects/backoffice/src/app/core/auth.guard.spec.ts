import { TestBed } from '@angular/core/testing';
import { CurrentUser } from 'api-client';
import { provideRouter, UrlTree } from '@angular/router';
import { exigeDroit, sessionOuverte } from './auth.guard';
import { SessionStore } from './session.store';

/**
 * La garde n'est qu'une commodité d'écran : le backend refuse de son côté
 * (décision 0028). Elle évite d'ouvrir une page que l'utilisateur ne peut pas
 * remplir.
 */
const utilisateurAvec = (permissions: string[]): CurrentUser => ({
  id: '00000000-0000-7000-8000-000000000001',
  email: 'essai@exemple.test',
  firstName: 'Recette',
  lastName: 'Essai',
  role: 'COACH',
  permissions,
  club: {
    id: '00000000-0000-7000-8000-0000000000aa',
    name: 'Club A',
    timezone: 'Africa/Casablanca',
    currency: 'MAD',
  },
});

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
    session.attendreSecondFacteur('VERIFICATION');

    expect((executer(sessionOuverte) as UrlTree).toString()).toBe('/second-facteur');
  });

  it('laisse passer une session complète', () => {
    session.poserJeton('jeton-d-acces');

    expect(executer(sessionOuverte)).toBe(true);
  });

  it("détourne vers le premier écran que l'utilisateur peut ouvrir", () => {
    session.poserUtilisateur(utilisateurAvec(['users.consulter']));

    const refus = TestBed.runInInjectionContext(() =>
      exigeDroit('audit.consulter')(null!, null!),
    ) as UrlTree;

    expect(refus.toString()).toBe('/utilisateurs');
  });

  it("dit franchement à un rôle sans droit qu'aucun écran ne lui est ouvert", () => {
    // Le coach en est le cas : son application arrive en R4. Le renvoyer d'un
    // écran à l'autre le ferait tourner en rond.
    session.poserUtilisateur(utilisateurAvec([]));

    const refus = TestBed.runInInjectionContext(() =>
      exigeDroit('club.settings.consulter')(null!, null!),
    ) as UrlTree;

    expect(refus.toString()).toBe('/sans-acces');
  });
});
