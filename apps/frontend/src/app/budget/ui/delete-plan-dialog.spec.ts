import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { BudgetItem, HouseholdPlanOverview } from '../budget.model';
import { BudgetApi } from '../data-access/budget-api';
import { DeletePlanDialog, deletedMessage } from './delete-plan-dialog';

const item: BudgetItem = {
  id: '1',
  description: 'Gehalt',
  amount: '3000.00',
  period: 'YEARLY',
  monthlyAmount: '250.00',
  annualAmount: '3000.00',
  type: 'INCOME',
  category: null,
  subcategory: null,
  note: null,
  important: false,
  savingPotential: null,
  percentageOfGroup: null,
};

function overview(items: number): HouseholdPlanOverview {
  return {
    id: 'plan-2026',
    year: 2026,
    totalIncome: '0.00',
    totalExpense: '0.00',
    balance: '0.00',
    monthlyTotalIncome: '0.00',
    monthlyTotalExpense: '0.00',
    monthlyBalance: '0.00',
    monthlySavingPotential: '0.00',
    savingPotential: '0.00',
    savingPotentialsBySubcategory: [],
    topSavingPotentials: [],
    topExpenses: [],
    expensesExceedIncome: false,
    categoryBreakdowns: [],
    items: Array.from({ length: items }, (_, i) => ({ ...item, id: String(i) })),
    incomeGroups: [],
    expenseCategories: [],
  };
}

// PL-8, PL-9
describe('DeletePlanDialog', () => {
  let api: { getOverview: ReturnType<typeof vi.fn>; deletePlan: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<DeletePlanDialog>;
  let element: HTMLElement;

  async function open(plan: HouseholdPlanOverview | null) {
    api = {
      getOverview: vi.fn(() => of(plan)),
      deletePlan: vi.fn(() => of(undefined)),
    };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BudgetApi, useValue: api },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { year: 2026 } },
      ],
    });
    fixture = TestBed.createComponent(DeletePlanDialog);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function button(label: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button')].find(
      (b) => b.textContent?.trim() === label,
    )!;
  }

  it('names the year and the number of items', async () => {
    await open(overview(12));

    expect(api.getOverview).toHaveBeenCalledWith(2026);
    expect(element.querySelector('h2')!.textContent).toContain('Plan 2026 löschen');
    expect(element.textContent).toContain(
      'Der Haushaltsplan 2026 mit 12 Posten wird gelöscht. Das lässt sich nicht rückgängig machen.',
    );
    expect(button('Abbrechen')).toBeTruthy();
  });

  it('names 0 items for an empty plan', async () => {
    await open(overview(0));

    expect(element.textContent).toContain('Der Haushaltsplan 2026 mit 0 Posten wird gelöscht.');
  });

  it('deletes the plan and closes', async () => {
    await open(overview(2));

    button('Löschen').click();
    await fixture.whenStable();

    expect(api.deletePlan).toHaveBeenCalledWith('plan-2026');
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('stays open and shows the message if deleting fails', async () => {
    await open(overview(2));
    api.deletePlan.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' })),
    );

    button('Löschen').click();
    await fixture.whenStable();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
  });

  it('offers no deletion if the plan is gone already', async () => {
    await open(null);

    expect(button('Löschen').disabled).toBe(true);
    expect(element.textContent).toContain('Für 2026 gibt es keinen Haushaltsplan mehr.');
  });

  it('confirms the deletion with the year', () => {
    expect(deletedMessage(2026)).toBe('Plan 2026 gelöscht.');
  });
});
