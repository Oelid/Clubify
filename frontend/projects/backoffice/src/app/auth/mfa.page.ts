import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { QRCodeComponent } from 'angularx-qrcode';
import { codeDErreur } from '../core/api-errors';
import { AuthSession } from '../core/auth.service';
import { SessionStore } from '../core/session.store';

/**
 * Second facteur (décision 0027). Deux cas sur le même écran, parce que
 * l'utilisateur ne fait la différence qu'une fois : activer, ou vérifier.
 *
 * <p>Les codes de secours sont affichés une seule fois, à l'activation : sans
 * eux, un téléphone perdu ferme le club (benchmark B5).
 */
@Component({
  selector: 'app-mfa',
  imports: [FormsModule, NgTemplateOutlet, TranslocoDirective, QRCodeComponent],
  templateUrl: './mfa.page.html',
})
export class MfaPage {
  private readonly auth = inject(AuthSession);
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);

  protected readonly otpauthUri = signal<string | null>(null);
  protected readonly codesDeSecours = signal<string[]>([]);
  protected readonly code = signal('');
  protected readonly appareilDeConfiance = signal(false);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<string | null>(null);

  /**
   * Activation en cours : l'écran porte alors le QR et les codes de secours.
   * L'étape vient du backend ; la deviner reviendrait à régénérer le secret
   * d'un compte déjà inscrit.
   */
  protected readonly activation = computed(() => this.session.etape() === 'ACTIVATION');

  constructor() {
    void this.preparer();
  }

  protected async soumettre(): Promise<void> {
    if (this.enCours()) {
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    try {
      if (this.activation()) {
        await this.auth.activerSecondFacteur(this.code());
      } else {
        await this.auth.verifierSecondFacteur(this.code(), this.appareilDeConfiance());
      }
      await this.router.navigate(['/club']);
    } catch (echec) {
      this.erreur.set(codeDErreur(echec));
      this.code.set('');
    } finally {
      this.enCours.set(false);
    }
  }

  /**
   * Prépare l'activation quand le compte n'a pas encore de second facteur. Une
   * simple vérification n'a rien à préparer : le défi est déjà ouvert.
   */
  private async preparer(): Promise<void> {
    if (!this.activation()) {
      return;
    }
    try {
      const preparation = await this.auth.preparerSecondFacteur();
      this.otpauthUri.set(preparation.otpauthUri);
      this.codesDeSecours.set(preparation.recoveryCodes ?? []);
    } catch (echec) {
      this.erreur.set(codeDErreur(echec));
    }
  }
}
