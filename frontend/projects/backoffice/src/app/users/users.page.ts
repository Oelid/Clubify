import { Component, computed, inject, resource, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';
import { ExportsApi, Role, RoleDefinition, User, UsersApi } from 'api-client';
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
  private readonly exports = inject(ExportsApi);
  private readonly session = inject(SessionStore);

  protected readonly paliers = PALIERS;
  protected readonly erreur = signal<string | null>(null);

  /** La liste s'ouvre à la taille que le club a retenue (règle `ui.page_size`). */
  protected readonly criteres = signal({
    page: 0,
    taille: this.session.current()?.club?.pageSize ?? PALIERS[0],
    role: '' as '' | Role,
    actif: '' as '' | 'true' | 'false',
    recherche: '',
  });

  protected readonly page = resource({
    params: () => this.criteres(),
    loader: async ({ params }) => {
      this.erreur.set(null);
      try {
        return await firstValueFrom(
          this.api.listUsers({
            page: params.page,
            size: params.taille,
            role: params.role || undefined,
            active: params.actif === '' ? undefined : params.actif === 'true',
            search: params.recherche || undefined,
          }),
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

  // ------------------------------------------------- création d'un compte

  protected readonly formulaireOuvert = signal(false);
  protected readonly enCours = signal(false);
  protected readonly succes = signal<string | null>(null);

  /** Champs saisis. Le mot de passe ne quitte jamais ce formulaire. */
  protected readonly nouveau = signal({
    email: '',
    firstName: '',
    lastName: '',
    phone: '',
    role: 'FRONT_DESK' as Role,
    password: '',
  });

  /**
   * Rôles proposés, tels que le backend les déclare.
   *
   * <p>Les recopier ici ferait diverger l'écran le jour où le coach recevra son
   * application (R4) : c'est une règle métier, elle vit d'un seul côté.
   */
  protected readonly roles = resource({
    loader: async () => {
      try {
        const tous = await firstValueFrom(this.api.listRoles());
        return tous.filter((role: RoleDefinition) => role.assignable);
      } catch {
        return [] as RoleDefinition[];
      }
    },
  });

  protected champ<K extends keyof ReturnType<typeof this.nouveau>>(
    nom: K,
    valeur: string,
  ): void {
    this.nouveau.set({ ...this.nouveau(), [nom]: valeur });
  }

  protected ouvrirLeFormulaire(): void {
    this.succes.set(null);
    this.erreur.set(null);
    this.formulaireOuvert.set(true);
  }

  protected fermerLeFormulaire(): void {
    this.formulaireOuvert.set(false);
    this.nouveau.set({
      email: '',
      firstName: '',
      lastName: '',
      phone: '',
      role: 'FRONT_DESK' as Role,
      password: '',
    });
  }

  protected async creer(): Promise<void> {
    if (this.enCours()) {
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    try {
      const saisi = this.nouveau();
      const cree = await firstValueFrom(
        this.api.createUser({
          userCreateRequest: {
            email: saisi.email,
            firstName: saisi.firstName,
            lastName: saisi.lastName,
            phone: saisi.phone || undefined,
            role: saisi.role,
            password: saisi.password,
          },
        }),
      );
      this.succes.set(`${cree.firstName} ${cree.lastName}`);
      this.fermerLeFormulaire();
      // La liste repart de la première page : le nouveau compte s'y trouve.
      this.criteres.set({ ...this.criteres(), page: 0 });
      this.page.reload();
    } catch (echec) {
      this.erreur.set(codeDErreur(echec));
    } finally {
      this.enCours.set(false);
    }
  }

  // --------------------------------------------------------------- export

  protected readonly exportEnCours = signal(false);

  /**
   * Télécharge la liste. Le fichier arrive en flux : il est rendu au navigateur
   * par un lien éphémère, faute de quoi le jeton d'authentification devrait
   * voyager dans une adresse, ce qu'on ne fait pas (frontend/CLAUDE.md).
   */
  protected async exporter(format: 'CSV' | 'XLSX'): Promise<void> {
    if (this.exportEnCours()) {
      return;
    }
    this.exportEnCours.set(true);
    this.erreur.set(null);
    try {
      const fichier = await firstValueFrom(
        this.exports.createExport({ exportRequest: { dataset: 'users', format } }),
      );
      this.telecharger(fichier, `utilisateurs.${format === 'CSV' ? 'csv' : 'xlsx'}`);
    } catch (echec) {
      this.erreur.set(codeDErreur(echec));
    } finally {
      this.exportEnCours.set(false);
    }
  }

  private telecharger(contenu: Blob, nom: string): void {
    const adresse = URL.createObjectURL(contenu);
    const lien = document.createElement('a');
    lien.href = adresse;
    lien.download = nom;
    lien.click();
    URL.revokeObjectURL(adresse);
  }

  // --------------------------------------------------------------- filtres

  /** Tous les rôles, pour filtrer — y compris ceux qu'on n'attribue plus. */
  protected readonly tousLesRoles = resource({
    loader: async () => {
      try {
        return await firstValueFrom(this.api.listRoles());
      } catch {
        return [] as RoleDefinition[];
      }
    },
  });

  /** Un filtre change la population : on repart de la première page. */
  protected filtrer(champ: 'role' | 'actif' | 'recherche', valeur: string): void {
    this.criteres.set({ ...this.criteres(), page: 0, [champ]: valeur });
  }

  protected reinitialiserLesFiltres(): void {
    this.criteres.set({ ...this.criteres(), page: 0, role: '', actif: '', recherche: '' });
  }

  protected readonly filtreActif = computed(() => {
    const c = this.criteres();
    return c.role !== '' || c.actif !== '' || c.recherche !== '';
  });

  protected allerA(page: number): void {
    this.criteres.set({ ...this.criteres(), page: Math.max(0, page) });
  }

  /** Changer la taille ramène à la première page : sinon on saute dans le vide. */
  protected changerLaTaille(taille: number): void {
    this.criteres.set({ ...this.criteres(), page: 0, taille: Number(taille) });
  }
}
