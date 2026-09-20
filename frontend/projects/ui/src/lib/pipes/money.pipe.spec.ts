import { TestBed } from '@angular/core/testing';
import { MoneyPipe } from './money.pipe';
import { ClubContext } from '../club-context';

/**
 * Les montants arrivent en centimes avec leur devise (invariant du CLAUDE.md).
 * Aucun composant ne les formate à la main : ce pipe est le seul chemin.
 */
/**
 * Le séparateur de milliers français est une espace fine insécable : elle doit
 * le rester, sans quoi un montant peut se couper en fin de ligne. Les tests
 * comparent donc à espace près, jamais au code de l'espace, qui appartient à
 * la version d'ICU du moteur.
 */
const lisible = (rendu: string): string => rendu.replace(/[  ]/g, ' ');

describe('MoneyPipe', () => {
  let pipe: MoneyPipe;
  let contexte: ClubContext;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [MoneyPipe, ClubContext] });
    pipe = TestBed.inject(MoneyPipe);
    contexte = TestBed.inject(ClubContext);
    contexte.set({ currency: 'MAD', timezone: 'Africa/Casablanca', language: 'fr' });
  });

  it('formate des centimes dans la devise du club', () => {
    expect(lisible(pipe.transform({ amountCents: 350000, currency: 'MAD' })))
      .toContain('3 500,00');
  });

  it('ne perd jamais les centimes', () => {
    expect(pipe.transform({ amountCents: 1, currency: 'MAD' })).toContain('0,01');
    expect(pipe.transform({ amountCents: 199, currency: 'MAD' })).toContain('1,99');
  });

  it('rend un montant négatif lisible', () => {
    expect(pipe.transform({ amountCents: -5000, currency: 'MAD' })).toContain('-50,00');
  });

  it('respecte la devise portée par le montant plutôt que celle du club', () => {
    expect(pipe.transform({ amountCents: 1000, currency: 'EUR' })).toContain('€');
  });

  it('rend une chaîne vide plutôt que « NaN » pour une valeur absente', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
