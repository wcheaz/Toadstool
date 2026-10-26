import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { isAuthenticated } from './auth-storage';

export const authGuard: CanActivateFn = () => {
  if (isAuthenticated()) {
    return true;
  }

  return inject(Router).createUrlTree(['/login']);
};
