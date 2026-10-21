import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSelectHarness } from '@angular/material/select/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, of, throwError } from 'rxjs';
import { BudgetItem, HouseholdPlanOverview } from '../budget.model';
import { BudgetApi } from '../data-access/budget-api';
import {
  CopyPlanDialog,
  CopyPlanDialogData,
  suggestedSourceYear,
  suggestedTargetYear,
} from './copy-plan-dialog';

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
  percentageOfGroup: null,
};

function overview(year: number, items: number): HouseholdPlanOverview {
  return {
    id: `plan-${year}`,
    year,
    totalIncome: '0.00',
    totalExpense: '0.00',
    balance: '0.00',
    monthlyTotalIncome: '0.00',
    monthlyTotalExpense: '0.00',
    monthlyBalance: '0.00',
    expensesExceedIncome: false,
    categoryBreakdowns: [],
    items: Array.from({ length: items }, (_, i) => ({ ...item, id: String(i) })),
    incomeGroups: [],
    expenseCategories: [],
  };
}

// PU-2, PU-13, PU-14
describe('CopyPlanDialog', () => {
  let plans: Record<number, number>;
  let api: { getOverview: ReturnType<typeof vi.fn>; copyPlan: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<CopyPlanDialog>;
  let element: HTMLElement;

  async function open(data: CopyPlanDialogData) {
    api = {
      getOverview: vi.fn((year: number) => of(year in plans ? overview(year, plans[year]) : null)),
      copyPlan: vi.fn(() => of({ id: 'copy', overview: overview(2027, 2) })),
    };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BudgetApi, useValue: api },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(CopyPlanDialog);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function yearInput(): HTMLInputElement {
    return element.querySelector<HTMLInputElement>('input')!;
  }

  async function typeYear(value: string) {
    yearInput().value = value;
    yearInput().dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  function submit(): HTMLButtonElement {
    return element.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  }

  beforeEach(() => {
    plans = { 2025: 1, 2026: 2 };
  });

  it('suggests the following year and names the items taken over', async () => {
    await open({ sourceYear: 2026 });

    expect(element.querySelector('h2')!.textContent).toContain('Plan 2026 als Vorlage übernehmen');
    expect(yearInput().value).toBe('2027');
    expect(element.textContent).toContain('2 Posten werden übernommen.');
    expect(submit().textContent).toContain('Übernehmen');
    expect(element.querySelector('.warning')).toBeNull();
  });

  it('copies into a year without plan and closes with both years', async () => {
    await open({ sourceYear: 2026 });

    submit().click();
    await fixture.whenStable();

    expect(api.copyPlan).toHaveBeenCalledWith('plan-2026', { targetYear: 2027, overwrite: false });
    expect(dialogRef.close).toHaveBeenCalledWith({ sourceYear: 2026, targetYear: 2027 });
  });

  it('warns before overwriting the plan of the target year', async () => {
    await open({ sourceYear: 2026 });

    await typeYear('2025');

    expect(element.querySelector('.warning')!.textContent).toContain(
      'Für 2025 gibt es bereits einen Haushaltsplan mit 1 Posten. Er wird überschrieben',
    );
    expect(submit().textContent).toContain('Überschreiben');
    submit().click();
    await fixture.whenStable();
    expect(api.copyPlan).toHaveBeenCalledWith('plan-2026', { targetYear: 2025, overwrite: true });
  });

  it('accepts neither the year of the source nor years out of range', async () => {
    await open({ sourceYear: 2026 });

    for (const [year, message] of [
      ['2026', 'Bitte ein anderes Jahr als 2026 wählen.'],
      ['2201', 'Bitte ein Jahr von 1900 bis 2200 angeben.'],
      ['', 'Bitte ein Jahr von 1900 bis 2200 angeben.'],
    ]) {
      await typeYear(year);
      yearInput().dispatchEvent(new Event('blur'));
      await fixture.whenStable();
      expect(element.querySelector('mat-error')?.textContent).toContain(message);
      expect(submit().disabled).toBe(true);
    }
    expect(api.copyPlan).not.toHaveBeenCalled();
  });

  it('keeps the dialog open and shows the error if copying fails', async () => {
    await open({ sourceYear: 2026 });
    api.copyPlan.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({ status: 409, error: { type: 'urn:hbt:plan-already-exists' } }),
      ),
    );

    submit().click();
    await fixture.whenStable();

    expect(element.querySelector('.error')!.textContent).toContain(
      'Für dieses Jahr gibt es bereits einen Haushaltsplan.',
    );
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('cannot be submitted while the target year is being looked up', async () => {
    const pending = new Subject<HouseholdPlanOverview | null>();
    await open({ sourceYear: 2026 });
    api.getOverview.mockReturnValue(pending);

    await typeYear('2030');

    expect(submit().disabled).toBe(true);
    pending.next(null);
    await fixture.whenStable();
    expect(submit().disabled).toBe(false);
  });

  // PU-12
  it('lets a year without plan choose the plan to take over', async () => {
    plans = { 2024: 3, 2025: 1, 2030: 4 };
    await open({ targetYear: 2027, sourceYears: [2030, 2025, 2024] });

    expect(element.querySelector('h2')!.textContent).toContain('Plan für 2027 übernehmen');
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    expect(await select.getValueText()).toBe('Plan 2025');
    expect(element.textContent).toContain('1 Posten werden übernommen.');

    await select.open();
    await select.clickOptions({ text: 'Plan 2024' });
    await fixture.whenStable();
    expect(element.textContent).toContain('3 Posten werden übernommen.');

    submit().click();
    await fixture.whenStable();
    expect(api.copyPlan).toHaveBeenCalledWith('plan-2024', { targetYear: 2027, overwrite: false });
    expect(dialogRef.close).toHaveBeenCalledWith({ sourceYear: 2024, targetYear: 2027 });
  });
});

describe('suggested years', () => {
  // PU-2
  it('suggests the following year as target, the year before for 2200', () => {
    expect(suggestedTargetYear(2026)).toBe(2027);
    expect(suggestedTargetYear(2200)).toBe(2199);
  });

  // PU-13
  it('suggests the newest plan before the target year as source, otherwise the oldest after it', () => {
    expect(suggestedSourceYear(2027, [2030, 2025, 2024])).toBe(2025);
    expect(suggestedSourceYear(2020, [2030, 2025])).toBe(2025);
    expect(suggestedSourceYear(2020, [])).toBeNull();
  });
});
