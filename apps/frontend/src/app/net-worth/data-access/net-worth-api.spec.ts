import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { NetWorthApi } from './net-worth-api';

// docs/specs/vermoegen.md
describe('NetWorthApi', () => {
  let api: NetWorthApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(NetWorthApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('addresses assets and liabilities by their year', async () => {
    const entry = { type: 'ASSET', name: 'Girokonto', amount: '3200.00' } as const;
    const requests = [
      firstValueFrom(api.getOverview(2026)),
      firstValueFrom(api.getHistory()),
      firstValueFrom(api.addEntry(2026, entry)),
      firstValueFrom(api.changeEntry(2026, 'e1', entry)),
      firstValueFrom(api.removeEntry(2026, 'e1')),
      firstValueFrom(api.copy(2026, { targetYear: 2027, overwrite: false })),
    ];

    for (const [method, url, body] of [
      ['GET', '/api/net-worth/2026', null],
      ['GET', '/api/net-worth/years', null],
      ['POST', '/api/net-worth/2026/entries', entry],
      ['PUT', '/api/net-worth/2026/entries/e1', entry],
      ['DELETE', '/api/net-worth/2026/entries/e1', null],
      ['POST', '/api/net-worth/2026/copies', { targetYear: 2027, overwrite: false }],
    ] as const) {
      const request = http.expectOne({ method, url });
      expect(request.request.body).toEqual(body);
      request.flush({});
    }
    await Promise.all(requests);
  });
});
