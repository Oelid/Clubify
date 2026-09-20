import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Activation imposée du second facteur pour l'administrateur et le gérant
 * (décision 0027) : QR à scanner, puis codes de secours à conserver, sans
 * lesquels un téléphone perdu ferme le club (benchmark B5).
 */
@Component({
  selector: 'app-mfa',
  imports: [RouterLink],
  templateUrl: './mfa.page.html',
  styleUrl: './auth.css',
})
export class MfaPage {
  protected readonly codes = signal([
    '4F2K-9ZQ1', '8HTM-3BVD', 'W7NE-5RXC', 'QJ64-PL2A',
    'Z93D-KM8F', 'T5YB-H1WS', 'R2VC-6NQX', 'E8PA-4JKD',
  ]);
}
