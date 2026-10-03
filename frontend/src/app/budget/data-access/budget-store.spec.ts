import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { BudgetItem, BudgetOverview } from '../budget.model';
import { BudgetApi } from './budget-api';
import { BudgetStore } from './budget-store';

const item: BudgetItem = { id: '1', description: 'Gehalt', amount: 2000, type: 'INCOME', category: null };
const overview: BudgetOverview = {
  totalIncome: 2000,
  totalExpense: 0,
  balance: 2000,
  expensesExceedIncome: false,
  categoryBreakdowns: [],
};

describe('BudgetStore', () => {
  let api: { [K in keyof BudgetApi]: ReturnType<typeof vi.fn> };
  let store: BudgetStore;

  beforeEach(() => {
    api = {
      addIncome: vi.fn(() => of({ id: '1' })),
      addExpense: vi.fn(() => of({ id: '2' })),
      removeItem: vi.fn(() => of(undefined)),
      listItems: vi.fn(() => of([item])),
      getOverview: vi.fn(() => of(overview)),
    };
    TestBed.configureTestingModule({ providers: [{ provide: BudgetApi, useValue: api }] });
    store = TestBed.inject(BudgetStore);
  });

  it('loads items and overview', async () => {
    await store.load();

    expect(store.items()).toEqual([item]);
    expect(store.overview()).toEqual(overview);
    expect(store.loading()).toBe(false);
    expect(store.loadError()).toBeNull();
  });

  it('reloads after a mutation', async () => {
    await store.addIncome({ description: 'Gehalt', amount: 2000 });

    expect(api.addIncome).toHaveBeenCalledWith({ description: 'Gehalt', amount: 2000 });
    expect(api.listItems).toHaveBeenCalledTimes(1);
    expect(store.items()).toEqual([item]);
  });

  it('surfaces the backend error message on a failed mutation', async () => {
    api.addExpense.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { error: 'amount must be positive' } })),
    );

    await expect(
      store.addExpense({ description: 'Miete', amount: -1, category: 'LEBENSUNTERHALT' }),
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
