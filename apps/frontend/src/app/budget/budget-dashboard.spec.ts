import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject } from 'rxjs';
import { BudgetDashboard } from './budget-dashboard';
import { BudgetItem, HouseholdPlanOverview } from './budget.model';
import { BudgetStore } from './data-access/budget-store';

const item: BudgetItem = {
  id: '1',
  description: 'Kino',
  amount: '12.50',
  period: 'YEARLY',
  monthlyAmount: '1.04',
  annualAmount: '12.50',
  type: 'EXPENSE',
  category: 'FREIZEIT_SPASS',
  subcategory: null,
  note: null,
};
const overview: HouseholdPlanOverview = {
  id: 'plan-2026',
  year: 2026,
  totalIncome: '0.00',
  totalExpense: '12.50',
  balance: '-12.50',

  monthlyTotalIncome: '0.00',

  monthlyTotalExpense: '1.04',

  monthlyBalance: '-1.04',
  expensesExceedIncome: true,
  categoryBreakdowns: [],
  items: [item],
  incomeGroups: [],
  expenseCategories: [
    {
      category: 'FREIZEIT_SPASS',
      total: '12.50',
      monthlyTotal: '1.04',
      groups: [{ subcategory: 'Sonstiges', total: '12.50', monthlyTotal: '1.04', items: [item] }],
    },
  ],
};

describe('BudgetDashboard', () => {
  it('offers to undo a deletion', async () => {
    const undo = new Subject<void>();
    const snackBar = { open: vi.fn(() => ({ onAction: () => undo })) };
    const store = {
      year: signal(2026),
      hasPreviousYear: signal(true),
      hasNextYear: signal(true),
      planId: signal('plan-2026'),
      planMissing: signal(false),
      items: signal([item]),
      overview: signal(overview),
      subcategorySuggestions: vi.fn(() => []),
      loading: signal(false),
      loadError: signal(null),
      load: vi.fn(async () => undefined),
      removeItem: vi.fn(async () => undefined),
      restoreItem: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: MatSnackBar, useValue: snackBar }],
    });
    // The dashboard provides a store of its own, replace that with the fake.
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('button[aria-label="Eintrag Kino löschen"]')!
      .click();
    await fixture.whenStable();

    expect(store.removeItem).toHaveBeenCalledWith('1');
    expect(snackBar.open).toHaveBeenCalledWith('„Kino" gelöscht', 'Rückgängig', expect.anything());

    undo.next();
    expect(store.restoreItem).toHaveBeenCalledWith('plan-2026', item);
  });

  // PB-1, PB-8
  it('edits an item in a dialog and confirms the change without offering to undo it', async () => {
    const snackBar = { open: vi.fn() };
    const store = {
      year: signal(2026),
      hasPreviousYear: signal(true),
      hasNextYear: signal(true),
      planId: signal('plan-2026'),
      planMissing: signal(false),
      items: signal([item]),
      overview: signal(overview),
      subcategorySuggestions: vi.fn(() => []),
      loading: signal(false),
      loadError: signal(null),
      load: vi.fn(async () => undefined),
      changeItem: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: MatSnackBar, useValue: snackBar }],
    });
    // The dashboard provides a store of its own, replace that with the fake.
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('button[aria-label="Eintrag Kino bearbeiten"]')!
      .click();
    await fixture.whenStable();
    const dialog = document.querySelector('app-edit-item-dialog')!;
    const amount = dialog.querySelector<HTMLInputElement>('input[formcontrolname="amount"]')!;
    expect(amount.value).toBe('12.5');
    amount.value = '15';
    amount.dispatchEvent(new Event('input'));
    dialog.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();

    expect(store.changeItem).toHaveBeenCalledWith('1', {
      description: 'Kino',
      amount: '15',
      period: 'YEARLY',
      category: 'FREIZEIT_SPASS',
    });
    // The dialog reports the saved change only once its closing animation has finished.
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith('Eintrag geändert', undefined, { duration: 3000 }),
    );
  });

  // UK-14
  it('saves a new order of the groups and shows why saving it failed', async () => {
    const snackBar = { open: vi.fn() };
    const store = {
      year: signal(2026),
      hasPreviousYear: signal(true),
      hasNextYear: signal(true),
      planId: signal('plan-2026'),
      planMissing: signal(false),
      items: signal([item, { ...item, id: '2', description: 'Tank', subcategory: 'Auto' }]),
      overview: signal<HouseholdPlanOverview>({
        ...overview,
        expenseCategories: [
          {
            category: 'FREIZEIT_SPASS',
            total: '25.00',
            monthlyTotal: '2.08',
            groups: [
              {
                subcategory: 'Auto',
                total: '12.50',
                monthlyTotal: '1.04',
                items: [{ ...item, id: '2', description: 'Tank', subcategory: 'Auto' }],
              },
              { subcategory: 'Sonstiges', total: '12.50', monthlyTotal: '1.04', items: [item] },
            ],
          },
        ],
      }),
      loading: signal(false),
      loadError: signal(null),
      subcategorySuggestions: vi.fn(() => []),
      load: vi.fn(async () => undefined),
      reorderSubcategories: vi.fn(async () => {
        throw new Error('Die Reihenfolge konnte nicht gespeichert werden.');
      }),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: MatSnackBar, useValue: snackBar }],
    });
    // The dashboard provides a store of its own, replace that with the fake.
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('button[aria-label="Unterkategorie Auto verschieben"]')!
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    await fixture.whenStable();

    expect(store.reorderSubcategories).toHaveBeenCalledWith(
      { type: 'EXPENSE', category: 'FREIZEIT_SPASS' },
      ['Sonstiges', 'Auto'],
    );
    expect(snackBar.open).toHaveBeenCalledWith(
      'Die Reihenfolge konnte nicht gespeichert werden.',
      'OK',
      { duration: 6000 },
    );
  });

  it('offers to create the plan of a year without one', async () => {
    const store = {
      year: signal(2030),
      hasPreviousYear: signal(true),
      hasNextYear: signal(true),
      planId: signal(null),
      planMissing: signal(true),
      items: signal([]),
      overview: signal(null),
      loading: signal(false),
      loadError: signal(null),
      load: vi.fn(async () => undefined),
      createPlan: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: MatSnackBar, useValue: { open: vi.fn() } }],
    });
    // The dashboard provides a store of its own, replace that with the fake.
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Für 2030 gibt es noch keinen Haushaltsplan.');
    expect(element.querySelector('app-add-item-form')).toBeNull();

    [...element.querySelectorAll<HTMLButtonElement>('button')]
      .find((button) => button.textContent?.includes('Haushaltsplan anlegen'))!
      .click();
    await fixture.whenStable();

    expect(store.createPlan).toHaveBeenCalled();
  });
});
