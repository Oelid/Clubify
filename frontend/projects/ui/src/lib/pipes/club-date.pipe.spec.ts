import { TestBed } from '@angular/core/testing';
import { ClubDatePipe } from './club-date.pipe';
import { ClubContext } from '../club-context';

/**
 * Les instants arrivent en UTC ; le fuseau du club sert à l'affichage
 * (invariant du CLAUDE.md, critère C29 côté interface).
 */
describe('ClubDatePipe', () => {
  let pipe: ClubDatePipe;
  let contexte: ClubContext;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ClubDatePipe, ClubContext] });
    pipe = TestBed.inject(ClubDatePipe);
    contexte = TestBed.inject(ClubContext);
    contexte.set({ currency: 'MAD', timezone: 'Africa/Casablanca', language: 'fr' });
  });

  it('affiche un instant UTC dans le fuseau du club', () => {
    // Casablanca est à UTC+1 : 9 h UTC vaut 10 h locale.
    expect(pipe.transform('2027-01-15T09:00:00Z', 'time')).toBe('10:00');
  });

  it('affiche une date au format français', () => {
    expect(pipe.transform('2027-01-15T09:00:00Z', 'date')).toBe('15/01/2027');
  });

  it('suit le fuseau du club quand celui-ci change', () => {
    contexte.set({ currency: 'MAD', timezone: 'Europe/Paris', language: 'fr' });
    // Paris est à UTC+1 en janvier également, mais à UTC+2 en juillet.
    expect(pipe.transform('2027-07-15T09:00:00Z', 'time')).toBe('11:00');
  });

  it('traite une date pure sans lui appliquer de fuseau', () => {
    // Une date de naissance n'a pas d'heure : elle ne doit jamais glisser d'un jour.
    expect(pipe.transform('2018-03-01', 'date')).toBe('01/03/2018');
  });

  it('rend une chaîne vide pour une valeur absente', () => {
    expect(pipe.transform(null, 'date')).toBe('');
  });
});
