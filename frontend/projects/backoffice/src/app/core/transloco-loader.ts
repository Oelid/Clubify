import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Translation, TranslocoLoader } from '@jsverse/transloco';

/** Charge `public/i18n/<lang>.json`. Aucun libellé n'est en dur dans le code. */
@Injectable({ providedIn: 'root' })
export class HttpTranslocoLoader implements TranslocoLoader {
  private readonly http = inject(HttpClient);

  getTranslation(langue: string) {
    return this.http.get<Translation>(`/i18n/${langue}.json`);
  }
}
