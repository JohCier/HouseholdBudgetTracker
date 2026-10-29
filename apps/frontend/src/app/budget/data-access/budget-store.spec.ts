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
  period: 'YEARLY',
  monthlyAmount: '166.67',
  annualAmount: '2000.00',
  type: 'INCOME',
  category: null,
  subcategory: null,
  note: null,
  important: false,
  savingPotential: null,
  percentageOfGroup: null,
};
const overview = (year: number, items = [item]): HouseholdPlanOverview => ({
  id: `plan-${year}`,
  year,
  totalIncome: '2000.00',
  totalExpense: '0.00',
  balance: '2000.00',

  monthlyTotalIncome: '166.67',

  monthlyTotalExpense: '0.00',

  monthlyBalance: '166.67',

  monthlySavingPotential: '0.00',

  savingPotential: '0.00',
  savingPotentialsBySubcategory: [],
  topSavingPotentials: [],
  topExpenses: [],
  expensesExceedIncome: false,
  categoryBreakdowns: [],
  items,
  incomeGroups:
    items.length === 0
      ? []
      : [
          {
            subcategory: 'Sonstiges',
            total: '2000.00',
            monthlyTotal: '166.67',
            percentageOfSection: 100,
            monthlySavingPotential: '0.00',
            items,
          },
        ],
  expenseCategories: [],
});
const yearOf = (planId: string) => Number(planId.replace('plan-', ''));
const added: BudgetItem = { ...item, id: 'added', description: 'Bonus' };

