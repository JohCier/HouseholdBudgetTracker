import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from './auth-api';

describe('AuthApi', () => {
  const user = { id: 'u1', email: 'max@example.org' };
  const credentials = { email: 'max@example.org', password: 'geheim123' };
  let api: AuthApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AuthApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('fetches the signed-in user', async () => {
    const result = firstValueFrom(api.currentUser());

    http.expectOne('/api/auth/me').flush(user);

    expect(await result).toEqual(user);
  });

  it('yields null without a session', async () => {
    const result = firstValueFrom(api.currentUser());

    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(await result).toBeNull();
  });

  it('passes on other errors when fetching the user', async () => {
    const result = firstValueFrom(api.currentUser());

    http.expectOne('/api/auth/me').flush(null, { status: 500, statusText: 'Server Error' });

    await expect(result).rejects.toMatchObject({ status: 500 });
  });

  it('registers, signs in and out', () => {
    api.register(credentials).subscribe();
    api.login(credentials).subscribe();
    api.logout().subscribe();

    const register = http.expectOne('/api/auth/register');
    expect(register.request.method).toBe('POST');
    expect(register.request.body).toEqual(credentials);
    register.flush(user, { status: 201, statusText: 'Created' });
    const login = http.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual(credentials);
    login.flush(user);
    const logout = http.expectOne('/api/auth/logout');
    expect(logout.request.method).toBe('POST');
    logout.flush(null, { status: 204, statusText: 'No Content' });
  });
});
