import { Component, computed, inject, resource, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';
import { AuditEntry, AuditApi } from 'api-client';
import { firstValueFrom } from 'rxjs';
import { ClubDatePipe } from 'ui';
import { codeDErreur } from '../core/api-errors';

/**
 * Journal d'audit (SEC-04) avec ses filtres : sans recherche, il ne sert pas au
 * gérant qui cherche l'origine d'un écart de caisse (benchmark B3).
 */
@Component({
  selector: 'app-audit',
  imports: [FormsModule, TranslocoDirective, ClubDatePipe],
  templateUrl: './audit.page.html',
})
export class AuditPage {
  private readonly api = inject(AuditApi);

  protected readonly erreur = signal<string | null>(null);

  /** Critères de recherche, appliqués ensemble à la demande de l'utilisateur. */
  protected readonly action = signal('');
  protected readonly typeObjet = signal('');
  protected readonly depuis = signal('');
  protected readonly jusqua = signal('');
  protected readonly numeroDePage = signal(0);

  private readonly criteres = signal({ action: '', entityType: '', from: '', to: '', page: 0 });

  protected readonly resultat = resource({
    params: () => this.criteres(),
    loader: async ({ params }) => {
      this.erreur.set(null);
      try {
        return await firstValueFrom(
          this.api.searchAuditEntries({
            page: params.page,
            size: 25,
            action: params.action || undefined,
            entityType: params.entityType || undefined,
            // Une date saisie vaut le jour entier, en UTC comme le contrat l'attend.
            from: params.from ? `${params.from}T00:00:00Z` : undefined,
            to: params.to ? `${params.to}T23:59:59Z` : undefined,
          }),
        );
      } catch (echec) {
        this.erreur.set(codeDErreur(echec));
        return null;
      }
    },
  });

  protected readonly entrees = computed<AuditEntry[]>(
    () => this.resultat.value()?.content ?? [],
  );
  protected readonly pages = computed(() => this.resultat.value()?.page?.totalPages ?? 0);
  protected readonly page = computed(() => (this.resultat.value()?.page?.page ?? 0) + 1);

  protected filtrer(): void {
    this.numeroDePage.set(0);
    this.appliquer();
  }

  protected reinitialiser(): void {
    this.action.set('');
    this.typeObjet.set('');
    this.depuis.set('');
    this.jusqua.set('');
    this.numeroDePage.set(0);
    this.appliquer();
  }

  protected allerA(page: number): void {
    this.numeroDePage.set(Math.max(0, page));
    this.appliquer();
  }

  private appliquer(): void {
    this.criteres.set({
      action: this.action(),
      entityType: this.typeObjet(),
      from: this.depuis(),
      to: this.jusqua(),
      page: this.numeroDePage(),
    });
  }
}
