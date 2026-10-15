import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStore } from './data-access/auth-store';
import { sessionExpiredInterceptor } from './session-expired.interceptor';

describe('sessionExpiredInterceptor', () => {
  const auth = { sessionEnded: vi.fn() };
  let http: HttpClient;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    auth.sessionEnded.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessionExpiredInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthStore, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  it('sends the user to the login when the API rejects their session', async () => {
    const result = firstValueFrom(http.get('/api/plans'));

    backend.expectOne('/api/plans').flush(null, { status: 401, statusText: 'Unauthorized' });

    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(auth.sessionEnded).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/login');
  });

  it('leaves failed logins and other errors alone', async () => {
    const login = firstValueFrom(http.post('/api/auth/login', {}));
    const plans = firstValueFrom(http.get('/api/plans'));

    backend.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/plans').flush(null, { status: 500, statusText: 'Server Error' });

    await expect(login).rejects.toBeDefined();
    await expect(plans).rejects.toBeDefined();
    expect(auth.sessionEnded).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });
});
