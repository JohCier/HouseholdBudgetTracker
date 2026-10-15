import { HttpErrorResponse } from '@angular/common/http';

/** Extracts a user-facing message, preferring the `detail` of an RFC 9457 problem response. */
export function toMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error?.detail === 'string') {
      return error.error.detail;
    }
    if (error.status === 0) {
      return 'Server nicht erreichbar.';
    }
    return `Anfrage fehlgeschlagen (${error.status}).`;
  }
  return 'Unbekannter Fehler.';
}
