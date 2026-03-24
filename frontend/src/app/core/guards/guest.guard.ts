import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const guestGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    return true;
  }

  const role = authService.getCurrentUserSnapshot()?.role;

  if (role === 'SELLER') {
    return router.parseUrl('/seller/dashboard');
  }

  if (role === 'BUYER') {
    return router.parseUrl('/buyer/dashboard');
  }

  authService.logout();
  return router.parseUrl('/login');
};
