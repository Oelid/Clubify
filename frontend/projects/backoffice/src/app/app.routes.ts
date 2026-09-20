import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'connexion' },
  {
    path: 'connexion',
    loadComponent: () => import('./auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'second-facteur',
    loadComponent: () => import('./auth/mfa.page').then((m) => m.MfaPage),
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      {
        path: 'club',
        loadComponent: () => import('./club/club-settings.page').then((m) => m.ClubSettingsPage),
      },
      {
        path: 'utilisateurs',
        loadComponent: () => import('./users/users.page').then((m) => m.UsersPage),
      },
      {
        path: 'journal',
        loadComponent: () => import('./audit/audit.page').then((m) => m.AuditPage),
      },
    ],
  },
];
