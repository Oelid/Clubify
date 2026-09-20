import {
  ApplicationConfig,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
  inject,
  isDevMode,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideTransloco } from '@jsverse/transloco';
import { providePrimeNG } from 'primeng/config';
import { ClubifyPreset } from 'ui';
import { authInterceptor } from './core/auth.interceptor';
import { AuthSession } from './core/auth.service';
import { HttpTranslocoLoader } from './core/transloco-loader';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    provideTransloco({
      config: {
        // FR livré ; AR et EN suivront sans toucher aux écrans (PLT-08).
        availableLangs: ['fr'],
        defaultLang: 'fr',
        fallbackLang: 'fr',
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: HttpTranslocoLoader,
    }),
    providePrimeNG({
      theme: { preset: ClubifyPreset, options: { darkModeSelector: false } },
      ripple: false,
    }),
    // Le jeton d'accès ne survit pas au rechargement ; le cookie HttpOnly, si.
    // On tente donc de reprendre la session avant d'afficher quoi que ce soit.
    provideAppInitializer(() => inject(AuthSession).reprendre()),
  ],
};
