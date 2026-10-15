import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthStore } from '../data-access/auth-store';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let auth: { login: ReturnType<typeof vi.fn> };
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    auth = { login: vi.fn(async () => undefined) };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStore, useValue: auth }],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  async function signIn(email: string, password: string) {
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    for (const [name, value] of Object.entries({ email, password })) {
      const input = element.querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
    return element;
  }

  it('signs in and opens the household plan', async () => {
    await signIn('max@example.org', 'geheim123');

    expect(auth.login).toHaveBeenCalledWith({ email: 'max@example.org', password: 'geheim123' });
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('shows why signing in failed', async () => {
    auth.login.mockRejectedValue(new Error('E-Mail-Adresse oder Passwort ist falsch.'));

    const element = await signIn('max@example.org', 'falsch');

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'E-Mail-Adresse oder Passwort ist falsch.',
    );
    expect(navigate).not.toHaveBeenCalled();
  });

  it('does not send an incomplete form', async () => {
    await signIn('keine-adresse', '');

    expect(auth.login).not.toHaveBeenCalled();
  });
});
