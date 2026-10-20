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

  // MJ-13
  it('shows income, expenses and balance per month and per year', async () => {
    const element = await render(overview({}));

    const amounts = [...element.querySelectorAll('.amount')].map((v) =>
      v.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(amounts).toEqual([
      '€250.00im Monat',
      '€3,000.00im Jahr',
      '€166.67im Monat',
      '€2,000.00im Jahr',
      '€83.33im Monat',
      '€1,000.00im Jahr',
    ]);
    expect(element.querySelectorAll('.value')[5].classList).not.toContain('expense');
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

    const [monthly, yearly] = [...element.querySelectorAll('.value')].slice(4);
    expect(monthly.textContent?.trim()).toBe('-€41.67');
    expect(monthly.classList).toContain('expense');
    expect(yearly.textContent?.trim()).toBe('-€500.00');
    expect(yearly.classList).toContain('expense');
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Deine Ausgaben übersteigen deine Einnahmen.',
    );
  });
});
