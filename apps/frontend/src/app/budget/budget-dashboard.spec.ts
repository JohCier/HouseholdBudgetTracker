import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { BudgetDashboard } from './budget-dashboard';
import { BudgetItem, HouseholdPlanOverview, YearlyOverviewEntry } from './budget.model';
import { BudgetApi } from './data-access/budget-api';
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
  important: false,
  savingPotential: null,
  percentageOfGroup: null,
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

  monthlySavingPotential: '0.00',

  savingPotential: '0.00',
  expensesExceedIncome: true,
  categoryBreakdowns: [],
  items: [item],
  incomeGroups: [],
  expenseCategories: [
    {
      category: 'FREIZEIT_SPASS',
      total: '12.50',
      monthlyTotal: '1.04',
      groups: [
        {
          subcategory: 'Sonstiges',
          total: '12.50',
          monthlyTotal: '1.04',
          percentageOfSection: 0,
          items: [item],
        },
      ],
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
      selectYear: vi.fn(async () => undefined),
      removeItem: vi.fn(async () => undefined),
      restoreItem: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: MatSnackBar, useValue: snackBar }],
    });
    // The dashboard provides a store of its own, replace that with the fake.
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', String(store.year()));
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
      selectYear: vi.fn(async () => undefined),
      changeItem: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: MatSnackBar, useValue: snackBar }],
    });
    // The dashboard provides a store of its own, replace that with the fake.
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', String(store.year()));
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
                percentageOfSection: 0,
                total: '12.50',
                monthlyTotal: '1.04',
                items: [{ ...item, id: '2', description: 'Tank', subcategory: 'Auto' }],
              },
              {
                subcategory: 'Sonstiges',
                total: '12.50',
                monthlyTotal: '1.04',
                percentageOfSection: 0,
                items: [item],
              },
            ],
          },
        ],
      }),
      loading: signal(false),
      loadError: signal(null),
      subcategorySuggestions: vi.fn(() => []),
      load: vi.fn(async () => undefined),
      selectYear: vi.fn(async () => undefined),
      reorderSubcategories: vi.fn(async () => {
        throw new Error('Die Reihenfolge konnte nicht gespeichert werden.');
      }),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: MatSnackBar, useValue: snackBar }],
    });
    // The dashboard provides a store of its own, replace that with the fake.
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', String(store.year()));
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
      selectYear: vi.fn(async () => undefined),
      createPlan: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: MatSnackBar, useValue: { open: vi.fn() } }],
    });
    // The dashboard provides a store of its own, replace that with the fake.
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', String(store.year()));
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

  // PU-12, PU-15
  it('takes over the plan shown as template and opens the target year', async () => {
    const snackBar = { open: vi.fn() };
    const dialog = {
      open: vi.fn(() => ({ afterClosed: () => of({ sourceYear: 2026, targetYear: 2027 }) })),
    };
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
      selectYear: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', '2026');
    await fixture.whenStable();

    [...(fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')]
      .find((button) => button.textContent?.includes('Als Vorlage übernehmen'))!
      .click();

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), { data: { sourceYear: 2026 } });
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith('Plan 2026 nach 2027 übernommen.', undefined, {
        duration: 3000,
      }),
    );
    expect(navigate).toHaveBeenCalledWith(['/jahre', 2027]);
  });

  // PL-7, PL-9
  it('deletes the plan shown after asking and shows the yearly overview', async () => {
    const snackBar = { open: vi.fn() };
    const dialog = { open: vi.fn(() => ({ afterClosed: () => of(true) })) };
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
      selectYear: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', '2026');
    await fixture.whenStable();

    [...(fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')]
      .find((button) => button.textContent?.includes('Plan löschen'))!
      .click();

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), { data: { year: 2026 } });
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith('Plan 2026 gelöscht.', undefined, {
        duration: 3000,
      }),
    );
    expect(navigateByUrl).toHaveBeenCalledWith('/');
  });

  // PU-12, PU-15
  it('takes over another plan for a year without one if the user has other plans', async () => {
    const snackBar = { open: vi.fn() };
    const dialog = {
      open: vi.fn(() => ({ afterClosed: () => of({ sourceYear: 2025, targetYear: 2030 }) })),
    };
    const api = {
      getYearlyOverview: vi.fn(() =>
        of({ years: [{ year: 2031 }, { year: 2025 }] as YearlyOverviewEntry[] }),
      ),
    };
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
      selectYear: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: BudgetApi, useValue: api },
      ],
    });
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', '2030');
    await fixture.whenStable();

    [...(fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')]
      .find((button) => button.textContent?.includes('Aus anderem Jahr übernehmen'))!
      .click();

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), {
      data: { targetYear: 2030, sourceYears: [2031, 2025] },
    });
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith('Plan 2025 nach 2030 übernommen.', undefined, {
        duration: 3000,
      }),
    );
    expect(store.load).toHaveBeenCalled();
  });

  // PU-12
  it('offers only to create the plan of a year without one if the user has no other plans', async () => {
    const api = { getYearlyOverview: vi.fn(() => of({ years: [] })) };
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
      selectYear: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: BudgetApi, useValue: api }],
    });
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', '2030');
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(api.getYearlyOverview).toHaveBeenCalled();
    expect(element.textContent).toContain('Haushaltsplan anlegen');
    expect(element.textContent).not.toContain('Aus anderem Jahr übernehmen');
    expect(element.textContent).not.toContain('Als Vorlage übernehmen');
  });

  // JU-11
  it('shows the plan of the year in the address and changes the address with the arrows', async () => {
    const store = {
      year: signal(2026),
      hasPreviousYear: signal(true),
      hasNextYear: signal(true),
      planId: signal(null),
      planMissing: signal(true),
      items: signal([]),
      overview: signal(null),
      loading: signal(false),
      loadError: signal(null),
      selectYear: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', '2026');
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(store.selectYear).toHaveBeenCalledWith(2026);
    expect(element.querySelector('a[href="/"]')?.textContent).toContain('Alle Jahre');

    element.querySelector<HTMLButtonElement>('button[aria-label="Nächstes Jahr"]')!.click();
    expect(navigate).toHaveBeenCalledWith(['/jahre', 2027]);
    element.querySelector<HTMLButtonElement>('button[aria-label="Vorheriges Jahr"]')!.click();
    expect(navigate).toHaveBeenCalledWith(['/jahre', 2025]);

    fixture.componentRef.setInput('year', '2025');
    await fixture.whenStable();
    expect(store.selectYear).toHaveBeenLastCalledWith(2025);
  });

  // HP-1
  it('leads back to the yearly overview for an address without a valid year', async () => {
    const store = {
      year: signal(2026),
      hasPreviousYear: signal(true),
      hasNextYear: signal(true),
      planId: signal(null),
      planMissing: signal(false),
      items: signal([]),
      overview: signal(null),
      loading: signal(false),
      loadError: signal(null),
      selectYear: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    TestBed.overrideComponent(BudgetDashboard, {
      set: { providers: [{ provide: BudgetStore, useValue: store }] },
    });
    const navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(BudgetDashboard);
    fixture.componentRef.setInput('year', '2201');
    await fixture.whenStable();

    expect(navigateByUrl).toHaveBeenCalledWith('/');
    expect(store.selectYear).not.toHaveBeenCalled();
  });
});
