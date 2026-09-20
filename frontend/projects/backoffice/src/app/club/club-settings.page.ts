import { Component, signal } from '@angular/core';

/** Paramètres du club (ADM-01) et registre des règles configurables (9.8). */
@Component({
  selector: 'app-club-settings',
  imports: [],
  templateUrl: './club-settings.page.html',
  styleUrl: '../shared/page.css',
})
export class ClubSettingsPage {
  protected readonly reglages = signal([
    { cle: 'club.timezone', libelle: 'Fuseau horaire', valeur: 'Africa/Casablanca', defaut: true },
    { cle: 'club.currency', libelle: 'Devise', valeur: 'MAD', defaut: true },
    { cle: 'security.mfa.trusted_device_days', libelle: 'Appareil de confiance', valeur: '30 jours', defaut: true },
    { cle: 'files.max_size_mb', libelle: 'Taille maximale des fichiers', valeur: '25 Mo', defaut: false },
    { cle: 'files.link_ttl_minutes', libelle: 'Durée des liens de fichier', valeur: '15 minutes', defaut: true },
    { cle: 'billing.receipt_prefix', libelle: 'Préfixe des reçus', valeur: 'RC', defaut: false },
  ]);
}
