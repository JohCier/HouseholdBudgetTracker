import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './data-access/auth-store';

/** Only lets signed-in users through, everyone else is sent to the login. */
export const signedInGuard: CanActivateFn = async () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  await auth.ensureLoaded();
  return auth.signedIn() || router.createUrlTree(['/login']);
};

/** Keeps signed-in users away from login and registration. */
export const signedOutGuard: CanActivateFn = async () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  await auth.ensureLoaded();
  return !auth.signedIn() || router.createUrlTree(['/']);
};
