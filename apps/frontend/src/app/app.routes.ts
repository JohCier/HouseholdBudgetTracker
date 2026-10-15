import { Routes } from '@angular/router';
import { signedInGuard, signedOutGuard } from './auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./budget/budget-dashboard').then((m) => m.BudgetDashboard),
    canActivate: [signedInGuard],
    title: 'Haushaltsbuch',
  },
  {
    path: 'login',
    loadComponent: () => import('./auth/ui/login-page').then((m) => m.LoginPage),
    canActivate: [signedOutGuard],
    title: 'Anmelden – Haushaltsbuch',
  },
  {
    path: 'register',
    loadComponent: () => import('./auth/ui/register-page').then((m) => m.RegisterPage),
    canActivate: [signedOutGuard],
    title: 'Registrieren – Haushaltsbuch',
  },
  { path: '**', redirectTo: '' },
];
