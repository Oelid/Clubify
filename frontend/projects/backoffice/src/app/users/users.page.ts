import { Component, computed, inject, resource, signal } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { User, UsersApi } from 'api-client';
import { firstValueFrom } from 'rxjs';
import { ClubDatePipe } from 'ui';
import { codeDErreur } from '../core/api-errors';
import { SessionStore } from '../core/session.store';

/**
 * Liste des utilisateurs du club. Densité de tableau assumée : c'est l'écran le
 * plus proche des grilles que l'accueil connaît déjà (risque d'adoption, 10.I).
 */
@Component({
  selector: 'app-users',
  imports: [TranslocoDirective, ClubDatePipe],
  templateUrl: './users.page.html',
  styleUrl: '../shared/page.css',
})
export class UsersPage {
  private readonly api = inject(UsersApi);
  private readonly session = inject(SessionStore);

  protected readonly erreur = signal<string | null>(null);

  protected readonly page = resource({
    loader: async () => {
      this.erreur.set(null);
      try {
        return await firstValueFrom(this.api.listUsers({ page: 0, size: 50 }));
      } catch (echec) {
        this.erreur.set(codeDErreur(echec));
        return null;
      }
    },
  });

  protected readonly utilisateurs = computed<User[]>(() => this.page.value()?.content ?? []);
  protected readonly desactives = computed(
    () => this.utilisateurs().filter((u) => !u.active).length,
  );

  protected readonly peutCreer = computed(() => this.session.permet('users.creer'));
  protected readonly peutExporter = computed(() => this.session.permet('users.exporter'));
}
