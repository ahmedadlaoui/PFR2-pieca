import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing/landing.component';

export const routes: Routes = [
    { path: '', component: LandingPageComponent },
    { path: 'login', loadComponent: () => import('./auth/login/login').then(m => m.Login) },
    { path: 'register', loadComponent: () => import('./auth/register/register').then(m => m.Register) },
    { path: 'register/:role', loadComponent: () => import('./auth/register/register').then(m => m.Register) }
];
