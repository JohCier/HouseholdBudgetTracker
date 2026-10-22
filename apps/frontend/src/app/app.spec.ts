import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { App } from './app';
import { User } from './auth/auth.model';
import { AuthStore } from './auth/data-access/auth-store';

describe('App', () => {
  let user: ReturnType<typeof signal<User | null>>;
  let auth: { user: typeof user; logout: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    user = signal<User | null>(null);
    auth = { user, logout: vi.fn(async () => undefined) };
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: AuthStore, useValue: auth }],
    }).compileComponents();
  });

  it('should render the toolbar title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('mat-toolbar')?.textContent).toContain('Haushaltsbuch');
    expect(compiled.querySelector('mat-toolbar button')).toBeNull();
  });

  it('shows the signed-in user and signs them out', async () => {
    user.set({ id: 'u1', email: 'max@example.org' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const toolbar = (fixture.nativeElement as HTMLElement).querySelector('mat-toolbar')!;

    expect(toolbar.textContent).toContain('max@example.org');
    expect(toolbar.querySelector('a[href="/passwort"]')?.textContent).toContain('Passwort ändern');
    toolbar.querySelector('button')!.click();
    await fixture.whenStable();

    expect(auth.logout).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
