import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: route.routeConfig?.path ? `/${route.routeConfig.path}` : '/' }
    });
  }

  const expectedRoles = (route.data?.['roles'] as string[] | undefined) ?? [];
  const currentRole = authService.currentUser$.value?.role;

  if (!expectedRoles.length || (currentRole && expectedRoles.includes(currentRole))) {
    return true;
  }

  if (currentRole === 'SELLER') {
    return router.parseUrl('/seller/dashboard');
  }

  return router.parseUrl('/buyer/dashboard');
};
