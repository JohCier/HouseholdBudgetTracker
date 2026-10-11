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
};

describe('BudgetDashboard', () => {
  it('offers to undo a deletion', async () => {
    const undo = new Subject<void>();
    const snackBar = { open: vi.fn(() => ({ onAction: () => undo })) };
    const store = {
      year: signal(2026),
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
      providers: [
        { provide: BudgetStore, useValue: store },
        { provide: MatSnackBar, useValue: snackBar },
      ],
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

  it('offers to create the plan of a year without one', async () => {
    const store = {
      year: signal(2030),
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
      providers: [
        { provide: BudgetStore, useValue: store },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
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
