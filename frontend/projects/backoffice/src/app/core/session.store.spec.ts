import { TestBed } from '@angular/core/testing';
import { CurrentUser } from 'api-client';
import { ClubContext } from 'ui';
import { SessionStore } from './session.store';

const utilisateurFictif = (surcharges: Partial<CurrentUser> = {}): CurrentUser => ({
  id: '00000000-0000-7000-8000-000000000001',
  email: 'accueil.a@exemple.test',
  firstName: 'Salma',
  lastName: 'Fictive',
  language: 'fr',
  role: 'FRONT_DESK',
  permissions: ['users.consulter'],
  club: {
    id: '00000000-0000-7000-8000-0000000000aa',
    name: 'Club A',
    timezone: 'Africa/Casablanca',
    currency: 'MAD',
  },
  ...surcharges,
});

/**
 * Le jeton d'accès ne vit qu'en mémoire (frontend/CLAUDE.md) : un jeton rangé
 * dans le stockage du navigateur est un jeton exfiltrable.
 */
describe('SessionStore', () => {
  let session: SessionStore;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    session = TestBed.inject(SessionStore);
    localStorage.clear();
    sessionStorage.clear();
  });

  it("ne range le jeton ni dans localStorage ni dans sessionStorage", () => {
    session.poserJeton('jeton-d-acces');

    expect(session.accessToken()).toBe('jeton-d-acces');
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('installe le fuseau et la devise du club, dont dépendent tous les affichages', () => {
    session.poserUtilisateur(utilisateurFictif());

    expect(TestBed.inject(ClubContext).get()).toEqual({
      currency: 'MAD',
      timezone: 'Africa/Casablanca',
      language: 'fr',
    });
  });

  it("applique la couleur du club quand il en a choisi une", () => {
    session.poserUtilisateur(
      utilisateurFictif({
        club: {
          id: '00000000-0000-7000-8000-0000000000aa',
          name: 'Club A',
          timezone: 'Africa/Casablanca',
          currency: 'MAD',
          brandPrimary: '#307890',
        },
      }),
    );

    expect(document.documentElement.style.getPropertyValue('--brand-primary')).toBe('#307890');
  });

  it("garde les jetons Clubify quand le club n'a pas choisi de couleur", () => {
    document.documentElement.style.setProperty('--brand-primary', '#307890');

    session.poserUtilisateur(utilisateurFictif());

    expect(document.documentElement.style.getPropertyValue('--brand-primary')).toBe('');
  });

  it('ne reconnaît que les droits que le backend a délivrés', () => {
    session.poserUtilisateur(utilisateurFictif());

    expect(session.permet('users.consulter')).toBe(true);
    expect(session.permet('club.settings.modifier')).toBe(false);
  });

  it('efface jeton, utilisateur et marque à la fermeture', () => {
    session.poserJeton('jeton-d-acces');
    session.poserUtilisateur(utilisateurFictif());

    session.vider();

    expect(session.accessToken()).toBeNull();
    expect(session.current()).toBeNull();
    expect(session.authentifie()).toBe(false);
  });
});
