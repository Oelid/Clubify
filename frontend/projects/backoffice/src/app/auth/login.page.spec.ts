import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthSession } from '../core/auth.service';
import { SessionStore } from '../core/session.store';
import { provideTranslocoDeTest } from '../core/transloco.testing';
import { LoginPage } from './login.page';

/**
 * L'écran ne décide pas si un second facteur est requis : il suit l'issue que le
 * backend renvoie (décision 0027). C'est ce qu'on vérifie ici.
 */
describe('LoginPage', () => {
  let router: Router;
  let issue: 'AUTHENTICATED' | 'MFA_REQUIRED' | 'MFA_ENROLLMENT_REQUIRED';
  let echec: unknown;

  beforeEach(async () => {
    issue = 'AUTHENTICATED';
    echec = null;

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        ...provideTranslocoDeTest(),
        {
          provide: AuthSession,
          useValue: {
            connecter: async () => {
              if (echec) {
                throw echec;
              }
              return issue;
            },
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  const soumettre = async () => {
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(
      new Event('submit'),
    );
    await fixture.whenStable();
    return fixture;
  };

  it("ouvre le premier écran que le rôle permet", async () => {
    // Le gérant lit les paramètres du club : c'est là qu'il arrive.
    TestBed.inject(SessionStore).poserUtilisateur({
      id: '00000000-0000-7000-8000-000000000001',
      email: 'gerant@exemple.test',
      firstName: 'Recette',
      lastName: 'Gérant',
      role: 'MANAGER',
      permissions: ['club.settings.consulter'],
      club: {
        id: '00000000-0000-7000-8000-0000000000aa',
        name: 'Club A',
        timezone: 'Africa/Casablanca',
        currency: 'MAD',
      },
    });

    await soumettre();

    expect(router.navigate).toHaveBeenCalledWith(['/club']);
  });

  it('conduit au second facteur quand le backend l\u2019exige', async () => {
    issue = 'MFA_REQUIRED';

    await soumettre();

    expect(router.navigate).toHaveBeenCalledWith(['/second-facteur']);
  });

  it('affiche le code d\u2019erreur du backend, jamais un libellé en dur', async () => {
    const { HttpErrorResponse } = await import('@angular/common/http');
    echec = new HttpErrorResponse({
      status: 401,
      error: { code: 'auth.credentials.invalid' },
    });

    const fixture = await soumettre();
    fixture.detectChanges();

    expect(router.navigate).not.toHaveBeenCalled();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[data-testid="erreur"]'),
    ).not.toBeNull();
  });
});
