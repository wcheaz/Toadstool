import { HttpInterceptorFn } from '@angular/common/http';
import { getAccessToken } from './auth-storage';

const publicAuthEndpoints = ['/api/auth/login', '/api/auth/register'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
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
  );
};
