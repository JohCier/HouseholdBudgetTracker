import { HttpErrorResponse } from '@angular/common/http';
import { toMessage } from './http-error';

const problem = (status: number, type: string, detail: string) =>
  new HttpErrorResponse({ status, error: { type, title: 'Error', detail } });

describe('toMessage', () => {
  // FM-1, FM-2
  it('shows a German message for each known problem type instead of its detail', () => {
    expect(
      toMessage(
        problem(409, 'urn:hbt:plan-already-exists', 'A household plan for 2026 already exists'),
      ),
    ).toBe('Für dieses Jahr gibt es bereits einen Haushaltsplan.');
    expect(
      toMessage(
        problem(409, 'urn:hbt:email-already-registered', 'max@example.org is already registered'),
      ),
    ).toBe('Für diese E-Mail-Adresse gibt es bereits ein Konto.');
    expect(
      toMessage(problem(401, 'urn:hbt:invalid-credentials', 'E-mail address or password is wrong')),
    ).toBe('E-Mail-Adresse oder Passwort ist falsch.');
    // AN-10
    expect(
      toMessage(
        problem(429, 'urn:hbt:too-many-login-attempts', 'Too many failed sign-in attempts'),
      ),
    ).toBe('Zu viele Anmeldeversuche. Bitte versuche es in 15 Minuten erneut.');
    // PA-2, PA-4
    expect(
      toMessage(problem(400, 'urn:hbt:wrong-current-password', 'The current password is wrong')),
    ).toBe('Das aktuelle Passwort ist falsch.');
    expect(
      toMessage(
        problem(
          400,
          'urn:hbt:password-unchanged',
          'The new password must differ from the current one',
        ),
      ),
    ).toBe('Das neue Passwort muss sich vom aktuellen unterscheiden.');
    expect(toMessage(problem(400, 'urn:hbt:invalid-input', 'amount must be positive'))).toBe(
      'Die Eingabe ist ungültig.',
    );
    expect(toMessage(problem(404, 'urn:hbt:plan-not-found', 'Household plan not found: x'))).toBe(
      'Diesen Haushaltsplan gibt es nicht (mehr).',
    );
    expect(toMessage(problem(404, 'urn:hbt:item-not-found', 'Budget item not found: x'))).toBe(
      'Diesen Eintrag gibt es nicht (mehr).',
    );
  });

  // FM-3
  it('never shows the detail of an unknown problem type', () => {
    expect(toMessage(problem(400, 'about:blank', 'Failed to convert value'))).toBe(
      'Die Anfrage ist fehlgeschlagen (400).',
    );
  });

  it('reports an unreachable server', () => {
    expect(toMessage(new HttpErrorResponse({ status: 0 }))).toBe('Server nicht erreichbar.');
  });

  it('falls back to the status code without a problem response', () => {
    expect(toMessage(new HttpErrorResponse({ status: 500, error: 'Internal Server Error' }))).toBe(
      'Die Anfrage ist fehlgeschlagen (500).',
    );
  });

  it('does not expose other errors', () => {
    expect(toMessage(new TypeError('x is undefined'))).toBe('Unbekannter Fehler.');
  });
});
