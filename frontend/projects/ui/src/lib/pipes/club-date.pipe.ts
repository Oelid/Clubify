import { Pipe, PipeTransform } from '@angular/core';

export type ClubDateFormat = 'date' | 'time' | 'datetime';

/**
 * Affiche un instant UTC dans le fuseau du club, ou une date pure telle quelle.
 * À implémenter à l'étape 5 ; les tests décrivent le comportement attendu.
 */
@Pipe({ name: 'clubDate' })
export class ClubDatePipe implements PipeTransform {
  transform(_value: string | null | undefined, _format: ClubDateFormat): string {
    throw new Error('ClubDatePipe : à implémenter (étape 5)');
  }
}
