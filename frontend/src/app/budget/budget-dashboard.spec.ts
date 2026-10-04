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
  bookingDate: '2026-10-04',
};

describe('BudgetDashboard', () => {
  it('offers to undo a deletion', async () => {
    const undo = new Subject<void>();
    const snackBar = { open: vi.fn(() => ({ onAction: () => undo })) };
    const store = {
      month: signal('2026-10'),
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
    expect(store.restoreItem).toHaveBeenCalledWith(item);
  });
});
