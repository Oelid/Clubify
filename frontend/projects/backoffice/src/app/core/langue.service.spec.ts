import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LangueService } from './langue.service';
import { provideTranslocoDeTest } from './transloco.testing';

/**
 * La direction du document suit la langue (PLT-08) : c'est le seul endroit qui
 * la décide, et aucun écran n'a à s'en soucier.
 */
describe('LangueService', () => {
  let langue: LangueService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ...provideTranslocoDeTest(['fr', 'ar']),
      ],
    });
    langue = TestBed.inject(LangueService);
  });

  it('pose le français de gauche à droite', () => {
    langue.appliquer('fr');

    expect(document.documentElement.lang).toBe('fr');
    expect(document.documentElement.dir).toBe('ltr');
  });

  it('bascule la direction pour l\u2019arabe', () => {
    langue.appliquer('ar');

    expect(document.documentElement.lang).toBe('ar');
    expect(document.documentElement.dir).toBe('rtl');
  });

  it('retombe sur la langue par défaut plutôt que sur une langue absente', () => {
    langue.appliquer('de');

    expect(document.documentElement.lang).toBe('fr');
    expect(document.documentElement.dir).toBe('ltr');
  });
});
