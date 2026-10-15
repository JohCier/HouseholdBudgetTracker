import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import { LoginRequest, RegisterUserRequest, User } from '../auth.model';
import { AuthApi } from './auth-api';

/**
 * Knows who is signed in. Whether there is a session is only known after asking the
 * server once ({@link ensureLoaded}), since the session cookie is not readable by scripts.
 * Actions reject with an Error carrying a user-facing message.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(AuthApi);

  /** `undefined` until the server has been asked, `null` when signed out. */
  private readonly _user = signal<User | null | undefined>(undefined);
  private loaded?: Promise<void>;

  readonly user = computed(() => this._user() ?? null);
  readonly signedIn = computed(() => !!this._user());

  /** Asks the server for the current session, once; later calls wait for that answer. */
  ensureLoaded(): Promise<void> {
    this.loaded ??= firstValueFrom(this.api.currentUser()).then(
      (user) => this._user.set(user),
      () => this._user.set(null),
    );
    return this.loaded;
  }

  login(request: LoginRequest): Promise<void> {
    return this.startSession(this.api.login(request), {
      [HttpStatusCode.Unauthorized]: 'E-Mail-Adresse oder Passwort ist falsch.',
    });
  }

  register(request: RegisterUserRequest): Promise<void> {
    return this.startSession(this.api.register(request), {
      [HttpStatusCode.Conflict]: 'Für diese E-Mail-Adresse gibt es bereits ein Konto.',
    });
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.api.logout(), { defaultValue: undefined });
    } finally {
      this.sessionEnded();
    }
  }

  /** Forgets the user, e.g. because the server no longer accepts their session. */
  sessionEnded(): void {
    this._user.set(null);
    this.loaded = Promise.resolve();
  }

  private async startSession(
    request: Observable<User>,
    messages: Partial<Record<number, string>>,
  ): Promise<void> {
    try {
      this._user.set(await firstValueFrom(request));
      this.loaded = Promise.resolve();
    } catch (error) {
      const message = error instanceof HttpErrorResponse ? messages[error.status] : undefined;
      throw new Error(message ?? toMessage(error));
    }
  }
}
