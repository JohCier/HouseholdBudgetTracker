import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject } from 'rxjs';
import { BudgetDashboard } from './budget-dashboard';
import { BudgetItem } from './budget.model';
import { BudgetStore } from './data-access/budget-store';

const item: BudgetItem = {
  id: '1',
  description: 'Kino',
  amount: '12.50',
  type: 'EXPENSE',
  category: 'FREIZEIT_SPASS',
  note: null,
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
      overview: signal(null),
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
      overview: signal(null),
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
      category: 'FREIZEIT_SPASS',
    });
    // The dialog reports the saved change only once its closing animation has finished.
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith('Eintrag geändert', undefined, { duration: 3000 }),
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
