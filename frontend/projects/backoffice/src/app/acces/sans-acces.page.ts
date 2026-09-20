import { Component, inject } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { AuthSession } from '../core/auth.service';
import { SessionStore } from '../core/session.store';

/**
 * Ce qu'on montre à quelqu'un dont le rôle n'ouvre encore aucun écran.
 *
 * <p>Le coach en est le cas : son rôle existe au modèle, son application arrive
 * en R4. Le renvoyer d'un écran à l'autre le ferait tourner en rond ; lui dire
 * franchement lui évite d'appeler le club en pensant s'être trompé.
 */
@Component({
  selector: 'app-sans-acces',
  imports: [TranslocoDirective],
  template: `
    <div class="auth" *transloco="let t">
      <div class="auth__carte">
        <div class="auth__marque">
          <span class="auth__logo">C</span>
          <span>{{ t('app.name') }}</span>
        </div>
        <h1 class="auth__titre">{{ t('access.none.title') }}</h1>
        <p class="auth__sous-titre">{{ t('access.none.text', { role: role() }) }}</p>
        <button class="bouton bouton--primaire" type="button" (click)="deconnecter()">
          {{ t('app.signOut') }}
        </button>
      </div>
    </div>
  `,
})
export class SansAccesPage {
  private readonly session = inject(SessionStore);
  private readonly auth = inject(AuthSession);

  protected role(): string {
    return this.session.current()?.role ?? '';
  }

  protected async deconnecter(): Promise<void> {
    await this.auth.deconnecter();
  }
}
