import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { ClubDatePipe } from 'ui';
import { AuthSession } from '../core/auth.service';
import { SessionStore } from '../core/session.store';

/**
 * Coquille du backoffice : navigation à gauche, identité du club en haut.
 * Densité inspirée du tableau de bord Stripe (décision 0025) : l'accueil a un
 * parent devant elle et cinq minutes.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslocoDirective, ClubDatePipe],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  private readonly session = inject(SessionStore);
  private readonly auth = inject(AuthSession);
  private readonly routeur = inject(Router);

  protected readonly club = computed(() => this.session.current()?.club ?? null);
  protected readonly nom = this.session.nomComplet;
  protected readonly initiales = this.session.initiales;
  protected readonly role = computed(() => this.session.current()?.role ?? null);

  /**
   * Rappel du second facteur, non masquable et présent sur chaque écran
   * (décision 0031). Le masquer une fois reviendrait à ne l'afficher jamais.
   */
  protected readonly rappel = this.session.rappelSecondFacteur;
  protected readonly exigeLe = this.session.secondFacteurExigeLe;

  /**
   * La navigation ne montre que ce que l'utilisateur a le droit d'ouvrir : un
   * écran inaccessible dans le menu est une promesse non tenue (décision 0028).
   */
  protected readonly sections = computed(() =>
    [
      {
        cle: 'nav.club',
        lien: '/club',
        droit: 'club.settings.consulter',
        icone: 'M12 15a3 3 0 100-6 3 3 0 000 6z',
      },
      {
        cle: 'nav.users',
        lien: '/utilisateurs',
        droit: 'users.consulter',
        icone: 'M17 20h5v-2a3 3 0 00-5.36-1.9M17 20H7',
      },
      {
        cle: 'nav.audit',
        lien: '/journal',
        droit: 'audit.consulter',
        icone: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5',
      },
    ].filter((section) => this.session.permet(section.droit)),
  );

  protected async deconnecter(): Promise<void> {
    await this.auth.deconnecter();
  }

  /** Conduit à l'activation, depuis l'intérieur de l'application. */
  protected async activerLeSecondFacteur(): Promise<void> {
    this.session.attendreSecondFacteur('ACTIVATION');
    await this.routeur.navigate(['/second-facteur']);
  }
}