describe('BudgetStore', () => {
  let api: { [K in keyof BudgetApi]: ReturnType<typeof vi.fn> };
  let store: BudgetStore;
  let plannedYears: number[];

  beforeEach(() => {
    plannedYears = [2025, 2026, 2027, currentYear()];
    api = {
      getOverview: vi.fn((year: number) => of(plannedYears.includes(year) ? overview(year) : null)),
      getYearlyOverview: vi.fn(() => of({ years: [], deletedPlanCount: 0 })),
      copyPlan: vi.fn(),
      deletePlan: vi.fn(),
      getDeletedPlans: vi.fn(),
      restorePlan: vi.fn(),
      purgePlan: vi.fn(),
      createPlan: vi.fn((year: number) => of({ id: `plan-${year}`, overview: overview(year, []) })),
      addIncome: vi.fn((planId: string) =>
        of({ id: 'added', overview: overview(yearOf(planId), [item, added]) }),
      ),
      addExpense: vi.fn((planId: string) =>
        of({ id: 'added', overview: overview(yearOf(planId), [item, added]) }),
      ),
      changeItem: vi.fn((planId: string) =>
        of(overview(yearOf(planId), [{ ...item, amount: '2100.00' }])),
      ),
      removeItem: vi.fn((planId: string) => of(overview(yearOf(planId), []))),
      reorderItems: vi.fn((planId: string) => of(overview(yearOf(planId)))),
      reorderSubcategories: vi.fn((planId: string) => of(overview(yearOf(planId)))),
      markImportant: vi.fn((planId: string) =>
        of(overview(yearOf(planId), [{ ...item, important: true }])),
      ),
      changeSavingPotential: vi.fn((planId: string) => of(overview(yearOf(planId)))),
      removeSavingPotential: vi.fn((planId: string) => of(overview(yearOf(planId)))),
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

  it('loads the plan of the selected year', async () => {
    await store.selectYear(2025);

    expect(store.year()).toBe(2025);
    expect(api.getOverview).toHaveBeenLastCalledWith(2025);
  });

  // HP-1
  it('knows whether there is a year before or after the selected one', async () => {
    await store.selectYear(1900);
    expect(store.hasPreviousYear()).toBe(false);
    expect(store.hasNextYear()).toBe(true);

    await store.selectYear(2200);
    expect(store.hasNextYear()).toBe(false);
    expect(store.hasPreviousYear()).toBe(true);
  });

  it('knows when a year has no plan yet and can create one', async () => {
    await store.selectYear(2030);

    expect(store.planId()).toBeNull();
    expect(store.planMissing()).toBe(true);
    expect(store.items()).toEqual([]);
    expect(store.overview()).toBeNull();

    api.getOverview.mockClear();
    await store.createPlan();

    expect(api.createPlan).toHaveBeenCalledWith(2030);
    expect(store.planId()).toBe('plan-2030');
    expect(store.planMissing()).toBe(false);
    expect(api.getOverview).not.toHaveBeenCalled();
  });

  // AU-5
  it('adds items to the plan of the selected year and shows the returned overview', async () => {
    await store.selectYear(2026);
    api.getOverview.mockClear();

    await store.addIncome({ description: 'Bonus', amount: '2000', period: 'YEARLY' });

    expect(api.addIncome).toHaveBeenCalledWith('plan-2026', {
      description: 'Bonus',
      amount: '2000',
      period: 'YEARLY',
    });
    expect(store.items()).toEqual([item, added]);
    expect(api.getOverview).not.toHaveBeenCalled();
  });

  // PB-7, AU-5
  it('changes an item of the plan of the selected year and shows the returned overview', async () => {
    await store.selectYear(2026);
    api.getOverview.mockClear();

    await store.changeItem('1', { description: 'Gehalt', amount: '2100', period: 'YEARLY' });

    expect(api.changeItem).toHaveBeenCalledWith('plan-2026', '1', {
      description: 'Gehalt',
      amount: '2100',
      period: 'YEARLY',
    });
    expect(store.items()[0].amount).toBe('2100.00');
    expect(api.getOverview).not.toHaveBeenCalled();
  });

  // AU-5
  it('removes an item and shows the returned overview', async () => {
    await store.selectYear(2026);
    api.getOverview.mockClear();

    await store.removeItem('1');

    expect(api.removeItem).toHaveBeenCalledWith('plan-2026', '1');
    expect(store.items()).toEqual([]);
    expect(api.getOverview).not.toHaveBeenCalled();
  });

  // AU-5, PO-9
  it('does not show the overview of a change to another year than the selected one', async () => {
    await store.selectYear(2027);

    await store.restoreItem('plan-2026', item);

    expect(store.year()).toBe(2027);
    expect(store.overview()).toEqual(overview(2027));
  });

  // AU-6
  it('prefers the overview of a change over a load still running', async () => {
    await store.selectYear(2026);
    const slowOverview = new Subject<HouseholdPlanOverview>();
    api.getOverview.mockReturnValueOnce(slowOverview);

    const loading = store.load();
    await store.removeItem('1');
    expect(store.loading()).toBe(false);
    slowOverview.next(overview(2026));
    slowOverview.complete();
    await loading;

    expect(store.items()).toEqual([]);
  });

  it('rejects adding an item while the year has no plan', async () => {
    await store.selectYear(2030);

    await expect(
      store.addIncome({ description: 'Gehalt', amount: '2000', period: 'YEARLY' }),
    ).rejects.toThrow('Für 2030 gibt es noch keinen Haushaltsplan.');
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

  it('forgets the previous year while the selected one loads (HP-1)', async () => {
    await store.selectYear(2026);
    const pending = new Subject<HouseholdPlanOverview>();
    api.getOverview.mockReturnValueOnce(pending);

    const loading = store.selectYear(2027);

    expect(store.year()).toBe(2027);
    expect(store.overview()).toBeNull();
    expect(store.planId()).toBeNull();
    expect(store.items()).toEqual([]);
    expect(store.planMissing()).toBe(false);
    await expect(
      store.addIncome({ description: 'Bonus', amount: '1.00', period: 'YEARLY' }),
    ).rejects.toThrow('Für 2027 gibt es noch keinen Haushaltsplan.');
    expect(api.addIncome).not.toHaveBeenCalled();

    pending.next(overview(2027));
    pending.complete();
    await loading;
    expect(store.planId()).toBe('plan-2027');
  });

  it('shows nothing of the previous year when loading the selected one fails (HP-1)', async () => {
    await store.selectYear(2026);
    api.getOverview.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 0 })));

    await store.selectYear(2027);

    expect(store.loadError()).not.toBeNull();
    expect(store.overview()).toBeNull();
    expect(store.planId()).toBeNull();
    expect(store.planMissing()).toBe(false);
  });

  it('keeps the overview while reloading the same year', async () => {
    await store.selectYear(2026);
    const pending = new Subject<HouseholdPlanOverview>();
    api.getOverview.mockReturnValueOnce(pending);

    const reloading = store.load();

    expect(store.planId()).toBe('plan-2026');
    pending.next(overview(2026));
    pending.complete();
    await reloading;
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
      period: 'YEARLY',
      category: 'FREIZEIT_SPASS',
    });
  });

  it('marks an item as important and shows the returned overview (PM-4a)', async () => {
    await store.selectYear(2026);

    await store.markImportant('1', true);

    expect(api.markImportant).toHaveBeenCalledWith('plan-2026', '1', { important: true });
    expect(store.items()[0].important).toBe(true);
  });

  it('sets the monthly saving potential of an expense and removes it (PM-2a, PM-4b)', async () => {
    await store.selectYear(2026);

    await store.changeSavingPotential('1', '5.00');
    await store.changeSavingPotential('1', null);

    expect(api.changeSavingPotential).toHaveBeenCalledWith('plan-2026', '1', {
      monthlyAmount: '5.00',
    });
    expect(api.removeSavingPotential).toHaveBeenCalledWith('plan-2026', '1');
  });

  it('restores a removed expense with its marks (PM-6)', async () => {
    await store.restoreItem('plan-2026', {
      ...item,
      type: 'EXPENSE',
      category: 'FREIZEIT_SPASS',
      description: 'Streaming',
      important: true,
      savingPotential: '5.00',
    });

    expect(api.addExpense).toHaveBeenCalledWith('plan-2026', {
      description: 'Streaming',
      amount: '2000.00',
      period: 'YEARLY',
      category: 'FREIZEIT_SPASS',
      important: true,
      savingPotential: '5.00',
    });
  });

  it('restores a removed item with its note (PN-8)', async () => {
    await store.restoreItem('plan-2026', { ...item, note: 'netto' });

    expect(api.addIncome).toHaveBeenCalledWith('plan-2026', {
      description: 'Gehalt',
      amount: '2000.00',
      period: 'YEARLY',
      note: 'netto',
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
      store.addExpense({
        description: 'Miete',
        amount: '-1',
        period: 'YEARLY',
        category: 'LEBENSUNTERHALT',
      }),
    ).rejects.toThrow('Die Eingabe ist ungültig.');
    expect(api.getOverview).not.toHaveBeenCalled();
  });

  it('records a load error', async () => {
    api.getOverview.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    await store.load();

    expect(store.loadError()).toBe('Server nicht erreichbar.');
    expect(store.loading()).toBe(false);
  });

  it('suggests the predefined and the used subcategories of a type (UK-7, UK-8)', async () => {
    api.getOverview.mockReturnValue(
      of(
        overview(2026, [
          { ...item, subcategory: 'Mieteinnahmen' },
          { ...added, subcategory: 'gehalt' },
        ]),
      ),
    );
    await store.selectYear(2026);

    const incomes = store.subcategorySuggestions('INCOME');
    expect(incomes).toContain('Mieteinnahmen');
    expect(incomes).toContain('gehalt');
    expect(incomes).not.toContain('Gehalt');
    expect(incomes).toContain('Sonstiges');
    expect(store.subcategorySuggestions('EXPENSE')).not.toContain('Mieteinnahmen');
  });

  // UK-14
  it('shows a new order of the items right away and saves it', async () => {
    api.getOverview.mockReturnValue(of(overview(2026, [item, added])));
    await store.selectYear(2026);
    api.getOverview.mockClear();
    const response = new Subject<HouseholdPlanOverview>();
    api.reorderItems.mockReturnValue(response);

    const saved = store.reorderItems(['added', '1']);

    expect(store.overview()!.incomeGroups[0].items.map((i) => i.id)).toEqual(['added', '1']);
    expect(api.reorderItems).toHaveBeenCalledWith('plan-2026', { itemIds: ['added', '1'] });
    response.next(overview(2026, [added, item]));
    response.complete();
    await saved;
    expect(store.items()).toEqual([added, item]);
    expect(api.getOverview).not.toHaveBeenCalled();
  });

  // UK-14
  it('shows a new order of the groups right away and loads the stored one if saving fails', async () => {
    const groups = {
      ...overview(2026),
      incomeGroups: [
        {
          subcategory: 'Gehalt',
          total: '2000.00',
          monthlyTotal: '166.67',
          percentageOfSection: 0,
          monthlySavingPotential: '0.00',
          items: [item],
        },
        {
          subcategory: 'Sonstiges',
          total: '2000.00',
          monthlyTotal: '166.67',
          percentageOfSection: 0,
          monthlySavingPotential: '0.00',
          items: [added],
        },
      ],
    };
    api.getOverview.mockReturnValue(of(groups));
    await store.selectYear(2026);
    const response = new Subject<HouseholdPlanOverview>();
    api.reorderSubcategories.mockReturnValue(response);

    const saved = store.reorderSubcategories({ type: 'INCOME', category: null }, [
      'Sonstiges',
      'Gehalt',
    ]);

    expect(store.overview()!.incomeGroups.map((g) => g.subcategory)).toEqual([
      'Sonstiges',
      'Gehalt',
    ]);
    expect(api.reorderSubcategories).toHaveBeenCalledWith('plan-2026', {
      type: 'INCOME',
      subcategories: ['Sonstiges', 'Gehalt'],
    });
    response.error(new HttpErrorResponse({ status: 0 }));
    await expect(saved).rejects.toThrow('Server nicht erreichbar.');
    await vi.waitFor(() =>
      expect(store.overview()!.incomeGroups.map((g) => g.subcategory)).toEqual([
        'Gehalt',
        'Sonstiges',
      ]),
    );
  });
});
