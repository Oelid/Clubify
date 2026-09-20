import { Component, signal } from '@angular/core';

/**
 * Liste des utilisateurs du club. Densité de tableau assumée : c'est l'écran le
 * plus proche des grilles que l'accueil connaît déjà (risque d'adoption, 10.I).
 */
@Component({
  selector: 'app-users',
  imports: [],
  templateUrl: './users.page.html',
  styleUrl: '../shared/page.css',
})
export class UsersPage {
  protected readonly utilisateurs = signal([
    { nom: 'Nadia B.', email: 'admin.a@exemple.test', role: "Administrateur du compte", mfa: true, actif: true, derniere: "Aujourd'hui, 08:12" },
    { nom: 'Younes T.', email: 'gerant.a@exemple.test', role: 'Gérant', mfa: true, actif: true, derniere: 'Hier, 19:40' },
    { nom: 'Salma R.', email: 'accueil.a@exemple.test', role: 'Accueil', mfa: false, actif: true, derniere: "Aujourd'hui, 07:55" },
    { nom: 'Karim E.', email: 'coach.judo@exemple.test', role: 'Coach', mfa: false, actif: true, derniere: '18/09/2026' },
    { nom: 'Imane L.', email: 'coach.natation@exemple.test', role: 'Coach', mfa: false, actif: false, derniere: '02/07/2026' },
  ]);
}
