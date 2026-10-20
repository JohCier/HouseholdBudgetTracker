import { TestBed } from '@angular/core/testing';
import { BudgetItem, HouseholdPlanOverview } from '../budget.model';
import { BudgetEntries, GroupOrder } from './budget-entries';

const salary: BudgetItem = {
  id: 'salary',
  description: 'Gehalt',
  amount: '3000.00',
  type: 'INCOME',
  category: null,
  subcategory: 'Gehalt',
  note: null,
};
const fuel: BudgetItem = {
  id: 'fuel',
  description: 'Sprit',
  amount: '80.00',
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
  subcategory: null,
};
const savings: BudgetItem = {
  ...tax,
  id: 'savings',
  description: 'ETF',
  amount: '500.00',
  category: 'SPAREN_INVESTIEREN',
  subcategory: 'Depot',
};

const overview: HouseholdPlanOverview = {
  id: 'plan-2026',
  year: 2026,
  totalIncome: '3000.00',
  totalExpense: '1500.00',
  balance: '1500.00',
  expensesExceedIncome: false,
  categoryBreakdowns: [],
  items: [salary, fuel, tax, rent, savings],
  incomeGroups: [{ subcategory: 'Gehalt', total: '3000.00', items: [salary] }],
  expenseCategories: [
    {
      category: 'LEBENSUNTERHALT',
      total: '1000.00',
      groups: [
        { subcategory: 'Auto', total: '100.00', items: [fuel, tax] },
        { subcategory: 'Sonstiges', total: '900.00', items: [rent] },
      ],
    },
    {
      category: 'SPAREN_INVESTIEREN',
      total: '500.00',
      groups: [{ subcategory: 'Depot', total: '500.00', items: [savings] }],
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

  // UK-9, UK-10, UK-11, UK-12
  it('shows the incomes first, then the expenses by category, each grouped by subcategory with its sum', async () => {
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
    expect(text(living.querySelector('.section-header'))).toContain('€1,000.00');
    const groups = [...living.querySelectorAll('[role="group"]')];
    expect(groups.map((group) => group.getAttribute('aria-label'))).toEqual(['Auto', 'Sonstiges']);
    expect(text(groups[0].querySelector('.group-header'))).toBe('Auto€100.00');
    expect([...groups[0].querySelectorAll('.item .description')].map((d) => text(d))).toEqual([
      'Sprit Diesel',
      'Steuer',
    ]);
    expect(text(element.querySelector('section[aria-label="Ausgaben"] .area-header'))).toContain(
      '€1,500.00',
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
