import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { inject } from '@angular/core';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected error occurred';
      
      if (error.error instanceof ErrorEvent) {
        // Client-side or network error
        errorMessage = `Network Error: ${error.error.message}`;
      } else {
        // Backend returned an unsuccessful response code
        switch (error.status) {
          case 400:
            errorMessage = error.error?.message || 'Bad Request (File processing failed, etc.)';
            break;
          case 401:
            errorMessage = error.error?.message || 'Unauthorized: Invalid credentials';
            break;
          case 403:
            errorMessage = error.error?.message || 'Forbidden: You do not have permission for this action';
            break;
          case 404:
            errorMessage = error.error?.message || 'Resource Not Found';
            break;
          case 409:
            errorMessage = error.error?.message || 'Conflict: Action conflicts with database state (e.g., duplicated phone)';
            break;
          case 422:
            errorMessage = error.error?.message || 'Unprocessable Entity: Business rule violated';
            break;
          case 500:
            errorMessage = 'Internal Server Error. Please contact support.';
            break;
          default:
            errorMessage = `Server Error: ${error.status}`;
            break;
        }
      }

      console.error('[ErrorInterceptor]', errorMessage, error);
      return throwError(() => new Error(errorMessage));
    })
  );
};
