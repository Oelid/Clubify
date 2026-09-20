import { inject, Pipe, PipeTransform } from '@angular/core';
import { ClubContext } from '../club-context';

export type ClubDateFormat = 'date' | 'time' | 'datetime';

/** Une date pure (naissance, borne de saison) n'a pas d'heure. */
const DATE_PURE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Affiche un instant UTC dans le fuseau du club, ou une date pure telle quelle.
 *
 * <p>Une date de naissance ne subit aucune conversion : lui appliquer un fuseau
 * la ferait glisser d'un jour selon l'endroit où l'écran est ouvert.
 */
@Pipe({ name: 'clubDate' })
export class ClubDatePipe implements PipeTransform {
  private readonly club = inject(ClubContext);

  transform(value: string | null | undefined, format: ClubDateFormat): string {
    if (!value) {
      return '';
    }
    const reglages = this.club.get();
    const langue = reglages?.language ?? 'fr';

    if (DATE_PURE.test(value)) {
      const [annee, mois, jour] = value.split('-');
      return format === 'time' ? '' : `${jour}/${mois}/${annee}`;
    }

    const instant = new Date(value);
    const fuseau = reglages?.timezone ?? 'UTC';

    const date = new Intl.DateTimeFormat(langue, {
      timeZone: fuseau,
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
    const heure = new Intl.DateTimeFormat(langue, {
      timeZone: fuseau,
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });

    switch (format) {
      case 'date':
        return date.format(instant);
      case 'time':
        return heure.format(instant);
      default:
        return `${date.format(instant)} ${heure.format(instant)}`;
    }
  }
}
