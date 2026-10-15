import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthStore } from '../data-access/auth-store';
import { RegisterPage } from './register-page';

describe('RegisterPage', () => {
  let auth: { register: ReturnType<typeof vi.fn> };
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    auth = { register: vi.fn(async () => undefined) };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStore, useValue: auth }],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  async function fillIn(values: Record<string, string>) {
    const fixture = TestBed.createComponent(RegisterPage);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    for (const [name, value] of Object.entries(values)) {
      const input = element.querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('blur'));
    }
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
    return element;
  }

  it('registers and opens the household plan', async () => {
    await fillIn({
      email: 'max@example.org',
      password: 'geheim123',
      passwordRepeat: 'geheim123',
    });

    expect(auth.register).toHaveBeenCalledWith({ email: 'max@example.org', password: 'geheim123' });
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('requires the password twice and at least 8 characters', async () => {
    const element = await fillIn({
      email: 'max@example.org',
      password: 'geheim123',
      passwordRepeat: 'geheim124',
    });
    expect(element.textContent).toContain('Die Passwörter stimmen nicht überein.');

    await fillIn({ email: 'max@example.org', password: 'kurz', passwordRepeat: 'kurz' });

    expect(auth.register).not.toHaveBeenCalled();
  });

  it('shows why registering failed', async () => {
    auth.register.mockRejectedValue(
      new Error('Für diese E-Mail-Adresse gibt es bereits ein Konto.'),
    );

    const element = await fillIn({
      email: 'max@example.org',
      password: 'geheim123',
      passwordRepeat: 'geheim123',
    });

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('bereits ein Konto');
    expect(navigate).not.toHaveBeenCalled();
  });
});
