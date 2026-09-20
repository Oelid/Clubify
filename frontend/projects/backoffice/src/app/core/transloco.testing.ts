import { Provider } from '@angular/core';
import { provideTransloco, Translation, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import libelles from '../../../public/i18n/fr.json';

/**
 * Transloco dans les tests, avec les vrais libellés FR chargés depuis le fichier
 * livré. Un test qui invente ses propres libellés ne dirait rien de l'écran
 * réel : une clé manquante doit se voir ici, pas en production.
 */
class LibellesEnMemoire implements TranslocoLoader {
  getTranslation() {
    return of(libelles as unknown as Translation);
  }
}

export const provideTranslocoDeTest = (langues = ['fr']): Provider[] => [
  provideTransloco({
    config: {
      availableLangs: langues,
      defaultLang: 'fr',
      fallbackLang: 'fr',
      prodMode: true,
    },
    loader: LibellesEnMemoire,
  }),
];
