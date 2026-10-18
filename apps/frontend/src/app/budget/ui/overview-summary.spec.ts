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
    expensesExceedIncome: false,
    items: [],
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

  it('shows income, expenses and balance', async () => {
    const element = await render(overview({}));

    const values = [...element.querySelectorAll('.value')].map((v) => v.textContent?.trim());
    expect(values).toEqual(['€3,000.00', '€2,000.00', '€1,000.00']);
    expect(element.querySelectorAll('.value')[2].classList).not.toContain('expense');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('warns when expenses exceed income', async () => {
    const element = await render(
      overview({ totalExpense: '3500.00', balance: '-500.00', expensesExceedIncome: true }),
    );

    const balance = element.querySelectorAll('.value')[2];
    expect(balance.textContent?.trim()).toBe('-€500.00');
    expect(balance.classList).toContain('expense');
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Deine Ausgaben übersteigen deine Einnahmen.',
    );
  });
});
