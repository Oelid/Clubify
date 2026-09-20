import { Component, computed, inject, resource, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';
import { User, UsersApi } from 'api-client';
import { firstValueFrom } from 'rxjs';
import { ClubDatePipe } from 'ui';
import { codeDErreur } from '../core/api-errors';
import { SessionStore } from '../core/session.store';

/**
 * Lignes par page proposées. Cent est la borne que le backend impose : au-delà,
 * la page coûte cher au serveur comme au navigateur, et personne ne la lit.
 */
const PALIERS = [20, 30, 50, 100] as const;

/**
 * Liste des utilisateurs du club. Densité de tableau assumée : c'est l'écran le
 * plus proche des grilles que l'accueil connaît déjà (risque d'adoption, 10.I).
 */
@Component({
  selector: 'app-users',
  imports: [FormsModule, TranslocoDirective, ClubDatePipe],
  templateUrl: './users.page.html',
})
export class UsersPage {
  private readonly api = inject(UsersApi);
  private readonly session = inject(SessionStore);

  protected readonly paliers = PALIERS;
  protected readonly erreur = signal<string | null>(null);

  /** La liste s'ouvre à la taille que le club a retenue (règle `ui.page_size`). */
  private readonly criteres = signal({
    page: 0,
    taille: this.session.current()?.club?.pageSize ?? PALIERS[0],
  });

  protected readonly page = resource({
    params: () => this.criteres(),
    loader: async ({ params }) => {
      this.erreur.set(null);
      try {
        return await firstValueFrom(
          this.api.listUsers({ page: params.page, size: params.taille }),
        );
      } catch (echec) {
        this.erreur.set(codeDErreur(echec));
        return null;
      }
    },
  });

  protected readonly utilisateurs = computed<User[]>(() => this.page.value()?.content ?? []);
  protected readonly taille = computed(() => this.criteres().taille);
  protected readonly total = computed(() => this.page.value()?.page?.totalElements ?? 0);
  protected readonly pages = computed(() => this.page.value()?.page?.totalPages ?? 0);
  protected readonly numero = computed(() => (this.page.value()?.page?.page ?? 0) + 1);

  /** Premier rang affiché, tel qu'on le lit : « 21–40 sur 57 ». */
  protected readonly premier = computed(() =>
    this.total() === 0 ? 0 : (this.numero() - 1) * this.taille() + 1,
  );
  protected readonly dernier = computed(() =>
    Math.min(this.numero() * this.taille(), this.total()),
  );

  protected readonly desactives = computed(
    () => this.utilisateurs().filter((u) => !u.active).length,
  );

  protected readonly peutCreer = computed(() => this.session.permet('users.creer'));
  protected readonly peutExporter = computed(() => this.session.permet('users.exporter'));

  protected allerA(page: number): void {
    this.criteres.set({ ...this.criteres(), page: Math.max(0, page) });
  }

  /** Changer la taille ramène à la première page : sinon on saute dans le vide. */
  protected changerLaTaille(taille: number): void {
    this.criteres.set({ page: 0, taille: Number(taille) });
  }
}
