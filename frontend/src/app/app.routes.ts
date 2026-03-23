import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing/landing.component';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
    { path: '', component: LandingPageComponent },
    { 
      path: 'buyer', 
      loadComponent: () => import('./buyer/buyer-layout').then(m => m.BuyerLayout),
      canActivate: [roleGuard], 
      data: { roles: ['BUYER'] },
      children: [
        { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
        { path: 'dashboard', loadComponent: () => import('./buyer/buyer-dashboard').then(m => m.BuyerDashboard) },
        { path: 'demand/new', loadComponent: () => import('./demand/new-demand').then(m => m.NewDemand) },
        { path: 'demand/:id', loadComponent: () => import('./demand/demand-details').then(m => m.DemandDetails) }
      ]
    },
    { path: 'seller/dashboard', loadComponent: () => import('./seller/seller-dashboard').then(m => m.SellerDashboard), canActivate: [roleGuard], data: { roles: ['SELLER'] } },
    { path: 'login', loadComponent: () => import('./auth/login/login').then(m => m.Login), canActivate: [guestGuard] },
    { path: 'register', loadComponent: () => import('./auth/register/register').then(m => m.Register), canActivate: [guestGuard] },
    { path: 'register/:role', loadComponent: () => import('./auth/register/register').then(m => m.Register), canActivate: [guestGuard] },
    { path: '**', redirectTo: '' }
];
