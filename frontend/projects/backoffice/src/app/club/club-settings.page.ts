import { Component, computed, inject, resource, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';
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

  protected readonly erreur = signal<string | null>(null);
  protected readonly enregistre = signal(false);
  protected readonly enCours = signal(false);

  /** Copie de travail : l'identité n'est envoyée qu'à l'enregistrement. */
  protected readonly saisie = signal<Club | null>(null);

  /** Couleurs choisies mais pas encore enregistrées, par clé de règle. */
  private readonly marqueChoisie = signal<Record<string, string>>({});

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

  /** Une valeur de règle peut être une liste : on l'affiche sans la reformater. */
  protected valeurLisible(valeur: unknown): string {
    if (valeur === null || valeur === undefined) {
      return '—';
    }
    return Array.isArray(valeur) ? valeur.join(', ') : String(valeur);
  }

  private couleurParmi(valeurs: SettingValue[], cle: string): string | null {
    const trouvee = valeurs.find((r) => r.key === cle)?.value;
    return typeof trouvee === 'string' && trouvee.length > 0 ? trouvee : null;
  }

  private reglage(cle: string): string | null {
    const choisie = this.marqueChoisie()[cle];
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

  /** Retient une couleur choisie ; elle part avec le reste à l'enregistrement. */
  protected choisirLaMarque(cle: string, valeur: string): void {
    this.marqueChoisie.set({ ...this.marqueChoisie(), [cle]: valeur });
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

      const couleurs = this.marqueChoisie();
      if (Object.keys(couleurs).length > 0) {
        // On applique ce que le backend vient de retenir, et non une relecture
        // qui n'est pas encore arrivée : c'est ce qui rendait l'accent
        // capricieux après l'enregistrement.
        const retenues = await firstValueFrom(
          this.api.updateClubSettings({
            settingUpdate: Object.entries(couleurs).map(([key, value]) => ({ key, value })),
          }),
        );
        this.reglages.set(retenues);
        this.marqueChoisie.set({});

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
