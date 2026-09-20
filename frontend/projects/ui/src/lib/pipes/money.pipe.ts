import { Pipe, PipeTransform } from '@angular/core';

/** Montant en centimes avec sa devise : jamais un nombre à virgule. */
export interface Money {
  readonly amountCents: number;
  readonly currency: string;
}

/**
 * Seul chemin de formatage d'un montant (frontend/CLAUDE.md).
 * À implémenter à l'étape 5 ; les tests décrivent le comportement attendu.
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(_value: Money | null | undefined): string {
    throw new Error('MoneyPipe : à implémenter (étape 5)');
  }
}
