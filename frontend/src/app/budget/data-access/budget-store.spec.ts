import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { BudgetItem, BudgetOverview } from '../budget.model';
import { currentYear } from '../year';
import { BudgetApi } from './budget-api';
import { BudgetStore } from './budget-store';

const item: BudgetItem = {
  id: '1',
  description: 'Gehalt',
  amount: '2000.00',
  type: 'INCOME',
  category: null,
  bookingDate: '2026-10-01',
};
const overview = (year: number): BudgetOverview => ({
  year,
  totalIncome: '2000.00',
  totalExpense: '0.00',
  balance: '2000.00',
  expensesExceedIncome: false,
  categoryBreakdowns: [],
});

describe('BudgetStore', () => {
  let api: { [K in keyof BudgetApi]: ReturnType<typeof vi.fn> };
  let store: BudgetStore;

  beforeEach(() => {
    api = {
      addIncome: vi.fn(() => of({ id: '1' })),
      addExpense: vi.fn(() => of({ id: '2' })),
      removeItem: vi.fn(() => of(undefined)),
      listItems: vi.fn(() => of([item])),
      getOverview: vi.fn((year: number) => of(overview(year))),
    };
    TestBed.configureTestingModule({ providers: [{ provide: BudgetApi, useValue: api }] });
    store = TestBed.inject(BudgetStore);
  });

  it('starts with the current year and loads its items and overview', async () => {
    await store.load();

    expect(store.year()).toBe(currentYear());
    expect(api.listItems).toHaveBeenCalledWith(currentYear());
    expect(store.items()).toEqual([item]);
    expect(store.overview()).toEqual(overview(currentYear()));
    expect(store.loading()).toBe(false);
    expect(store.loadError()).toBeNull();
  });

  it('navigates between years', async () => {
    await store.selectYear(2026);
    await store.nextYear();
    expect(store.year()).toBe(2027);

    await store.previousYear();
    await store.previousYear();
    expect(store.year()).toBe(2025);
    expect(api.getOverview).toHaveBeenLastCalledWith(2025);
  });

  it('switches to the booking year after adding an item', async () => {
    await store.selectYear(2026);

    await store.addIncome({ description: 'Gehalt', amount: '2000', bookingDate: '2027-01-01' });

    expect(api.addIncome).toHaveBeenCalledWith({
      description: 'Gehalt',
      amount: '2000',
      bookingDate: '2027-01-01',
    });
    expect(store.year()).toBe(2027);
    expect(api.listItems).toHaveBeenLastCalledWith(2027);
  });

  it('ignores responses of outdated loads', async () => {
    const slowItems = new Subject<BudgetItem[]>();
    api.listItems.mockReturnValueOnce(slowItems);

    const outdated = store.selectYear(2025);
    await store.selectYear(2026);
    slowItems.next([{ ...item, description: 'veraltet' }]);
    slowItems.complete();
    await outdated;

    expect(store.year()).toBe(2026);
    expect(store.items()).toEqual([item]);
    expect(store.overview()?.year).toBe(2026);
  });

  it('restores a removed expense with its original data', async () => {
    const expense: BudgetItem = {
      ...item,
      type: 'EXPENSE',
      category: 'FREIZEIT_SPASS',
      description: 'Kino',
    };

    await store.restoreItem(expense);

    expect(api.addExpense).toHaveBeenCalledWith({
      description: 'Kino',
      amount: '2000.00',
      category: 'FREIZEIT_SPASS',
      bookingDate: '2026-10-01',
    });
  });

  it('surfaces the problem detail on a failed mutation', async () => {
    api.addExpense.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { title: 'Bad Request', detail: 'amount must be positive' },
          }),
      ),
    );

    await expect(
      store.addExpense({
        description: 'Miete',
        amount: '-1',
        category: 'LEBENSUNTERHALT',
        bookingDate: '2026-10-01',
      }),
    ).rejects.toThrow('amount must be positive');
    expect(api.listItems).not.toHaveBeenCalled();
  });

  it('records a load error', async () => {
    api.listItems.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    await store.load();

    expect(store.loadError()).toBe('Server nicht erreichbar.');
    expect(store.loading()).toBe(false);
  });
});
