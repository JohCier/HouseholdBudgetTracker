import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, of, throwError } from 'rxjs';
import { NetWorthOverview } from '../net-worth.model';
import { NetWorthApi } from './net-worth-api';
import { NetWorthStore } from './net-worth-store';

const overview = (year: number, names: string[] = []): NetWorthOverview => ({
  year,
  assets: names.map((name, index) => ({ id: `e${index}`, type: 'ASSET', name, amount: '100.00' })),
  liabilities: [],
  totalAssets: '0.00',
  totalLiabilities: '0.00',
  netWorth: '0.00',
  overindebted: false,
  change: null,
});

// docs/specs/vermoegen.md
describe('NetWorthStore', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let store: NetWorthStore;

  beforeEach(() => {
    api = {
      getOverview: vi.fn((year: number) => of(overview(year))),
      addEntry: vi.fn(() => of(overview(2026, ['Girokonto']))),
      removeEntry: vi.fn(() =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 404,
              error: { type: 'urn:hbt:net-worth-entry-not-found' },
            }),
        ),
      ),
    };
    TestBed.configureTestingModule({
      providers: [NetWorthStore, { provide: NetWorthApi, useValue: api }],
    });
    store = TestBed.inject(NetWorthStore);
  });

  // VS-10
  it('knows a year without entries as empty', async () => {
    await store.selectYear(2027);

    expect(store.year()).toBe(2027);
    expect(store.empty()).toBe(true);
  });

  // VS-5
  it('shows the overview a change answers with', async () => {
    await store.selectYear(2026);
    await store.addEntry({ type: 'ASSET', name: 'Girokonto', amount: '100.00' });

    expect(store.overview()!.assets.map((entry) => entry.name)).toEqual(['Girokonto']);
    expect(store.empty()).toBe(false);
  });

  it('rejects a failed change with a German message', async () => {
    await store.selectYear(2026);

    await expect(store.removeEntry('e0')).rejects.toThrow('Diesen Wert gibt es nicht (mehr).');
  });

  it('forgets the previous year while the next one loads', async () => {
    await store.selectYear(2026);
    const pending = new Subject<NetWorthOverview>();
    api['getOverview'].mockReturnValue(pending);

    const loading = store.selectYear(2027);

    expect(store.overview()).toBeNull();
    pending.next(overview(2027));
    pending.complete();
    await loading;
    expect(store.overview()!.year).toBe(2027);
  });
});
