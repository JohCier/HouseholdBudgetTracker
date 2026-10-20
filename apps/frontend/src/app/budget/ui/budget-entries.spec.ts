import { TestBed } from '@angular/core/testing';
import { BudgetItem, HouseholdPlanOverview, NewItemPreset } from '../budget.model';
import { BudgetEntries, GroupOrder } from './budget-entries';

const salary: BudgetItem = {
  id: 'salary',
  description: 'Gehalt',
  amount: '3000.00',
  period: 'YEARLY',
  monthlyAmount: '250.00',
  annualAmount: '3000.00',
  type: 'INCOME',
  category: null,
  subcategory: 'Gehalt',
  note: null,
};
const fuel: BudgetItem = {
  id: 'fuel',
  description: 'Sprit',
  amount: '80.00',
  period: 'YEARLY',
  monthlyAmount: '6.67',
  annualAmount: '80.00',
  type: 'EXPENSE',
  category: 'LEBENSUNTERHALT',
  subcategory: 'Auto',
  note: 'Diesel',
};
const tax: BudgetItem = { ...fuel, id: 'tax', description: 'Steuer', amount: '20.00', note: null };
const rent: BudgetItem = {
  ...tax,
  id: 'rent',
  description: 'Miete',
  amount: '900.00',
  period: 'YEARLY',
  subcategory: null,
};
const savings: BudgetItem = {
  ...tax,
  id: 'savings',
  description: 'ETF',
  amount: '500.00',
  period: 'YEARLY',
  category: 'SPAREN_INVESTIEREN',
  subcategory: 'Depot',
};

const overview: HouseholdPlanOverview = {
  id: 'plan-2026',
  year: 2026,
  totalIncome: '3000.00',
  totalExpense: '1500.00',
  balance: '1500.00',

  monthlyTotalIncome: '250.00',

  monthlyTotalExpense: '125.00',

  monthlyBalance: '125.00',
  expensesExceedIncome: false,
  categoryBreakdowns: [],
  items: [salary, fuel, tax, rent, savings],
  incomeGroups: [
    { subcategory: 'Gehalt', total: '3000.00', monthlyTotal: '250.00', items: [salary] },
  ],
  expenseCategories: [
    {
      category: 'LEBENSUNTERHALT',
      total: '1000.00',
      monthlyTotal: '83.33',
      groups: [
        { subcategory: 'Auto', total: '100.00', monthlyTotal: '8.33', items: [fuel, tax] },
        { subcategory: 'Sonstiges', total: '900.00', monthlyTotal: '75.00', items: [rent] },
      ],
    },
    {
      category: 'SPAREN_INVESTIEREN',
      total: '500.00',
      monthlyTotal: '41.67',
      groups: [{ subcategory: 'Depot', total: '500.00', monthlyTotal: '41.67', items: [savings] }],
    },
  ],
};

