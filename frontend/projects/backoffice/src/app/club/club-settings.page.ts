import { Component, computed, inject, resource, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { Club, ClubApi, SettingDefinition, SettingValue } from 'api-client';
import { firstValueFrom } from 'rxjs';
import { ClubTheme } from 'ui';
import { codeDErreur } from '../core/api-errors';
import { SessionStore } from '../core/session.store';

/** Paramètres du club (ADM-01) et registre des règles configurables (9.8). */
@Component({
  selector: 'app-club-settings',
  imports: [FormsModule, TranslocoDirective],
  templateUrl: './club-settings.page.html',
})
export class ClubSettingsPage {
  private readonly api = inject(ClubApi);
  private readonly session = inject(SessionStore);
  private readonly theme = inject(ClubTheme);
  private readonly transloco = inject(TranslocoService);

  protected readonly erreur = signal<string | null>(null);
  protected readonly enregistre = signal(false);
  protected readonly enCours = signal(false);

  /** Copie de travail : l'identité n'est envoyée qu'à l'enregistrement. */
  protected readonly saisie = signal<Club | null>(null);

  /**
   * Règles modifiées mais pas encore enregistrées, par clé. Les couleurs en
   * font partie : ce sont des règles comme les autres, avec un champ à part.
   */
  private readonly reglagesModifies = signal<Record<string, string>>({});

  protected readonly club = resource({
    loader: async () => {
      const lu = await this.charger(() => firstValueFrom(this.api.getClub()));
      if (lu) {
        this.saisie.set({ ...lu });
      }
      return lu;
    },
  });

  protected readonly reglages = resource({
    loader: () => this.charger(() => firstValueFrom(this.api.getClubSettings())),
  });

  protected readonly definitions = resource({
    loader: () => this.charger(() => firstValueFrom(this.api.getSettingDefinitions())),
  });

  protected readonly peutModifier = computed(() =>
    this.session.permet('club.settings.modifier'),
  );

  /** Source documentée de chaque règle, pour que le gérant sache d'où elle vient. */
  protected readonly source = computed(() => {
    const index = new Map<string, SettingDefinition>();
    (this.definitions.value() ?? []).forEach((d) => index.set(d.key, d));
    return index;
  });

  protected valeurs(): SettingValue[] {
    return this.reglages.value() ?? [];
  }

  /** Couleurs de marque : deux règles configurables comme les autres (0025). */
  protected readonly marquePrincipale = computed(() => this.reglage('club.brand.primary'));
  protected readonly marqueSecondaire = computed(() => this.reglage('club.brand.secondary'));

  /**
   * Règle fixée par Clubify, qu'aucun club ne peut changer. Le dire vaut mieux
   * que de laisser quelqu'un chercher où la modifier.
   */
  protected estFigee(cle: string): boolean {
    return this.source().get(cle)?.scope === 'PLATFORM';
  }

  /**
   * La valeur, telle qu'un gérant la lit. Un « true » brut ne dit rien à qui
   * n'écrit pas de code.
   */
  protected valeurLisible(valeur: unknown): string {
    if (valeur === null || valeur === undefined) {
      return '—';
    }
    if (typeof valeur === 'boolean') {
      return this.transloco.translate(valeur ? 'app.yes' : 'app.no');
    }
    return Array.isArray(valeur) ? valeur.join(', ') : String(valeur);
  }

  private couleurParmi(valeurs: SettingValue[], cle: string): string | null {
    const trouvee = valeurs.find((r) => r.key === cle)?.value;
    return typeof trouvee === 'string' && trouvee.length > 0 ? trouvee : null;
  }

  private reglage(cle: string): string | null {
    const choisie = this.reglagesModifies()[cle];
    if (choisie) {
      return choisie;
    }
    const trouvee = this.valeurs().find((r) => r.key === cle)?.value;
    return typeof trouvee === 'string' && trouvee.length > 0 ? trouvee : null;
  }

  protected champ<K extends keyof Club>(nom: K, valeur: Club[K]): void {
    const courant = this.saisie();
    if (courant) {
      this.saisie.set({ ...courant, [nom]: valeur });
    }
  }

  /** Retient une valeur saisie ; elle part avec le reste à l'enregistrement. */
  protected choisirLaMarque(cle: string, valeur: string): void {
    this.reglagesModifies.set({ ...this.reglagesModifies(), [cle]: valeur });
  }

  /** Valeur en cours de saisie pour une règle, ou celle qui est enregistrée. */
  protected valeurSaisie(regle: SettingValue): string {
    const modifiee = this.reglagesModifies()[regle.key];
    if (modifiee !== undefined) {
      return modifiee;
    }
    if (Array.isArray(regle.value)) {
      return regle.value.join(', ');
    }
    return regle.value === null || regle.value === undefined ? '' : String(regle.value);
  }

  protected typeDe(cle: string): string {
    return this.source().get(cle)?.type ?? 'STRING';
  }

  /**
   * Convertit une saisie vers le type que la règle attend.
   *
   * <p>Le backend refuse une valeur mal typée ; la convertir ici évite au gérant
   * une erreur pour avoir écrit « 15 » dans un champ qui attend un nombre.
   */
  private valeurTypee(cle: string, saisie: string): unknown {
    switch (this.typeDe(cle)) {
      case 'BOOLEAN':
        return saisie === 'true';
      case 'INTEGER':
        return Number.parseInt(saisie, 10);
      case 'DECIMAL':
        return Number.parseFloat(saisie);
      case 'JSON':
        return saisie
          .split(',')
          .map((element) => element.trim())
          .filter((element) => element.length > 0);
      default:
        return saisie;
    }
  }

  protected async enregistrer(): Promise<void> {
    const demande = this.saisie();
    if (!demande || this.enCours()) {
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    this.enregistre.set(false);
    try {
      const mis = await firstValueFrom(
        this.api.updateClub({
          clubUpdateRequest: {
            name: demande.name,
            legalForm: demande.legalForm,
            ice: demande.ice,
            taxId: demande.taxId,
            tradeRegister: demande.tradeRegister,
            address: demande.address,
            phone: demande.phone,
            email: demande.email,
            timezone: demande.timezone,
            currency: demande.currency,
            defaultLanguage: demande.defaultLanguage,
          },
        }),
      );
      // Le backend a pu normaliser le téléphone : on affiche ce qu'il a retenu.
      this.saisie.set({ ...mis });

      const modifiees = this.reglagesModifies();
      if (Object.keys(modifiees).length > 0) {
        // On applique ce que le backend vient de retenir, et non une relecture
        // qui n'est pas encore arrivée : c'est ce qui rendait l'accent
        // capricieux après l'enregistrement.
        const retenues = await firstValueFrom(
          this.api.updateClubSettings({
            settingUpdate: Object.entries(modifiees).map(([key, saisie]) => ({
              key,
              value: this.valeurTypee(key, saisie),
            })),
          }),
        );
        this.reglages.set(retenues);
        this.reglagesModifies.set({});

        const principale = this.couleurParmi(retenues, 'club.brand.primary');
        if (principale) {
          this.theme.apply({
            primary: principale,
            secondary: this.couleurParmi(retenues, 'club.brand.secondary') ?? undefined,
          });
        } else {
          this.theme.reset();
        }
      }
      this.enregistre.set(true);
    } catch (echec) {
      this.erreur.set(codeDErreur(echec));
    } finally {
      this.enCours.set(false);
    }
  }

  private async charger<T>(appel: () => Promise<T>): Promise<T | null> {
    try {
      return await appel();
    } catch (echec) {
      this.erreur.set(codeDErreur(echec));
      return null;
    }
  }
}
