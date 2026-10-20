import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { BudgetApi } from './budget-api';

describe('BudgetApi', () => {
  const planId = '123e4567-e89b-12d3-a456-426614174000';
  const emptyOverview = { id: planId, year: 2026, items: [] };
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
    req.flush({ id: planId, overview: emptyOverview });

    expect(await result).toEqual({ id: planId, overview: emptyOverview });
  });

  it('posts an income to a plan', async () => {
    const request = { description: 'Gehalt', amount: '2000', period: 'YEARLY' as const };
    const result = firstValueFrom(api.addIncome(planId, request));

    const req = http.expectOne(`/api/plans/${planId}/incomes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 'abc', overview: emptyOverview });

    expect(await result).toEqual({ id: 'abc', overview: emptyOverview });
  });

  it('posts an expense with category to a plan', () => {
    const request = {
      description: 'Miete',
      amount: '1000',
      period: 'YEARLY' as const,
      category: 'LEBENSUNTERHALT' as const,
    };
    api.addExpense(planId, request).subscribe();

    const req = http.expectOne(`/api/plans/${planId}/expenses`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 'abc' });
  });

  // AU-1
  it('changes and deletes an item of a plan by id and returns the overview', async () => {
    const changed = firstValueFrom(
      api.changeItem(planId, 'item-1', { description: 'Miete', amount: '900', period: 'YEARLY' }),
    );

    const change = http.expectOne(`/api/plans/${planId}/items/item-1`);
    expect(change.request.method).toBe('PUT');
    expect(change.request.body).toEqual({ description: 'Miete', amount: '900', period: 'YEARLY' });
    change.flush(emptyOverview);
    expect(await changed).toEqual(emptyOverview);

    const removed = firstValueFrom(api.removeItem(planId, 'item-1'));

    const req = http.expectOne(`/api/plans/${planId}/items/item-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(emptyOverview);
    expect(await removed).toEqual(emptyOverview);
  });

  // UK-14
  it('puts the items and the subcategory groups of a plan in a new order', async () => {
    const items = firstValueFrom(api.reorderItems(planId, { itemIds: ['b', 'a'] }));
    const itemOrder = http.expectOne(`/api/plans/${planId}/item-order`);
    expect(itemOrder.request.method).toBe('PUT');
    expect(itemOrder.request.body).toEqual({ itemIds: ['b', 'a'] });
    itemOrder.flush(emptyOverview);
    expect(await items).toEqual(emptyOverview);

    const request = { type: 'INCOME' as const, subcategories: ['Gehalt', 'Sonstiges'] };
    const groups = firstValueFrom(api.reorderSubcategories(planId, request));
    const groupOrder = http.expectOne(`/api/plans/${planId}/subcategory-order`);
    expect(groupOrder.request.method).toBe('PUT');
    expect(groupOrder.request.body).toEqual(request);
    groupOrder.flush(emptyOverview);
    expect(await groups).toEqual(emptyOverview);
  });
});
