import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    title: 'Job Board',
    canActivate: [authGuard],
    loadComponent: () => import('./applications/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'login',
    title: 'Sign in – Job Board',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/login-page/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    title: 'Create account – Job Board',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/register-page/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'profile',
    title: 'Profile – Job Board',
    canActivate: [authGuard],
    loadComponent: () => import('./profile/profile-page').then((m) => m.ProfilePage),
  },
  { path: '**', redirectTo: '' },
];
