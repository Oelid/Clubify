import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

/**
 * Connexion du staff (SEC-01). Pensée clavier : la mise au point part sur le
 * courriel, la tabulation suit l'ordre visuel, Entrée valide.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.page.html',
  styleUrl: './auth.css',
})
export class LoginPage {
  protected readonly email = signal('');
  protected readonly motDePasse = signal('');
}
