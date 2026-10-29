import { HttpErrorResponse } from '@angular/common/http';

/**
 * German messages for the errors of the API, by the `type` of their RFC 9457 problem
 * response (FM-1, FM-2). The English `detail` is meant for logs and API clients and is never
 * shown.
 */
const MESSAGES: Readonly<Record<string, string>> = {
  'urn:hbt:invalid-input': 'Die Eingabe ist ungültig.',
  'urn:hbt:plan-not-found': 'Diesen Haushaltsplan gibt es nicht (mehr).',
  'urn:hbt:item-not-found': 'Diesen Eintrag gibt es nicht (mehr).',
  'urn:hbt:plan-already-exists': 'Für dieses Jahr gibt es bereits einen Haushaltsplan.',
  'urn:hbt:net-worth-not-found': 'Für dieses Jahr ist kein Vermögen (mehr) erfasst.',
  'urn:hbt:net-worth-entry-not-found': 'Diesen Wert gibt es nicht (mehr).',
  'urn:hbt:net-worth-already-exists': 'Für dieses Jahr ist bereits Vermögen erfasst.',
  'urn:hbt:one-time-item-not-found': 'Diesen Einmalposten gibt es nicht (mehr).',
  'urn:hbt:email-already-registered': 'Für diese E-Mail-Adresse gibt es bereits ein Konto.',
  'urn:hbt:invalid-credentials': 'E-Mail-Adresse oder Passwort ist falsch.',
  'urn:hbt:too-many-login-attempts':
    'Zu viele Anmeldeversuche. Bitte versuche es in 15 Minuten erneut.',
  'urn:hbt:wrong-current-password': 'Das aktuelle Passwort ist falsch.',
  'urn:hbt:password-unchanged': 'Das neue Passwort muss sich vom aktuellen unterscheiden.',
};

/** A German, user-facing message for an error; a general one for unknown errors (FM-3). */
export function toMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const type: unknown = error.error?.type;
    if (typeof type === 'string' && type in MESSAGES) {
      return MESSAGES[type];
    }
    if (error.status === 0) {
      return 'Server nicht erreichbar.';
    }
    return `Die Anfrage ist fehlgeschlagen (${error.status}).`;
  }
  return 'Unbekannter Fehler.';
}
