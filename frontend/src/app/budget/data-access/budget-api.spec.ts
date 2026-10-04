import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { BudgetApi } from './budget-api';

describe('BudgetApi', () => {
  let api: BudgetApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(BudgetApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts an income', async () => {
    const request = { description: 'Gehalt', amount: '2000', bookingDate: '2026-10-01' };
    const result = firstValueFrom(api.addIncome(request));

    const req = http.expectOne('/api/budget/incomes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 'abc' });

    expect(await result).toEqual({ id: 'abc' });
  });

  it('posts an expense with category', () => {
    const request = {
      description: 'Miete',
      amount: '1000',
      category: 'LEBENSUNTERHALT' as const,
      bookingDate: '2026-10-03',
    };
    api.addExpense(request).subscribe();

    const req = http.expectOne('/api/budget/expenses');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 'abc' });
  });

  it('deletes an item by id', () => {
    api.removeItem('123e4567-e89b-12d3-a456-426614174000').subscribe();

    const req = http.expectOne('/api/budget/items/123e4567-e89b-12d3-a456-426614174000');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('lists items and fetches the overview of a year', () => {
    api.listItems(2026).subscribe();
    api.getOverview(2026).subscribe();

    expect(http.expectOne('/api/budget/items?year=2026').request.method).toBe('GET');
    expect(http.expectOne('/api/budget/overview?year=2026').request.method).toBe('GET');
  });
});
