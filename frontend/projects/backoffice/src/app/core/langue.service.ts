import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

/** Langues écrites de droite à gauche (PLT-08). */
const DROITE_A_GAUCHE = new Set(['ar']);

/**
 * Langue de l'interface et direction du document.
 *
 * <p>La direction suit la langue, sans qu'aucun écran ait à le savoir : les
 * feuilles de style n'emploient que des propriétés logiques (décision 0025).
 */
@Injectable({ providedIn: 'root' })
export class LangueService {
  private readonly document = inject(DOCUMENT);
  private readonly transloco = inject(TranslocoService);

  appliquer(langue: string): void {
    const retenue = this.transloco.getAvailableLangs().some((l) =>
      (typeof l === 'string' ? l : l.id) === langue,
    )
      ? langue
      : this.transloco.getDefaultLang();

    this.transloco.setActiveLang(retenue);
    this.document.documentElement.lang = retenue;
    this.document.documentElement.dir = DROITE_A_GAUCHE.has(retenue) ? 'rtl' : 'ltr';
  }
}
