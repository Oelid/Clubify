import { Component, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { ClubTheme } from 'ui';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly document = inject(DOCUMENT);
  private readonly theme = inject(ClubTheme);

  constructor() {
    // FR au MVP. L'arabe basculera `dir` sans toucher aux écrans, parce que
    // les feuilles de style n'emploient que des propriétés logiques.
    this.document.documentElement.lang = 'fr';
    this.document.documentElement.dir = 'ltr';

    // Marque du club, lue dans ses paramètres (ADM-01). En maquette, celle du
    // club pilote ; à la connexion, celle que renvoie /club.
    this.theme.apply({ primary: '#307890', secondary: '#f08840' });
  }
}
