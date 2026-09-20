import { TestBed } from '@angular/core/testing';
import { ClubTheme } from './club-theme';

/**
 * La marque d'un club ne doit jamais rendre un bouton illisible. Ces cas fixent
 * la règle pour tous les clubs à venir, pas seulement pour le pilote.
 */
describe('ClubTheme', () => {
  let theme: ClubTheme;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    theme = TestBed.inject(ClubTheme);
    theme.reset();
  });

  it('pose la couleur du club sur les jetons de marque', () => {
    theme.apply({ primary: '#307890', secondary: '#f08840' });

    const style = document.documentElement.style;
    expect(style.getPropertyValue('--brand-primary')).toBe('#307890');
    expect(style.getPropertyValue('--brand-secondary')).toBe('#f08840');
  });

  it('choisit du texte clair sur une marque sombre', () => {
    // Bleu acier du club pilote : 4,98:1 avec du blanc, donc conforme AA.
    expect(ClubTheme.texteLisibleSur('#307890')).toBe('#ffffff');
  });

  it('choisit du texte foncé sur une marque claire', () => {
    // L'orange du club n'atteint que 2,53:1 avec du blanc : le texte doit être foncé.
    expect(ClubTheme.texteLisibleSur('#f08840')).toBe('#1a1a18');
  });

  it('garantit AA sur la couleur retenue, quelle que soit la marque', () => {
    for (const marque of ['#307890', '#f08840', '#ffe066', '#0b1f2a', '#c2185b', '#8bc34a']) {
      const texte = ClubTheme.texteLisibleSur(marque);

      expect(
        ClubTheme.contraste(marque, texte),
        `contraste de ${marque} sur ${texte}`,
      ).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('calcule le contraste comme le prévoit WCAG', () => {
    // Bornes connues : noir sur blanc vaut 21, une couleur sur elle-même vaut 1.
    expect(ClubTheme.contraste('#000000', '#ffffff')).toBeCloseTo(21, 1);
    expect(ClubTheme.contraste('#307890', '#307890')).toBeCloseTo(1, 5);
  });

  it('assombrit la marque au survol et davantage à l’appui', () => {
    const marque = '#307890';

    const survol = ClubTheme.teinteSurvol(marque);
    const appui = ClubTheme.teinteAppui(marque);

    // Un bouton plein s'assombrit quand on le survole : baisser son opacité
    // l'éclaircirait, puisqu'il se mélangerait au fond blanc.
    expect(ClubTheme.luminance(survol)).toBeLessThan(ClubTheme.luminance(marque));
    expect(ClubTheme.luminance(appui)).toBeLessThan(ClubTheme.luminance(survol));
  });

  it('éclaircit au survol lorsque la marque est déjà très sombre', () => {
    // Presque noire : l'assombrir encore ne se verrait pas.
    const marque = '#0b1116';

    const survol = ClubTheme.teinteSurvol(marque);

    expect(ClubTheme.luminance(survol)).toBeGreaterThan(ClubTheme.luminance(marque));
  });

  it('garde un survol perceptible et lisible, quelle que soit la marque', () => {
    for (const marque of ['#307890', '#f08840', '#ffe066', '#0b1116', '#c2185b', '#8bc34a']) {
      const survol = ClubTheme.teinteSurvol(marque);
      const texte = ClubTheme.texteLisibleSur(marque);

      // Perceptible : le survol se distingue de l'état au repos.
      expect(
        Math.abs(ClubTheme.luminance(survol) - ClubTheme.luminance(marque)),
        `écart de luminance sur ${marque}`,
      ).toBeGreaterThan(0.01);

      // Lisible : le texte choisi pour l'état au repos tient aussi au survol.
      expect(
        ClubTheme.contraste(survol, texte),
        `contraste du survol de ${marque}`,
      ).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('pose aussi les teintes de survol et d’appui sur les jetons', () => {
    theme.apply({ primary: '#307890' });

    const style = document.documentElement.style;
    expect(style.getPropertyValue('--brand-primary-hover')).toBe('#2a6a7f');
    expect(style.getPropertyValue('--brand-primary-active')).toBe('#255e70');
  });

  it('revient aux jetons Clubify à la déconnexion', () => {
    theme.apply({ primary: '#307890' });
    theme.reset();

    expect(document.documentElement.style.getPropertyValue('--brand-primary')).toBe('');
    expect(document.documentElement.style.getPropertyValue('--brand-primary-hover')).toBe('');
  });
});
