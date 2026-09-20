import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthSession } from '../core/auth.service';
import { SessionStore } from '../core/session.store';
import { provideTranslocoDeTest } from '../core/transloco.testing';
import { MfaPage } from './mfa.page';

/**
 * L'écran suit l'étape que le backend a nommée. Préparer une activation pour un
 * compte déjà inscrit régénérerait son secret et ses codes de secours en
 * silence : le téléphone de l'utilisateur ne fonctionnerait plus (benchmark B5).
 */
describe('MfaPage', () => {
  let preparations = 0;
  let verifications: string[] = [];
  let activations: string[] = [];

  beforeEach(async () => {
    preparations = 0;
    verifications = [];
    activations = [];

    await TestBed.configureTestingModule({
      imports: [MfaPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        ...provideTranslocoDeTest(),
        {
          provide: AuthSession,
          useValue: {
            preparerSecondFacteur: async () => {
              preparations += 1;
              return {
                otpauthUri: 'otpauth://totp/Clubify:essai?secret=ABCDEFGH',
                recoveryCodes: ['AAAA-1111', 'BBBB-2222'],
              };
            },
            activerSecondFacteur: async (code: string) => {
              activations.push(code);
            },
            verifierSecondFacteur: async (code: string) => {
              verifications.push(code);
            },
          },
        },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  const ouvrir = async (etape: 'ACTIVATION' | 'VERIFICATION') => {
    TestBed.inject(SessionStore).attendreSecondFacteur(etape);
    const fixture = TestBed.createComponent(MfaPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  };

  it("prépare l'activation quand le backend la demande", async () => {
    const fixture = await ouvrir('ACTIVATION');

    expect(preparations).toBe(1);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[data-testid="codes-de-secours"]'),
    ).not.toBeNull();
  });

  it("ne régénère rien quand une simple vérification est demandée", async () => {
    const fixture = await ouvrir('VERIFICATION');

    expect(preparations).toBe(0);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[data-testid="codes-de-secours"]'),
    ).toBeNull();
  });

  it('envoie le code à la vérification, et non à l\u2019activation', async () => {
    const fixture = await ouvrir('VERIFICATION');
    fixture.componentInstance['code'].set('123456');

    await fixture.componentInstance['soumettre']();

    expect(verifications).toEqual(['123456']);
    expect(activations).toEqual([]);
  });
});
