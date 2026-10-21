import { TestBed } from '@angular/core/testing';
import { HouseholdPlanOverview } from '../budget.model';
import { OverviewSummary } from './overview-summary';

function overview(changes: Partial<HouseholdPlanOverview>): HouseholdPlanOverview {
  return {
    id: 'plan-2026',
    year: 2026,
    totalIncome: '3000.00',
    totalExpense: '2000.00',
    balance: '1000.00',

    monthlyTotalIncome: '250.00',

    monthlyTotalExpense: '166.67',

    monthlyBalance: '83.33',

    monthlySavingPotential: '0.00',

    savingPotential: '0.00',
    expensesExceedIncome: false,
    items: [],
    incomeGroups: [],
    expenseCategories: [],
    categoryBreakdowns: [],
    ...changes,
  };
}

describe('OverviewSummary', () => {
  async function render(value: HouseholdPlanOverview) {
    const fixture = TestBed.createComponent(OverviewSummary);
    fixture.componentRef.setInput('overview', value);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  // BI-1, BI-2, BI-4, PM-14
  it('shows income, expenses, balance and saving potential in rows, per month and per year in columns', async () => {
    const element = await render(
      overview({ monthlySavingPotential: '35.00', savingPotential: '420.00' }),
    );

    const text = (cell: Element) => cell.textContent?.replace(/\s+/g, ' ').trim();
    expect([...element.querySelectorAll('thead th')].map(text)).toEqual(['Monat', 'Jahr']);
    const rows = [...element.querySelectorAll('tbody tr')].map((row) =>
      [...row.querySelectorAll('th, td')].map(text),
    );
    expect(rows).toEqual([
      ['Gesamteinnahmen', '€250.00', '€3,000.00'],
      ['Gesamtausgaben', '€166.67', '€2,000.00'],
      ['Saldo', '€83.33', '€1,000.00'],
      ['Sparpotential', '€35.00', '€420.00'],
    ]);
    expect(element.querySelector('mat-card-title')?.textContent).toBe('Bilanz');
    expect(element.querySelector('.balance .value.yearly')?.classList).not.toContain('expense');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('warns when expenses exceed income', async () => {
    const element = await render(
      overview({
        totalExpense: '3500.00',
        balance: '-500.00',
        monthlyBalance: '-41.67',
        expensesExceedIncome: true,
      }),
    );

    // BI-3, BI-6
    const monthly = element.querySelector('.balance .value.monthly')!;
    const yearly = element.querySelector('.balance .value.yearly')!;
    expect(monthly.textContent?.trim()).toBe('-€41.67');
    expect(monthly.classList).toContain('expense');
    expect(yearly.textContent?.trim()).toBe('-€500.00');
    expect(yearly.classList).toContain('expense');
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Deine Ausgaben übersteigen deine Einnahmen.',
    );
  });
});
