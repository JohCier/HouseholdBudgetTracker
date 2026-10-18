import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuthApi } from './auth-api';
import { AuthStore } from './auth-store';

const user = { id: 'u1', email: 'max@example.org' };
const credentials = { email: 'max@example.org', password: 'geheim123' };
const httpError = (status: number, type?: string) => () =>
  new HttpErrorResponse({ status, error: type ? { type, detail: 'English detail' } : null });

describe('AuthStore', () => {
  let api: { [K in keyof AuthApi]: ReturnType<typeof vi.fn> };
  let store: AuthStore;

  beforeEach(() => {
    api = {
      currentUser: vi.fn(() => of(null)),
      register: vi.fn(() => of(user)),
      login: vi.fn(() => of(user)),
      logout: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({ providers: [{ provide: AuthApi, useValue: api }] });
    store = TestBed.inject(AuthStore);
  });

  it('asks the server for the session only once', async () => {
    api.currentUser.mockReturnValue(of(user));

    await Promise.all([store.ensureLoaded(), store.ensureLoaded()]);
    await store.ensureLoaded();

    expect(api.currentUser).toHaveBeenCalledTimes(1);
    expect(store.user()).toEqual(user);
    expect(store.signedIn()).toBe(true);
  });

  it('treats an unreachable server as signed out', async () => {
    api.currentUser.mockReturnValue(throwError(httpError(0)));

    await store.ensureLoaded();

    expect(store.signedIn()).toBe(false);
  });

  it('signs in and out', async () => {
    await store.login(credentials);
    expect(store.user()).toEqual(user);

    await store.logout();
    expect(store.signedIn()).toBe(false);
    await store.ensureLoaded();
    expect(api.currentUser).not.toHaveBeenCalled();
  });

  it('is signed in right after registering', async () => {
    await store.register(credentials);

    expect(api.register).toHaveBeenCalledWith(credentials);
    expect(store.user()).toEqual(user);
  });

  it('explains failures in German', async () => {
    api.login.mockReturnValue(throwError(httpError(401, 'urn:hbt:invalid-credentials')));
    api.register.mockReturnValueOnce(
      throwError(httpError(409, 'urn:hbt:email-already-registered')),
    );

    await expect(store.login(credentials)).rejects.toThrow(
      'E-Mail-Adresse oder Passwort ist falsch.',
    );
    await expect(store.register(credentials)).rejects.toThrow(
      'Für diese E-Mail-Adresse gibt es bereits ein Konto.',
    );
    expect(store.signedIn()).toBe(false);
  });

  it('explains invalid input in German', async () => {
    api.register.mockReturnValue(throwError(httpError(400, 'urn:hbt:invalid-input')));

    await expect(store.register(credentials)).rejects.toThrow('Die Eingabe ist ungültig.');
  });

  it('is signed out even if the server cannot be told', async () => {
    await store.login(credentials);
    api.logout.mockReturnValue(throwError(httpError(0)));

    await expect(store.logout()).rejects.toBeDefined();
    expect(store.signedIn()).toBe(false);
  });
});
