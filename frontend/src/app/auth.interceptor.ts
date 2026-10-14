import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { getAccessToken } from './auth-storage';
import { catchError, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { AuthService } from './core/auth.service';

const publicAuthEndpoints = ['/api/auth/login', '/api/auth/register'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  if (publicAuthEndpoints.some((endpoint) => request.url.includes(endpoint))) {
    return next(request);
  }

  const accessToken = getAccessToken();
  if (!accessToken) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      }
    })
  ).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        authService.forceLogout();
      }
      return throwError(() => error);
    })
  );
};
