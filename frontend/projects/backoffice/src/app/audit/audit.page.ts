import { Component, signal } from '@angular/core';

/**
 * Journal d'audit (SEC-04) avec ses filtres : sans recherche, il ne sert pas au
 * gérant qui cherche l'origine d'un écart de caisse (benchmark B3).
 */
@Component({
  selector: 'app-audit',
  imports: [],
  templateUrl: './audit.page.html',
  styleUrl: '../shared/page.css',
})
export class AuditPage {
  protected readonly entrees = signal([
    { heure: "Aujourd'hui, 08:12", auteur: 'Nadia B.', type: 'Utilisateur', action: 'auth.login.succeeded', cible: '—' },
    { heure: "Aujourd'hui, 08:14", auteur: 'Nadia B.', type: 'Utilisateur', action: 'club.settings.updated', cible: 'files.max_size_mb : 10 → 25' },
    { heure: "Aujourd'hui, 08:20", auteur: 'Nadia B.', type: 'Utilisateur', action: 'user.permissions.updated', cible: 'Younes T.' },
    { heure: "Aujourd'hui, 09:02", auteur: 'Système', type: 'Système', action: 'auth.challenge.expire', cible: 'Règle automatique' },
    { heure: 'Hier, 19:41', auteur: 'Younes T.', type: 'Utilisateur', action: 'export.created', cible: 'users · CSV · 5 lignes' },
    { heure: 'Hier, 18:03', auteur: 'Inconnu', type: 'Utilisateur', action: 'auth.login.failed', cible: 'accueil.a@exemple.test' },
  ]);
}
