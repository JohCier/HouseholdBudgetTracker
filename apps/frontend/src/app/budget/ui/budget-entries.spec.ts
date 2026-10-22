import { TestBed } from '@angular/core/testing';
import { BudgetItem, HouseholdPlanOverview, NewItemPreset } from '../budget.model';
import { BudgetEntries, GroupOrder, ImportantMark } from './budget-entries';

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
  important: false,
  savingPotential: null,
  percentageOfGroup: null,
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
  important: false,
  savingPotential: null,
  percentageOfGroup: null,
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

  monthlySavingPotential: '0.00',

  savingPotential: '0.00',
  savingPotentialsBySubcategory: [],
  expensesExceedIncome: false,
  categoryBreakdowns: [],
  items: [salary, fuel, tax, rent, savings],
  incomeGroups: [
    {
      subcategory: 'Gehalt',
      total: '3000.00',
      monthlyTotal: '250.00',
      percentageOfSection: 0,
      monthlySavingPotential: '0.00',
      items: [salary],
    },
  ],
  expenseCategories: [
    {
      category: 'LEBENSUNTERHALT',
      total: '1000.00',
      monthlyTotal: '83.33',
      groups: [
        {
          subcategory: 'Auto',
          total: '100.00',
          monthlyTotal: '8.33',
          percentageOfSection: 0,
          monthlySavingPotential: '0.00',
          items: [fuel, tax],
        },
        {
          subcategory: 'Sonstiges',
          total: '900.00',
          monthlyTotal: '75.00',
          percentageOfSection: 0,
          monthlySavingPotential: '0.00',
          items: [rent],
        },
      ],
    },
    {
      category: 'SPAREN_INVESTIEREN',
      total: '500.00',
      monthlyTotal: '41.67',
      groups: [
        {
          subcategory: 'Depot',
          total: '500.00',
          monthlyTotal: '41.67',
          percentageOfSection: 0,
          monthlySavingPotential: '0.00',
          items: [savings],
        },
      ],
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

  // SU-10, SU-11, SU-12
  it('shows the saving potential of an expense group left of its monthly sum, nothing without one', async () => {
    const [living, saving] = overview.expenseCategories;
    const { element } = await render({
      ...overview,
      expenseCategories: [
        {
          ...living,
          groups: [{ ...living.groups[0], monthlySavingPotential: '15.00' }, living.groups[1]],
        },
        saving,
      ],
    });

    const summaries = [...element.querySelectorAll('.group-summary')];
    const potentials = summaries.map((summary) => summary.querySelector('.group-saving-potential'));
    expect(potentials.map((potential) => potential !== null)).toEqual([false, true, false, false]);
    const car = summaries[1];
    expect(text(car.querySelector('.summary-label'))).toBe('Summe Auto');
    expect(text(potentials[1])).toBe('Sparpotential Auto: €15.00 im Monat');
    // In the row of the sum, the saving potential comes right before the monthly amount
    const amounts = [...car.querySelectorAll('.group-saving-potential, .amount-column')];
    expect(amounts.map((amount) => amount.classList.contains('monthly'))).toEqual([
      false,
      true,
      false,
    ]);
    expect(element.querySelector('.section > .summary .group-saving-potential')).toBeNull();
    expect(element.querySelector('.area-summary .group-saving-potential')).toBeNull();
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
              percentageOfSection: 0,
              monthlySavingPotential: '0.00',
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

  // PM-1, PM-2, PM-4, PM-9 to PM-11
  it('highlights important items and shows the saving potential of expenses', async () => {
    const importantRent: BudgetItem = { ...rent, important: true, savingPotential: '25.50' };
    const { element } = await render({
      ...overview,
      expenseCategories: [
        {
          ...overview.expenseCategories[0],
          groups: [
            overview.expenseCategories[0].groups[0],
            { ...overview.expenseCategories[0].groups[1], items: [importantRent] },
          ],
        },
      ],
    });

    const rows = [...element.querySelectorAll('.item')];
    const rentRow = rows.find((row) => row.textContent?.includes('Miete'))!;
    const fuelRow = rows.find((row) => row.textContent?.includes('Sprit'))!;
    expect(rentRow.classList).toContain('important');
    expect(fuelRow.classList).not.toContain('important');
    expect(text(rentRow.querySelector('.saving-potential'))).toBe('Sparpotential: €25.50 im Monat');
    expect(fuelRow.querySelector('.saving-potential')).toBeNull();
    const star = rentRow.querySelector(
      'button[aria-label="Markierung wichtig bei Miete entfernen"]',
    );
    expect(star?.getAttribute('aria-pressed')).toBe('true');
    expect(
      fuelRow
        .querySelector('button[aria-label="Sprit als wichtig markieren"]')
        ?.getAttribute('aria-pressed'),
    ).toBe('false');
    expect(rentRow.querySelector('.saving-mark')?.classList).toContain('marked');
    expect(fuelRow.querySelector('.saving-mark')?.classList).not.toContain('marked');
  });

  // PM-2, PM-4
  it('offers a saving potential only for expenses', async () => {
    const { element } = await render(overview);

    expect(
      element.querySelector('button[aria-label="Gehalt als wichtig markieren"]'),
    ).not.toBeNull();
    expect(element.querySelector('button[aria-label="Sparpotential bei Gehalt"]')).toBeNull();
    expect(element.querySelector('button[aria-label="Sparpotential bei Miete"]')).not.toBeNull();
  });

  // PM-4a, PM-4b
  it('emits the item to mark as important or to give a saving potential', async () => {
    const { component, element } = await render(overview);
    const marks: ImportantMark[] = [];
    const potentials: BudgetItem[] = [];
    component.markImportant.subscribe((mark) => marks.push(mark));
    component.savingPotential.subscribe((item) => potentials.push(item));

    element
      .querySelector<HTMLButtonElement>('button[aria-label="Gehalt als wichtig markieren"]')!
      .click();
    element
      .querySelector<HTMLButtonElement>('button[aria-label="Sparpotential bei Miete"]')!
      .click();

    expect(marks).toEqual([{ item: salary, important: true }]);
    expect(potentials).toEqual([rent]);
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
