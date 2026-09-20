import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';
import { SessionStore } from './session.store';

/**
 * Un seul intercepteur porte le jeton, la langue et le cookie
 * (frontend/CLAUDE.md) : autant d'endroits où l'oublier sinon.
 */
describe('authInterceptor', () => {
  let http: HttpClient;
  let serveur: HttpTestingController;
  let session: SessionStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    serveur = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionStore);
  });

  afterEach(() => serveur.verify());

  it('porte le jeton et la langue sur une demande authentifiée', () => {
    session.poserJeton('jeton-d-acces');

    http.get('/api/v1/users').subscribe();

    const demande = serveur.expectOne('/api/v1/users');
    expect(demande.request.headers.get('Authorization')).toBe('Bearer jeton-d-acces');
    expect(demande.request.headers.get('Accept-Language')).toBe('fr');
    demande.flush({});
  });

  it("n'envoie aucun en-tête d'autorisation tant qu'il n'y a pas de session", () => {
    http.post('/api/v1/auth/login', {}).subscribe();

    const demande = serveur.expectOne('/api/v1/auth/login');
    expect(demande.request.headers.has('Authorization')).toBe(false);
    demande.flush({});
  });

  it("n'envoie le cookie que sur les points d'authentification", () => {
    http.post('/api/v1/auth/refresh', {}).subscribe();
    const authentification = serveur.expectOne('/api/v1/auth/refresh');
    expect(authentification.request.withCredentials).toBe(true);
    authentification.flush({});

    http.get('/api/v1/audit').subscribe();
    const metier = serveur.expectOne('/api/v1/audit');
    expect(metier.request.withCredentials).toBe(false);
    metier.flush({});
  });

  it('ferme la session quand le serveur refuse le jeton', () => {
    session.poserJeton('jeton-perime');

    http.get('/api/v1/users').subscribe({ error: () => undefined });
    serveur.expectOne('/api/v1/users').flush(
      { code: 'auth.refresh.invalid' },
      { status: 401, statusText: 'Unauthorized' },
    );

    // L'utilisateur rouvre sa session lui-même : aucune boucle silencieuse.
    expect(session.authentifie()).toBe(false);
  });
});
