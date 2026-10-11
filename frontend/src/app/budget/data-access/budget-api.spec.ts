import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { BudgetApi } from './budget-api';

describe('BudgetApi', () => {
  const planId = '123e4567-e89b-12d3-a456-426614174000';
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

  it('fetches the overview of the plan of a year', async () => {
    const result = firstValueFrom(api.getOverview(2026));

    const req = http.expectOne('/api/plans?year=2026');
    expect(req.request.method).toBe('GET');
    req.flush({ id: planId, year: 2026 });

    expect(await result).toEqual({ id: planId, year: 2026 });
  });

  it('yields null for a year without plan', async () => {
    const result = firstValueFrom(api.getOverview(2030));

    http
      .expectOne('/api/plans?year=2030')
      .flush({ detail: 'No household plan for 2030' }, { status: 404, statusText: 'Not Found' });

    expect(await result).toBeNull();
  });

  it('passes on other errors when fetching the overview', async () => {
    const result = firstValueFrom(api.getOverview(2026));

    http.expectOne('/api/plans?year=2026').flush(null, { status: 500, statusText: 'Server Error' });

    await expect(result).rejects.toMatchObject({ status: 500 });
  });

  it('creates a plan for a year', async () => {
    const result = firstValueFrom(api.createPlan(2026));

    const req = http.expectOne('/api/plans');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ year: 2026 });
    req.flush({ id: planId });

    expect(await result).toEqual({ id: planId });
  });

  it('posts an income to a plan', async () => {
    const request = { description: 'Gehalt', amount: '2000' };
    const result = firstValueFrom(api.addIncome(planId, request));

    const req = http.expectOne(`/api/plans/${planId}/incomes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 'abc' });

    expect(await result).toEqual({ id: 'abc' });
  });

  it('posts an expense with category to a plan', () => {
    const request = { description: 'Miete', amount: '1000', category: 'LEBENSUNTERHALT' as const };
    api.addExpense(planId, request).subscribe();

    const req = http.expectOne(`/api/plans/${planId}/expenses`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 'abc' });
  });

  it('deletes an item of a plan by id', () => {
    api.removeItem(planId, 'item-1').subscribe();

    const req = http.expectOne(`/api/plans/${planId}/items/item-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
