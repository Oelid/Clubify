import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * Coquille du backoffice : navigation à gauche, identité du club en haut.
 * Densité inspirée du tableau de bord Stripe (décision 0025) : l'accueil a un
 * parent devant elle et cinq minutes.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  protected readonly club = signal({ name: 'Club A', user: 'Nadia B.', role: 'Administrateur du compte' });

  protected readonly sections = [
    { libelle: 'Tableau de bord', lien: '/club', icone: 'M3 12h4l3 8 4-16 3 8h4' },
    { libelle: 'Paramètres du club', lien: '/club', icone: 'M12 15a3 3 0 100-6 3 3 0 000 6z' },
    { libelle: 'Utilisateurs', lien: '/utilisateurs', icone: 'M17 20h5v-2a3 3 0 00-5.36-1.9M17 20H7' },
    { libelle: "Journal d'audit", lien: '/journal', icone: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5' },
  ];
}
