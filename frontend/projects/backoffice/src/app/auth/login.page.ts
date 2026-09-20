import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { codeDErreur } from '../core/api-errors';
import { AuthSession } from '../core/auth.service';

/**
 * Connexion du staff (SEC-01). Pensée clavier : la mise au point part sur le
 * courriel, la tabulation suit l'ordre visuel, Entrée valide.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, TranslocoDirective],
  templateUrl: './login.page.html',
})
export class LoginPage {
  private readonly auth = inject(AuthSession);
  private readonly router = inject(Router);

  protected readonly email = signal('');
  protected readonly motDePasse = signal('');
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected async soumettre(): Promise<void> {
    if (this.enCours()) {
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    try {
      const issue = await this.auth.connecter(this.email(), this.motDePasse());
      // Le backend dit s'il faut un second facteur : l'écran ne le devine pas.
      await this.router.navigate([
        issue === 'AUTHENTICATED' ? '/club' : '/second-facteur',
      ]);
    } catch (echec) {
      this.erreur.set(codeDErreur(echec));
    } finally {
      this.enCours.set(false);
    }
  }
}
