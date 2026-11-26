import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { AUTH_URL, AuthService } from './auth.service';

function withToken(request: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}

/**
 * Adds the access token to API calls. On a 401 it refreshes once (shared across parallel requests)
 * and retries; if the refresh fails the user is sent to sign in.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/') || request.url.startsWith(`${AUTH_URL}/`)) {
    return next(request);
  }

  const auth = inject(AuthService);
  const sentToken = auth.accessToken();

  return next(withToken(request, sentToken)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      // Another request may already have refreshed while this one was in flight.
      const current = auth.accessToken();
      const freshToken =
        current && current !== sentToken ? Promise.resolve(current) : auth.refresh();
      return from(freshToken).pipe(
        switchMap((token) => {
          if (!token) {
            void auth.sessionEnded();
            return throwError(() => error);
          }
          return next(withToken(request, token));
        }),
      );
    }),
  );
};
