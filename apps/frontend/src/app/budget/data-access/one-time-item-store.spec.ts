import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OneTimeItemOverview } from '../budget.model';
import { OneTimeItemStore } from './one-time-item-store';

const overview = (year: number, description = 'Nebenkostennachzahlung'): OneTimeItemOverview => ({
  year,
  items: [
    {
      id: '1',
      type: 'EXPENSE',
      description,
      amount: '1200.00',
      date: `${year}-03-15`,
      note: null,
    },
  ],
  totalIncome: '0.00',
  totalExpense: '1200.00',
  balance: '-1200.00',
  expensesExceedIncome: true,
});

// docs/specs/einmalposten.md
describe('OneTimeItemStore', () => {
  let store: OneTimeItemStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OneTimeItemStore, provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(OneTimeItemStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // EP-8, EP-10
  it('loads the one-time items of the selected year', async () => {
    const selected = store.selectYear(2026);
    http.expectOne('/api/one-time-items?year=2026').flush(overview(2026));
    await selected;

    expect(store.year()).toBe(2026);
    expect(store.items().map((item) => item.description)).toEqual(['Nebenkostennachzahlung']);
    expect(store.loadError()).toBeNull();
  });

  it('forgets the items of the previous year right away and ignores a late answer for it', async () => {
    const first = store.selectYear(2026);
    const second = store.selectYear(2027);
    expect(store.overview()).toBeNull();

    http.expectOne('/api/one-time-items?year=2027').flush(overview(2027, 'Bonus'));
    http.expectOne('/api/one-time-items?year=2026').flush(overview(2026));
    await Promise.all([first, second]);

    expect(store.items().map((item) => item.description)).toEqual(['Bonus']);
  });

  it('shows why loading failed', async () => {
    const selected = store.selectYear(2026);
    http
      .expectOne('/api/one-time-items?year=2026')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await selected;

    expect(store.loadError()).toBe('Die Anfrage ist fehlgeschlagen (500).');
  });

  // EP-7
  it('enters, changes and deletes an item and reads the items of the year again after each', async () => {
    const selected = store.selectYear(2026);
    http.expectOne('/api/one-time-items?year=2026').flush(overview(2026));
    await selected;
    const request = {
      type: 'INCOME' as const,
      description: 'Bonus',
      amount: '2000',
      date: '2026-12-01',
    };

    const created = store.createItem(request);
    const post = http.expectOne({ method: 'POST', url: '/api/one-time-items' });
    expect(post.request.body).toEqual(request);
    post.flush({ id: '2' });
    await Promise.resolve();
    http.expectOne('/api/one-time-items?year=2026').flush(overview(2026, 'Neu geladen'));
    await created;
    expect(store.items()[0].description).toBe('Neu geladen');

    const changed = store.changeItem('2', { ...request, date: '2027-01-05' });
    http.expectOne({ method: 'PUT', url: '/api/one-time-items/2' }).flush(null);
    await Promise.resolve();
    http.expectOne('/api/one-time-items?year=2026').flush(overview(2026));
    await changed;

    const deleted = store.deleteItem('1');
    http.expectOne({ method: 'DELETE', url: '/api/one-time-items/1' }).flush(null);
    await Promise.resolve();
    http.expectOne('/api/one-time-items?year=2026').flush({ ...overview(2026), items: [] });
    await deleted;
    expect(store.items()).toEqual([]);
  });

  // EP-4
  it('rejects a failed change with a German message and keeps the items', async () => {
    const selected = store.selectYear(2026);
    http.expectOne('/api/one-time-items?year=2026').flush(overview(2026));
    await selected;

    const deleted = store.deleteItem('1');
    http
      .expectOne('/api/one-time-items/1')
      .flush({ type: 'urn:hbt:one-time-item-not-found' }, { status: 404, statusText: 'Not Found' });

    await expect(deleted).rejects.toThrow('Diesen Einmalposten gibt es nicht (mehr).');
    expect(store.items()).toHaveLength(1);
  });
});
