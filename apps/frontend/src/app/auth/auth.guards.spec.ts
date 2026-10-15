import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { signedInGuard, signedOutGuard } from './auth.guards';
import { AuthStore } from './data-access/auth-store';

describe('auth guards', () => {
  const signedIn = signal(false);
  const auth = { signedIn, ensureLoaded: vi.fn(async () => undefined) };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStore, useValue: auth }],
    });
  });

  const run = (guard: typeof signedInGuard) =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Promise<boolean | UrlTree>;

  it('sends signed-out users to the login', async () => {
    signedIn.set(false);

    expect(String(await run(signedInGuard))).toBe('/login');
    expect(await run(signedOutGuard)).toBe(true);
    expect(auth.ensureLoaded).toHaveBeenCalled();
  });

  it('keeps signed-in users away from login and registration', async () => {
    signedIn.set(true);

    expect(await run(signedInGuard)).toBe(true);
    expect(String(await run(signedOutGuard))).toBe('/');
  });
});
