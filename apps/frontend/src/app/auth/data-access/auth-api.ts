import { HttpClient, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { LoginRequest, RegisterUserRequest, User } from '../auth.model';

/**
 * Thin HTTP adapter for the users app's /api/auth endpoints. The session itself is an
 * HttpOnly cookie set and removed by the server; the browser sends it along on its own.
 */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/auth';

  /** The signed-in user, or `null` without a valid session. */
  currentUser(): Observable<User | null> {
    return this.http
      .get<User>(`${this.baseUrl}/me`)
      .pipe(
        catchError((error: unknown) =>
          error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized
            ? of(null)
            : throwError(() => error),
        ),
      );
  }

  register(request: RegisterUserRequest): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/register`, request);
  }

  login(request: LoginRequest): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/login`, request);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, null);
  }
}
