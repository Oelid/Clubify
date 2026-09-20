import { inject, Pipe, PipeTransform } from '@angular/core';
import { ClubContext } from '../club-context';

/** Montant en centimes avec sa devise : jamais un nombre à virgule. */
export interface Money {
  readonly amountCents: number;
  readonly currency: string;
}

/**
 * Seul chemin de formatage d'un montant (frontend/CLAUDE.md).
 *
 * <p>La devise vient du montant lui-même, jamais du club : un club encaisse en
 * dirhams et peut afficher un tarif de camp libellé autrement. Celle du club ne
 * sert que de repli.
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private readonly club = inject(ClubContext);

  transform(value: Money | null | undefined): string {
    if (value === null || value === undefined || value.amountCents === null) {
      return '';
    }
    const reglages = this.club.get();
    return new Intl.NumberFormat(reglages?.language ?? 'fr', {
      style: 'currency',
      currency: value.currency ?? reglages?.currency ?? 'MAD',
    }).format(value.amountCents / 100);
  }
}
