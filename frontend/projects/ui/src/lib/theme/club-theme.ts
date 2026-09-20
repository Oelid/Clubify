import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/** Couleurs de marque d'un club, lues dans ses paramètres (ADM-01). */
export interface ClubBrand {
  readonly primary: string;
  readonly secondary?: string;
}

const TEXTE_CLAIR = '#ffffff';
const TEXTE_FONCE = '#1a1a18';

/** En deçà, assombrir davantage ne se verrait plus : le survol éclaircit. */
const SEUIL_MARQUE_SOMBRE = 0.06;

/**
 * Applique la marque du club par-dessus les jetons Clubify (décision 0025).
 *
 * <p>La couleur du texte posé sur la marque n'est jamais choisie à la main :
 * elle se calcule, parce qu'un club peut retenir une teinte pâle sur laquelle
 * du blanc deviendrait illisible. C'est l'accessibilité AA exigée par
 * `frontend/CLAUDE.md`, tenue sans dépendre du goût de celui qui configure.
 */
@Injectable({ providedIn: 'root' })
export class ClubTheme {
  private readonly document = inject(DOCUMENT);

  apply(brand: ClubBrand): void {
    const racine = this.document.documentElement;

    racine.style.setProperty('--brand-primary', brand.primary);
    racine.style.setProperty('--brand-primary-contrast', ClubTheme.texteLisibleSur(brand.primary));
    racine.style.setProperty('--brand-primary-hover', ClubTheme.teinteSurvol(brand.primary));
    racine.style.setProperty('--brand-primary-active', ClubTheme.teinteAppui(brand.primary));

    if (brand.secondary) {
      racine.style.setProperty('--brand-secondary', brand.secondary);
      racine.style.setProperty(
        '--brand-secondary-contrast',
        ClubTheme.texteLisibleSur(brand.secondary),
      );
    }
  }

  /** Revient aux jetons Clubify : utile à la déconnexion. */
  reset(): void {
    const racine = this.document.documentElement;
    for (const jeton of [
      '--brand-primary',
      '--brand-primary-contrast',
      '--brand-primary-hover',
      '--brand-primary-active',
      '--brand-secondary',
      '--brand-secondary-contrast',
    ]) {
      racine.style.removeProperty(jeton);
    }
  }

  /**
   * Teinte du survol d'un bouton plein : plus sombre, comme le veut l'usage.
   *
   * <p>Sauf lorsque la marque est déjà très sombre : l'assombrir encore ne se
   * verrait pas. Dans ce cas la teinte s'éclaircit, et le survol reste perçu.
   */
  static teinteSurvol(couleur: string): string {
    return ClubTheme.luminance(couleur) < SEUIL_MARQUE_SOMBRE
      ? ClubTheme.melanger(couleur, '#ffffff', 0.18)
      : ClubTheme.melanger(couleur, '#000000', 0.12);
  }

  /** Teinte de l'appui : un cran au-delà du survol, dans le même sens. */
  static teinteAppui(couleur: string): string {
    return ClubTheme.luminance(couleur) < SEUIL_MARQUE_SOMBRE
      ? ClubTheme.melanger(couleur, '#ffffff', 0.3)
      : ClubTheme.melanger(couleur, '#000000', 0.22);
  }

  /** Mélange deux couleurs, `part` étant la proportion de la seconde. */
  static melanger(a: string, b: string, part: number): string {
    const ca = ClubTheme.canaux(a);
    const cb = ClubTheme.canaux(b);
    const melange = ca.map((v, i) => Math.round(v * (1 - part) + cb[i] * part));
    return '#' + melange.map((v) => v.toString(16).padStart(2, '0')).join('');
  }

  /** Noir ou blanc, selon celui qui contraste le mieux avec le fond donné. */
  static texteLisibleSur(fond: string): string {
    return ClubTheme.contraste(fond, TEXTE_CLAIR) >= ClubTheme.contraste(fond, TEXTE_FONCE)
      ? TEXTE_CLAIR
      : TEXTE_FONCE;
  }

  /** Rapport de contraste WCAG entre deux couleurs, de 1 à 21. */
  static contraste(a: string, b: string): number {
    const [clair, sombre] = [ClubTheme.luminance(a), ClubTheme.luminance(b)].sort((x, y) => y - x);
    return (clair + 0.05) / (sombre + 0.05);
  }

  /** Luminance relative WCAG d'une couleur notée #rrggbb. */
  static luminance(couleur: string): number {
    const [r, v, b] = ClubTheme.canaux(couleur)
      .map((c) => c / 255)
      .map((c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
    return 0.2126 * r + 0.7152 * v + 0.0722 * b;
  }

  /** Les trois canaux d'une couleur notée #rrggbb, de 0 à 255. */
  private static canaux(couleur: string): number[] {
    const hex = couleur.replace('#', '');
    return [0, 2, 4].map((i) => parseInt(hex.slice(i, i + 2), 16));
  }
}
