import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { AuthStore } from './data-access/auth-store';

/**
 * Sends the user to the login once the server no longer accepts their session (e.g. because
 * it expired), instead of leaving them with failing requests. Answers of /api/auth itself
 * are left to the auth store, a failed login is not an expired session.
 */
export const sessionExpiredInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return next(request).pipe(
    tap({
      error: (error: unknown) => {
        if (
          error instanceof HttpErrorResponse &&
          error.status === HttpStatusCode.Unauthorized &&
          !request.url.startsWith('/api/auth/')
        ) {
          auth.sessionEnded();
          void router.navigateByUrl('/login');
        }
      },
    }),
  );
};
