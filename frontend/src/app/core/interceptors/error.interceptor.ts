import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

let isRefreshing = false;

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {

      // Auto-refresh on 401, but not for auth endpoints themselves
      if (error.status === 401 && !req.url.includes('/auth/') && !isRefreshing) {
        isRefreshing = true;

        return authService.refreshAccessToken().pipe(
          switchMap(res => {
            isRefreshing = false;
            const cloned = req.clone({
              setHeaders: { Authorization: `Bearer ${res.accessToken}` }
            });
            return next(cloned);
          }),
          catchError(refreshError => {
            isRefreshing = false;
            authService.logout();
            router.navigate(['/login']);
            return throwError(() => new Error('Session expirée, veuillez vous reconnecter'));
          })
        );
      }

      let errorMessage = 'Une erreur inattendue est survenue';
      
      if (error.error instanceof ErrorEvent) {
        errorMessage = `Erreur réseau: ${error.error.message}`;
      } else {
        switch (error.status) {
          case 400:
            errorMessage = error.error?.message || 'Requête invalide';
            break;
          case 401:
            errorMessage = error.error?.message || 'Identifiants incorrects';
            break;
          case 403:
            errorMessage = error.error?.message || 'Accès refusé';
            break;
          case 404:
            errorMessage = error.error?.message || 'Ressource introuvable';
            break;
          case 409:
            errorMessage = error.error?.message || 'Conflit: cet élément existe déjà';
            break;
          case 422:
            errorMessage = error.error?.message || 'Données invalides';
            break;
          case 500:
            errorMessage = 'Erreur serveur. Veuillez réessayer plus tard.';
            break;
          default:
            errorMessage = `Erreur serveur: ${error.status}`;
            break;
        }
      }

      console.error('[ErrorInterceptor]', errorMessage, error);
      return throwError(() => new Error(errorMessage));
    })
  );
};
