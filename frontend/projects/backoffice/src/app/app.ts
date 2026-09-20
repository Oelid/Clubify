import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { LangueService } from './core/langue.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly langue = inject(LangueService);
  private readonly transloco = inject(TranslocoService);

  constructor() {
    // Avant toute connexion, la langue par défaut du produit. Celle de
    // l'utilisateur prend le relais dès que la session s'ouvre, et la direction
    // du document suit (PLT-08).
    this.langue.appliquer(this.transloco.getDefaultLang());

    // La marque du club est appliquée à la connexion, depuis ses paramètres
    // (ADM-01) : rien n'est codé en dur ici.
  }
}
