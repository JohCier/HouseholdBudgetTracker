import { HttpErrorResponse } from '@angular/common/http';
import { toMessage } from './http-error';

describe('toMessage', () => {
  it('prefers the detail of a problem response', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { title: 'Conflict', detail: 'Für 2026 gibt es bereits einen Haushaltsplan.' },
    });

    expect(toMessage(error)).toBe('Für 2026 gibt es bereits einen Haushaltsplan.');
  });

  it('reports an unreachable server', () => {
    expect(toMessage(new HttpErrorResponse({ status: 0 }))).toBe('Server nicht erreichbar.');
  });

  it('falls back to the status code without a detail', () => {
    expect(toMessage(new HttpErrorResponse({ status: 500, error: 'Internal Server Error' }))).toBe(
      'Anfrage fehlgeschlagen (500).',
    );
  });

  it('does not expose other errors', () => {
    expect(toMessage(new TypeError('x is undefined'))).toBe('Unbekannter Fehler.');
  });
});
