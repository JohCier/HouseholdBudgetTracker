import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { BudgetItem, BudgetOverview } from '../budget.model';
import { currentMonth } from '../month';
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
const overview = (month: string): BudgetOverview => ({
  month,
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
      getOverview: vi.fn((month: string) => of(overview(month))),
    };
    TestBed.configureTestingModule({ providers: [{ provide: BudgetApi, useValue: api }] });
    store = TestBed.inject(BudgetStore);
  });

  it('starts with the current month and loads its items and overview', async () => {
    await store.load();

    expect(store.month()).toBe(currentMonth());
    expect(api.listItems).toHaveBeenCalledWith(currentMonth());
    expect(store.items()).toEqual([item]);
    expect(store.overview()).toEqual(overview(currentMonth()));
    expect(store.loading()).toBe(false);
    expect(store.loadError()).toBeNull();
  });

  it('navigates between months across year boundaries', async () => {
    await store.selectMonth('2026-12');
    await store.nextMonth();
    expect(store.month()).toBe('2027-01');

    await store.previousMonth();
    await store.previousMonth();
    expect(store.month()).toBe('2026-11');
    expect(api.getOverview).toHaveBeenLastCalledWith('2026-11');
  });

  it('switches to the booking month after adding an item', async () => {
    await store.selectMonth('2026-10');

    await store.addIncome({ description: 'Gehalt', amount: '2000', bookingDate: '2026-11-01' });

    expect(api.addIncome).toHaveBeenCalledWith({
      description: 'Gehalt',
      amount: '2000',
      bookingDate: '2026-11-01',
    });
    expect(store.month()).toBe('2026-11');
    expect(api.listItems).toHaveBeenLastCalledWith('2026-11');
  });

  it('ignores responses of outdated loads', async () => {
    const slowItems = new Subject<BudgetItem[]>();
    api.listItems.mockReturnValueOnce(slowItems);

    const outdated = store.selectMonth('2026-09');
    await store.selectMonth('2026-10');
    slowItems.next([{ ...item, description: 'veraltet' }]);
    slowItems.complete();
    await outdated;

    expect(store.month()).toBe('2026-10');
    expect(store.items()).toEqual([item]);
    expect(store.overview()?.month).toBe('2026-10');
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
