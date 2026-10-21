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
    savingPotentialsBySubcategory: [],
    expensesExceedIncome: false,
    items: [],
    incomeGroups: [],
    expenseCategories: [],
    categoryBreakdowns: [],
    ...changes,
  };
}

const text = (cell: Element) => cell.textContent?.replace(/\s+/g, ' ').trim();
const headers = (table: Element) => [...table.querySelectorAll('thead th')].map(text);
const rows = (table: Element) =>
  [...table.querySelectorAll('tbody tr')].map((row) =>
    [...row.querySelectorAll('th, td')].map(text),
  );

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

    const sheet = element.querySelector('.side-by-side')!;
    expect(headers(sheet.querySelector('table')!)).toEqual(['Monat', 'Jahr']);
    expect(rows(sheet.querySelector('table')!)).toEqual([
      ['Gesamteinnahmen', '€250.00', '€3,000.00'],
      ['Gesamtausgaben', '€166.67', '€2,000.00'],
      ['Saldo', '€83.33', '€1,000.00'],
      ['Sparpotential', '€35.00', '€420.00'],
    ]);
    expect(element.querySelector('mat-card-title')?.textContent).toBe('Bilanz');
    expect(element.querySelector('.balance .value.yearly')?.classList).not.toContain('expense');
    expect(element.querySelector('tr.total-income th')?.classList).not.toContain('income');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  // BI-5
  it('also provides the month above the year, for a narrow box', async () => {
    const element = await render(
      overview({ monthlySavingPotential: '35.00', savingPotential: '420.00' }),
    );

    const tables = [...element.querySelectorAll('.stacked table')];
    expect(tables.map(headers)).toEqual([['Monat'], ['Jahr']]);
    expect(tables.map(rows)).toEqual([
      [
        ['Gesamteinnahmen', '€250.00'],
        ['Gesamtausgaben', '€166.67'],
        ['Saldo', '€83.33'],
        ['Sparpotential', '€35.00'],
      ],
      [
        ['Gesamteinnahmen', '€3,000.00'],
        ['Gesamtausgaben', '€2,000.00'],
        ['Saldo', '€1,000.00'],
        ['Sparpotential', '€420.00'],
      ],
    ]);
    expect(tables[1].querySelector('.value.income')?.textContent?.trim()).toBe('€3,000.00');
    expect(tables[1].querySelector('.value.expense')?.textContent?.trim()).toBe('€2,000.00');
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

  // SU-6, SU-7
  it('shows the saving potential per subcategory beside the box only if there is any', async () => {
    const without = await render(overview({}));
    expect(without.querySelector('app-saving-potential-summary')).toBeNull();

    const element = await render(
      overview({
        monthlySavingPotential: '45.00',
        savingPotential: '540.00',
        savingPotentialsBySubcategory: [
          { subcategory: 'Hobbys', monthlyAmount: '30.00', amount: '360.00' },
          { subcategory: 'Abonnements', monthlyAmount: '15.00', amount: '180.00' },
        ],
      }),
    );

    const sums = element.querySelector('app-saving-potential-summary table')!;
    expect(rows(sums)).toEqual([
      ['Hobbys', '€30.00', '€360.00'],
      ['Abonnements', '€15.00', '€180.00'],
    ]);
  });
});
