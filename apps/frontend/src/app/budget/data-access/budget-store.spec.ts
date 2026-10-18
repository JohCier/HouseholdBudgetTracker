import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { BudgetItem, HouseholdPlanOverview } from '../budget.model';
import { currentYear } from '../year';
import { BudgetApi } from './budget-api';
import { BudgetStore } from './budget-store';

const item: BudgetItem = {
  id: '1',
  description: 'Gehalt',
  amount: '2000.00',
  type: 'INCOME',
  category: null,
};
const overview = (year: number, items = [item]): HouseholdPlanOverview => ({
  id: `plan-${year}`,
  year,
  totalIncome: '2000.00',
  totalExpense: '0.00',
  balance: '2000.00',
  expensesExceedIncome: false,
  categoryBreakdowns: [],
  items,
});

describe('BudgetStore', () => {
  let api: { [K in keyof BudgetApi]: ReturnType<typeof vi.fn> };
  let store: BudgetStore;
  let plannedYears: number[];

  beforeEach(() => {
    plannedYears = [2025, 2026, 2027, currentYear()];
    api = {
      getOverview: vi.fn((year: number) => of(plannedYears.includes(year) ? overview(year) : null)),
      createPlan: vi.fn(() => of({ id: 'new-plan' })),
      addIncome: vi.fn(() => of({ id: '1' })),
      addExpense: vi.fn(() => of({ id: '2' })),
      changeItem: vi.fn(() => of(undefined)),
      removeItem: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({
      providers: [BudgetStore, { provide: BudgetApi, useValue: api }],
    });
    store = TestBed.inject(BudgetStore);
  });

  it('starts with the current year and loads the overview of its plan', async () => {
    await store.load();

    expect(store.year()).toBe(currentYear());
    expect(api.getOverview).toHaveBeenCalledWith(currentYear());
    expect(store.planId()).toBe(`plan-${currentYear()}`);
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

  it('knows when a year has no plan yet and can create one', async () => {
    await store.selectYear(2030);

    expect(store.planId()).toBeNull();
    expect(store.planMissing()).toBe(true);
    expect(store.items()).toEqual([]);
    expect(store.overview()).toBeNull();

    plannedYears.push(2030);
    await store.createPlan();

    expect(api.createPlan).toHaveBeenCalledWith(2030);
    expect(store.planId()).toBe('plan-2030');
    expect(store.planMissing()).toBe(false);
  });

  it('adds items to the plan of the selected year', async () => {
    await store.selectYear(2026);

    await store.addIncome({ description: 'Gehalt', amount: '2000' });

    expect(api.addIncome).toHaveBeenCalledWith('plan-2026', {
      description: 'Gehalt',
      amount: '2000',
    });
    expect(api.getOverview).toHaveBeenLastCalledWith(2026);
  });

  // PB-7
  it('changes an item of the plan of the selected year and reloads the overview', async () => {
    await store.selectYear(2026);
    api.getOverview.mockClear();

    await store.changeItem('1', { description: 'Gehalt', amount: '2100' });

    expect(api.changeItem).toHaveBeenCalledWith('plan-2026', '1', {
      description: 'Gehalt',
      amount: '2100',
    });
    expect(api.getOverview).toHaveBeenCalledWith(2026);
  });

  it('rejects adding an item while the year has no plan', async () => {
    await store.selectYear(2030);

    await expect(store.addIncome({ description: 'Gehalt', amount: '2000' })).rejects.toThrow(
      'Für 2030 gibt es noch keinen Haushaltsplan.',
    );
    expect(api.addIncome).not.toHaveBeenCalled();
  });

  it('ignores responses of outdated loads', async () => {
    const slowOverview = new Subject<HouseholdPlanOverview>();
    api.getOverview.mockReturnValueOnce(slowOverview);

    const outdated = store.selectYear(2025);
    await store.selectYear(2026);
    slowOverview.next(overview(2025, [{ ...item, description: 'veraltet' }]));
    slowOverview.complete();
    await outdated;

    expect(store.year()).toBe(2026);
    expect(store.items()).toEqual([item]);
    expect(store.overview()?.year).toBe(2026);
  });

  it('restores a removed expense into the plan it was removed from', async () => {
    await store.selectYear(2027);
    const expense: BudgetItem = {
      ...item,
      type: 'EXPENSE',
      category: 'FREIZEIT_SPASS',
      description: 'Kino',
    };

    await store.restoreItem('plan-2026', expense);

    expect(api.addExpense).toHaveBeenCalledWith('plan-2026', {
      description: 'Kino',
      amount: '2000.00',
      category: 'FREIZEIT_SPASS',
    });
  });

  it('explains a failed mutation in German', async () => {
    await store.selectYear(2026);
    api.getOverview.mockClear();
    api.addExpense.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: {
              type: 'urn:hbt:invalid-input',
              title: 'Bad Request',
              detail: 'amount must be positive',
            },
          }),
      ),
    );

    await expect(
      store.addExpense({ description: 'Miete', amount: '-1', category: 'LEBENSUNTERHALT' }),
    ).rejects.toThrow('Die Eingabe ist ungültig.');
    expect(api.getOverview).not.toHaveBeenCalled();
  });

  it('records a load error', async () => {
    api.getOverview.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    await store.load();

    expect(store.loadError()).toBe('Server nicht erreichbar.');
    expect(store.loading()).toBe(false);
  });
});
