import { Routes } from '@angular/router';
import { signedInGuard, signedOutGuard } from './auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./budget/yearly-overview-page').then((m) => m.YearlyOverviewPage),
    canActivate: [signedInGuard],
    title: 'Haushaltsbuch',
  },
  {
    path: 'jahre/:year',
    loadComponent: () => import('./budget/budget-dashboard').then((m) => m.BudgetDashboard),
    canActivate: [signedInGuard],
    title: 'Haushaltsplan – Haushaltsbuch',
  },
  {
    path: 'geloeschte-plaene',
    loadComponent: () => import('./budget/deleted-plans-page').then((m) => m.DeletedPlansPage),
    canActivate: [signedInGuard],
    title: 'Gelöschte Pläne – Haushaltsbuch',
  },
  {
    path: 'vermoegen/:year',
    loadComponent: () => import('./net-worth/net-worth-page').then((m) => m.NetWorthPage),
    canActivate: [signedInGuard],
    title: 'Vermögen – Haushaltsbuch',
  },
  {
    path: 'passwort',
    loadComponent: () => import('./auth/ui/change-password-page').then((m) => m.ChangePasswordPage),
    canActivate: [signedInGuard],
    title: 'Passwort ändern – Haushaltsbuch',
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
