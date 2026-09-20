import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslocoDeTest } from './core/transloco.testing';
import { App } from './app';

/**
 * La coquille de l'application ne porte aucun contenu : elle accueille les
 * écrans de F01. Elle tient la langue et la direction, sur lesquelles repose
 * tout le droite-à-gauche (PLT-08).
 */
describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        ...provideTranslocoDeTest(),
      ],
    }).compileComponents();
  });

  it('se construit', () => {
    const fixture = TestBed.createComponent(App);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('expose une sortie de routeur où se rendent les écrans', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('router-outlet')).toBeTruthy();
  });

  it('pose la langue et la direction sur le document', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    // FR avant connexion ; l'arabe basculera `dir` sans toucher aux écrans (0025).
    expect(document.documentElement.lang).toBe('fr');
    expect(document.documentElement.dir).toBe('ltr');
  });
});
