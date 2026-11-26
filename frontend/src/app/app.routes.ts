import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Job Board',
    loadComponent: () => import('./applications/dashboard/dashboard').then((m) => m.Dashboard),
  },
  { path: '**', redirectTo: '' },
];