describe('BudgetEntries', () => {
  async function render(value: HouseholdPlanOverview) {
    const fixture = TestBed.createComponent(BudgetEntries);
    fixture.componentRef.setInput('overview', value);
    await fixture.whenStable();
    return { component: fixture.componentInstance, element: fixture.nativeElement as HTMLElement };
  }

  const text = (element: Element | null) => element?.textContent?.replace(/\s+/g, ' ').trim();

  // UK-9, UK-10, UK-11, EG-5, EG-6
  it('shows the incomes first, then the expenses by category, each grouped by subcategory with its sum below', async () => {
    const { element } = await render(overview);

    const areas = [...element.querySelectorAll('section.area')].map((area) =>
      area.getAttribute('aria-label'),
    );
    expect(areas).toEqual(['Einnahmen', 'Ausgaben']);
    const categories = [...element.querySelectorAll('section.section')].map((section) =>
      section.getAttribute('aria-label'),
    );
    expect(categories).toEqual(['Lebensunterhalt', 'Sparen & Investieren']);

    const living = element.querySelector('section[aria-label="Lebensunterhalt"]')!;
    expect(text(living.querySelector('.section-header'))).toBe('Lebensunterhalt');
    expect(text(living.querySelector(':scope > .summary'))).toBe(
      'Summe Lebensunterhalt€83.33€1,000.00',
    );
    const groups = [...living.querySelectorAll('[role="group"]')];
    expect(groups.map((group) => group.getAttribute('aria-label'))).toEqual(['Auto', 'Sonstiges']);
    expect(text(groups[0].querySelector('.group-header'))).toBe('Auto');
    expect([...groups[0].querySelectorAll('.item .description')].map((d) => text(d))).toEqual([
      'Sprit Diesel',
      'Steuer',
    ]);
    expect(groups[0].lastElementChild!.classList).toContain('group-summary');
    expect(text(groups[0].querySelector('.group-summary'))).toBe('Summe Auto€8.33€100.00');

    const expenses = element.querySelector('section[aria-label="Ausgaben"]')!;
    expect(text(expenses.querySelector('.area-header'))).toBe('Ausgaben');
    expect(expenses.lastElementChild!.classList).toContain('area-summary');
    expect(text(expenses.lastElementChild)).toBe('Summe Ausgaben−€125.00−€1,500.00');
  });

  // EG-9, EG-10
  it('offers to add an item to each group, filling in what the group stands for', async () => {
    const { component, element } = await render(overview);
    const presets: NewItemPreset[] = [];
    component.add.subscribe((preset) => presets.push(preset));
    const add = (group: string) =>
      element
        .querySelector<HTMLButtonElement>(`button[aria-label="Posten in ${group} hinzufügen"]`)!
        .click();

    add('Auto');
    add('Sonstiges');
    add('Gehalt');

    expect(presets).toEqual([
      { group: 'Auto', type: 'EXPENSE', category: 'LEBENSUNTERHALT', subcategory: 'Auto' },
      { group: 'Sonstiges', type: 'EXPENSE', category: 'LEBENSUNTERHALT', subcategory: '' },
      { group: 'Gehalt', type: 'INCOME', category: null, subcategory: 'Gehalt' },
    ]);
  });

  // MJ-12
  it('shows each item and each sum per month and per year, the amount as entered standing out', async () => {
    const monthlyRent: BudgetItem = {
      ...rent,
      amount: '850.00',
      period: 'MONTHLY',
      monthlyAmount: '850.00',
      annualAmount: '10200.00',
    };
    const { element } = await render({
      ...overview,
      items: [salary, monthlyRent],
      expenseCategories: [
        {
          category: 'LEBENSUNTERHALT',
          monthlyTotal: '850.00',
          total: '10200.00',
          groups: [
            {
              subcategory: 'Sonstiges',
              monthlyTotal: '850.00',
              total: '10200.00',
              items: [monthlyRent],
            },
          ],
        },
      ],
    });

    expect(text(element.querySelector('.columns'))).toBe('MonatJahr');
    const [rentMonthly, rentYearly] = element.querySelectorAll(
      'section[aria-label="Lebensunterhalt"] .item .amount',
    );
    expect(text(rentMonthly)).toBe('−€850.00');
    expect(rentMonthly.classList).not.toContain('derived');
    expect(text(rentYearly)).toBe('−€10,200.00');
    expect(rentYearly.classList).toContain('derived');
    const [salaryMonthly, salaryYearly] = element.querySelectorAll(
      'section[aria-label="Einnahmen"] .item .amount',
    );
    expect(text(salaryMonthly)).toBe('+€250.00');
    expect(salaryMonthly.classList).toContain('derived');
    expect(salaryYearly.classList).not.toContain('derived');
    expect(text(element.querySelector('section[aria-label="Lebensunterhalt"] > .summary'))).toBe(
      'Summe Lebensunterhalt€850.00€10,200.00',
    );
    expect(text(element.querySelector('section[aria-label="Einnahmen"] .section > .summary'))).toBe(
      'Summe Einnahmen+€250.00+€3,000.00',
    );
  });

  // UK-19
  it('gives the incomes and each category a color of their own', async () => {
    const { element } = await render(overview);

    expect(element.querySelector('section[aria-label="Einnahmen"] .section')!.classList).toContain(
      'INCOME',
    );
    expect(element.querySelector('section[aria-label="Lebensunterhalt"]')!.classList).toContain(
      'LEBENSUNTERHALT',
    );
    expect(
      element.querySelector('section[aria-label="Sparen & Investieren"]')!.classList,
    ).toContain('SPAREN_INVESTIEREN');
  });

  it('shows a hint while the plan has no entries', async () => {
    const { element } = await render({
      ...overview,
      items: [],
      incomeGroups: [],
      expenseCategories: [],
    });

    expect(text(element)).toContain('Noch keine Einträge in diesem Haushaltsplan.');
    expect(element.querySelector('section.area')).toBeNull();
  });

  // UK-14
  it('offers to move groups and items only where there is more than one', async () => {
    const { element } = await render(overview);

    const handles = [...element.querySelectorAll('button.handle')].map((handle) =>
      handle.getAttribute('aria-label'),
    );
    expect(handles).toEqual([
      'Unterkategorie Auto verschieben',
      'Eintrag Sprit verschieben',
      'Eintrag Steuer verschieben',
      'Unterkategorie Sonstiges verschieben',
    ]);
  });

  // UK-14
  it('moves groups and items with the arrow keys and emits the new order', async () => {
    const { component, element } = await render(overview);
    const groupOrders: GroupOrder[] = [];
    const itemOrders: string[][] = [];
    component.reorderGroups.subscribe((order) => groupOrders.push(order));
    component.reorderItems.subscribe((order) => itemOrders.push(order));
    const press = (label: string, key: string) =>
      element
        .querySelector(`button[aria-label="${label}"]`)!
        .dispatchEvent(new KeyboardEvent('keydown', { key }));

    press('Unterkategorie Auto verschieben', 'ArrowDown');
    press('Unterkategorie Auto verschieben', 'ArrowUp');
    press('Eintrag Steuer verschieben', 'ArrowUp');
    press('Eintrag Steuer verschieben', 'ArrowDown');

    expect(groupOrders).toEqual([
      {
        section: { type: 'EXPENSE', category: 'LEBENSUNTERHALT' },
        subcategories: ['Sonstiges', 'Auto'],
      },
    ]);
    expect(itemOrders).toEqual([['tax', 'fuel']]);
  });

  it('emits the item to edit or remove', async () => {
    const { component, element } = await render(overview);
    const edited: BudgetItem[] = [];
    const removed: BudgetItem[] = [];
    component.edit.subscribe((item) => edited.push(item));
    component.remove.subscribe((item) => removed.push(item));

    element
      .querySelector<HTMLButtonElement>('button[aria-label="Eintrag Miete bearbeiten"]')!
      .click();
    element
      .querySelector<HTMLButtonElement>('button[aria-label="Eintrag Gehalt löschen"]')!
      .click();

    expect(edited).toEqual([rent]);
    expect(removed).toEqual([salary]);
  });
});
