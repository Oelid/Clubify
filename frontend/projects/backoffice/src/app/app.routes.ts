import { Routes } from '@angular/router';
import { exigeDroit, sessionFermee, sessionOuverte } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'connexion' },
  {
    path: 'connexion',
    canActivate: [sessionFermee],
    loadComponent: () => import('./auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'second-facteur',
    loadComponent: () => import('./auth/mfa.page').then((m) => m.MfaPage),
  },
  {
    path: '',
    canActivate: [sessionOuverte],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      {
        path: 'club',
        canActivate: [exigeDroit('club.settings.consulter')],
        loadComponent: () => import('./club/club-settings.page').then((m) => m.ClubSettingsPage),
      },
      {
        path: 'utilisateurs',
        canActivate: [exigeDroit('users.consulter')],
        loadComponent: () => import('./users/users.page').then((m) => m.UsersPage),
      },
      {
        path: 'sans-acces',
        loadComponent: () => import('./acces/sans-acces.page').then((m) => m.SansAccesPage),
      },
      {
        path: 'journal',
        canActivate: [exigeDroit('audit.consulter')],
        loadComponent: () => import('./audit/audit.page').then((m) => m.AuditPage),
      },
    ],
  },
];
