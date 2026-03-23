import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing/landing.component';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
    { path: '', component: LandingPageComponent },
    { path: 'login', loadComponent: () => import('./auth/login/login').then(m => m.Login), canActivate: [guestGuard] },
    { path: 'register', loadComponent: () => import('./auth/register/register').then(m => m.Register), canActivate: [guestGuard] },
    { path: 'register/:role', loadComponent: () => import('./auth/register/register').then(m => m.Register), canActivate: [guestGuard] }
];
