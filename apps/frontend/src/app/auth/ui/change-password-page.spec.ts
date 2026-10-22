import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { AuthStore } from '../data-access/auth-store';
import { ChangePasswordPage } from './change-password-page';

describe('ChangePasswordPage', () => {
  let auth: { changePassword: ReturnType<typeof vi.fn> };
  let snackBar: { open: ReturnType<typeof vi.fn> };
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    auth = { changePassword: vi.fn(async () => undefined) };
    snackBar = { open: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthStore, useValue: auth },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  async function fillIn(values: Record<string, string>) {
    const fixture = TestBed.createComponent(ChangePasswordPage);
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

  // PA-1, PA-7
  it('changes the password, confirms it and returns to the overview', async () => {
    await fillIn({
      currentPassword: 'geheim123',
      password: 'neues-geheim',
      passwordRepeat: 'neues-geheim',
    });

    expect(auth.changePassword).toHaveBeenCalledWith({
      currentPassword: 'geheim123',
      newPassword: 'neues-geheim',
    });
    expect(snackBar.open).toHaveBeenCalledWith('Passwort geändert.', undefined, expect.anything());
    expect(navigate).toHaveBeenCalledWith('/');
  });

  // PA-3, PA-5
  it('requires the new password twice and at least 8 characters', async () => {
    const element = await fillIn({
      currentPassword: 'geheim123',
      password: 'neues-geheim',
      passwordRepeat: 'neues-geheimX',
    });
    expect(element.textContent).toContain('Die Passwörter stimmen nicht überein.');

    const short = await fillIn({
      currentPassword: 'geheim123',
      password: 'kurz',
      passwordRepeat: 'kurz',
    });
    expect(short.textContent).toContain('Mindestens 8 Zeichen.');

    expect(auth.changePassword).not.toHaveBeenCalled();
  });

  // PA-2
  it('shows why changing failed', async () => {
    auth.changePassword.mockRejectedValue(new Error('Das aktuelle Passwort ist falsch.'));

    const element = await fillIn({
      currentPassword: 'falsch123',
      password: 'neues-geheim',
      passwordRepeat: 'neues-geheim',
    });

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Das aktuelle Passwort ist falsch.',
    );
    expect(navigate).not.toHaveBeenCalled();
  });
});
