import { expect, test } from '@playwright/test';
import { login, logout, PASSWORD, register, uniqueEmail } from './support';

// docs/specs/anmeldung.md
test.describe('Anmeldung', () => {
  test('nach der Registrierung ist der Benutzer angemeldet und sieht das Haushaltsbuch (AN-5)', async ({
    page,
  }) => {
    const email = await register(page);

    await expect(page).toHaveURL('/');
    await expect(page.getByRole('button', { name: 'Abmelden' })).toBeVisible();
    await expect(page.getByText(email)).toBeVisible();
    await expect(
      page.getByRole('heading', { name: String(new Date().getFullYear()) }),
    ).toBeVisible();
  });

  test('ein zu kurzes Passwort wird abgelehnt, ohne die Anfrage zu senden (AN-4)', async ({
    page,
  }) => {
    const requests: string[] = [];
    page.on('request', (request) => requests.push(request.url()));
    await page.goto('/register');

    await page.getByLabel('E-Mail-Adresse').fill(uniqueEmail());
    await page.getByLabel('Passwort', { exact: true }).fill('1234567');
    await page.getByLabel('Passwort wiederholen').fill('1234567');
    await page.getByRole('button', { name: 'Konto anlegen' }).click();

    await expect(page.getByText('Mindestens 8 Zeichen.')).toBeVisible();
    await expect(page).toHaveURL('/register');
    expect(requests.filter((url) => url.includes('/api/auth/register'))).toEqual([]);
  });

  test('eine Adresse gehört zu höchstens einem Konto, unabhängig von der Schreibweise (AN-2, AN-3)', async ({
    page,
  }) => {
    const email = await register(page);
    await logout(page);

    await page.goto('/register');
    await page.getByLabel('E-Mail-Adresse').fill(email.toUpperCase());
    await page.getByLabel('Passwort', { exact: true }).fill(PASSWORD);
    await page.getByLabel('Passwort wiederholen').fill(PASSWORD);
    await page.getByRole('button', { name: 'Konto anlegen' }).click();

    await expect(page.getByRole('alert')).toHaveText(
      'Für diese E-Mail-Adresse gibt es bereits ein Konto.',
    );
  });

  test('anmelden mit richtigem Passwort, abmelden beendet die Sitzung (AN-1, AN-8, AN-9)', async ({
    page,
  }) => {
    const email = await register(page);
    await logout(page);

    // Signed out, only sign-in and registration are reachable
    await page.goto('/');
    await expect(page).toHaveURL('/login');

    await login(page, email);
    await expect(page).toHaveURL('/');
    await expect(page.getByText(email)).toBeVisible();

    // Signed in, the sign-in page is no longer reachable
    await page.goto('/login');
    await expect(page).toHaveURL('/');
  });

  test('falsches Passwort und unbekannte Adresse führen zur selben Meldung (AN-6)', async ({
    page,
  }) => {
    const email = await register(page);
    await logout(page);

    await login(page, email, 'falsches-passwort');
    await expect(page.getByRole('alert')).toHaveText('E-Mail-Adresse oder Passwort ist falsch.');

    await login(page, uniqueEmail());
    await expect(page.getByRole('alert')).toHaveText('E-Mail-Adresse oder Passwort ist falsch.');
  });

  test('nach 5 Fehlversuchen ist die Anmeldung gesperrt, auch mit richtigem Passwort (AN-10)', async ({
    page,
  }) => {
    const email = await register(page);
    await logout(page);

    for (let attempt = 0; attempt < 5; attempt++) {
      await login(page, email, 'falsches-passwort');
      await expect(page.getByRole('alert')).toHaveText('E-Mail-Adresse oder Passwort ist falsch.');
    }
    await login(page, email);

    await expect(page.getByRole('alert')).toHaveText(
      'Zu viele Anmeldeversuche. Bitte versuche es in 15 Minuten erneut.',
    );
    await expect(page).toHaveURL('/login');
  });
});
