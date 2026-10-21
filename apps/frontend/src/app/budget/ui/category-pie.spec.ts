import { TestBed } from '@angular/core/testing';
import {
  BudgetItem,
  CategoryBreakdown,
  CategoryItems,
  ExpenseCategory,
  PieSelection,
} from '../budget.model';
import { CategoryPieChart } from './category-pie';

/** A breakdown with the given total per month; the yearly total is twelve times as much (MJ-14). */
const breakdown = (
  category: ExpenseCategory,
  monthlyTotal: string,
  actualPercentageOfIncome: number,
  targetMet = true,
): CategoryBreakdown => ({
  category,
  total: (Number(monthlyTotal) * 12).toFixed(2),
  unplanned: '0.00',
  monthlyTotal,
  monthlyUnplanned: '0.00',
  unplannedPercentageOfTotal: 0,
  actualPercentageOfIncome,
  targetPercentageOfIncome:
    category === 'LEBENSUNTERHALT' ? 50 : category === 'FREIZEIT_SPASS' ? 30 : 20,
  targetDirection: category === 'SPAREN_INVESTIEREN' ? 'AT_LEAST' : 'AT_MOST',
  targetMet,
});

async function render(breakdowns: CategoryBreakdown[]): Promise<HTMLElement> {
  return (await renderPie(breakdowns)).element;
}

async function renderPie(
  breakdowns: CategoryBreakdown[],
  expenseCategories: CategoryItems[] = [],
  selection?: PieSelection,
) {
  const fixture = TestBed.createComponent(CategoryPieChart);
  fixture.componentRef.setInput('breakdowns', breakdowns);
  fixture.componentRef.setInput('expenseCategories', expenseCategories);
  if (selection) {
    fixture.componentRef.setInput('selection', selection);
  }
  await fixture.whenStable();
  return { fixture, element: fixture.nativeElement as HTMLElement };
}

const expense = (
  id: string,
  description: string,
  monthlyAmount: string,
  percentageOfGroup: number,
): BudgetItem => ({
  id,
  description,
  amount: monthlyAmount,
  period: 'MONTHLY',
  monthlyAmount,
  annualAmount: (Number(monthlyAmount) * 12).toFixed(2),
  type: 'EXPENSE',
  category: 'LEBENSUNTERHALT',
  subcategory: null,
  note: null,
  important: false,
  savingPotential: null,
  percentageOfGroup,
});

/** Lebensunterhalt: "Auto" with Sprit and Versicherung, "Miete" with Miete (TK-1). */
const living: CategoryItems = {
  category: 'LEBENSUNTERHALT',
  total: '15000.00',
  monthlyTotal: '1250.00',
  groups: [
    {
      subcategory: 'Auto',
      total: '3000.00',
      monthlyTotal: '250.00',
      percentageOfSection: 20,
      items: [expense('fuel', 'Sprit', '200.00', 80), expense('ins', 'Versicherung', '50.00', 20)],
    },
    {
      subcategory: 'Miete',
      total: '12000.00',
      monthlyTotal: '1000.00',
      percentageOfSection: 80,
      items: [expense('rent', 'Miete', '1000.00', 100)],
    },
  ],
};

const breakdownsWithLiving = (): CategoryBreakdown[] => [
  breakdown('LEBENSUNTERHALT', '1250.00', 41.67),
  breakdown('FREIZEIT_SPASS', '0.00', 0),
  breakdown('SPAREN_INVESTIEREN', '1750.00', 58.33),
];

describe('CategoryPieChart', () => {
  // TD-2, TD-3, TD-5, TD-9, MJ-14
  it('draws one labelled slice per category and lists shares and targets in the legend', async () => {
    const element = await render([
      breakdown('LEBENSUNTERHALT', '1500.00', 50),
      breakdown('FREIZEIT_SPASS', '600.00', 20),
      breakdown('SPAREN_INVESTIEREN', '900.00', 30),
    ]);

    const slices = [...element.querySelectorAll('.slice')];
    const labels = slices.map((s) => s.getAttribute('aria-label'));
    expect(labels[0]).toMatch(
      /^Lebensunterhalt: \S*1.500\S* im Monat, 50 % des Einkommens; Lebensunterhalt anzeigen$/,
    );
    expect(labels[1]).toMatch(/^Freizeit & Spaß: \S*600\S* im Monat, 20 % des Einkommens;/);
    expect(labels[2]).toMatch(/^Sparen & Investieren: \S*900\S* im Monat, 30 % des Einkommens;/);
    // Half the circle for living costs: from twelve to six o'clock
    expect(slices[0].getAttribute('d')).toBe('M 0 0 L 0 -80 A 80 80 0 0 1 0 80 Z');
    const legend = [...element.querySelectorAll('.legend-item')].map((i) => i.textContent);
    expect(legend[0]).toContain('50 % von max. 50 %');
    expect(legend[1]).toContain('20 % von max. 30 %');
    expect(legend[2]).toContain('30 % von mind. 20 %');
    expect(element.textContent).not.toMatch(/Ziel (überschritten|nicht erreicht)/);
  });

  // TD-5, TD-6
  it('leaves out empty categories from the pie but keeps them in the legend', async () => {
    const element = await render([
      breakdown('LEBENSUNTERHALT', '1200.00', 60, false),
      breakdown('FREIZEIT_SPASS', '0.00', 0),
      {
        ...breakdown('SPAREN_INVESTIEREN', '800.00', 40),
        unplanned: '9600.00',
        monthlyUnplanned: '800.00',
        unplannedPercentageOfTotal: 0,
      },
    ]);

    expect(element.querySelectorAll('.slice').length).toBe(2);
    expect(element.querySelector('.slice.FREIZEIT_SPASS')).toBeNull();
    const items = element.querySelectorAll('.legend-item');
    expect(items.length).toBe(3);
    expect(items[0].textContent).toMatch(/60 % von max\. 50 %\s+· Ziel überschritten/);
    expect(items[0].classList).toContain('missed');
    expect(items[2].textContent).toMatch(/davon nicht verplant: \S*800/);
    expect(items[1].textContent).toMatch(/Freizeit & Spaß\S*0.00/);
  });

  // TD-4
  it('fills the whole circle with the only category that has a total', async () => {
    const element = await render([
      breakdown('LEBENSUNTERHALT', '100.00', 0, false),
      breakdown('FREIZEIT_SPASS', '0.00', 0),
      breakdown('SPAREN_INVESTIEREN', '0.00', 0),
    ]);

    const slices = element.querySelectorAll('.slice');
    expect(slices.length).toBe(1);
    expect(slices[0].tagName.toLowerCase()).toBe('circle');
    expect(slices[0].classList).toContain('LEBENSUNTERHALT');
  });

  // TD-7
  it('shows an empty circle with a hint when there are no totals', async () => {
    const element = await render([
      breakdown('LEBENSUNTERHALT', '0.00', 0),
      breakdown('FREIZEIT_SPASS', '0.00', 0),
      breakdown('SPAREN_INVESTIEREN', '0.00', 0),
    ]);

    expect(element.querySelectorAll('.slice').length).toBe(0);
    expect(element.querySelector('.empty')).not.toBeNull();
    expect(element.textContent).toContain('Noch keine Ausgaben oder Einnahmen');
    expect(element.querySelectorAll('.legend-item').length).toBe(3);
  });

  // TK-1, TK-6 to TK-8, TK-10
  it('shows the groups of a category with their shares and the category above them', async () => {
    const { element } = await renderPie(breakdownsWithLiving(), [living], {
      level: 'category',
      category: 'LEBENSUNTERHALT',
    });

    const slices = [...element.querySelectorAll<SVGElement>('.slice')];
    expect(slices.length).toBe(2);
    // One fifth of the circle for "Auto", in the full category color, "Miete" in a lighter shade
    expect(slices[0].getAttribute('d')).toBe('M 0 0 L 0 -80 A 80 80 0 0 1 76.085 -24.721 Z');
    expect(slices[0].style.fill).toContain('--app-lebensunterhalt-color');
    expect(slices[1].style.fill).toContain('color-mix');
    expect(element.querySelector('.selection-summary')?.textContent).toMatch(
      /Lebensunterhalt\s*\S*1.250\S*\s*41.67 % des Einkommens von max. 50 %/,
    );
    const legend = [...element.querySelectorAll('.legend-item')].map((i) => i.textContent);
    expect(legend[0]).toMatch(/Auto\s*\S*250.00\S* · 20 %/);
    expect(legend[1]).toMatch(/Miete\s*\S*1.000.00\S* · 80 %/);
  });

  // TK-1, TK-6, TK-8
  it('shows the items of a group with their shares', async () => {
    const { element } = await renderPie(breakdownsWithLiving(), [living], {
      level: 'group',
      category: 'LEBENSUNTERHALT',
      subcategory: 'Auto',
    });

    expect(element.querySelectorAll('.slice').length).toBe(2);
    expect(element.querySelector('.selection-summary')?.textContent).toMatch(
      /Lebensunterhalt › Auto\s*\S*250.00/,
    );
    const legend = [...element.querySelectorAll('.legend-item')].map((i) => i.textContent);
    expect(legend[0]).toMatch(/Sprit\s*\S*200.00\S* · 80 %/);
    expect(legend[1]).toMatch(/Versicherung\s*\S*50.00\S* · 20 %/);
    // Items lead nowhere further (TK-4)
    expect(element.querySelector('.slice.selectable')).toBeNull();
  });

  // TK-4
  it('opens a category and a group by clicking their slices and goes back one level', async () => {
    const { fixture, element } = await renderPie(breakdownsWithLiving(), [living]);

    element
      .querySelector<SVGElement>('.slice.LEBENSUNTERHALT')!
      .dispatchEvent(new MouseEvent('click'));
    await fixture.whenStable();
    expect(fixture.componentInstance.selection()).toEqual({
      level: 'category',
      category: 'LEBENSUNTERHALT',
    });

    element.querySelector<SVGElement>('.slice')!.dispatchEvent(new MouseEvent('click'));
    await fixture.whenStable();
    expect(fixture.componentInstance.selection()).toEqual({
      level: 'group',
      category: 'LEBENSUNTERHALT',
      subcategory: 'Auto',
    });

    element.querySelector<HTMLButtonElement>('button.back')!.click();
    await fixture.whenStable();
    expect(fixture.componentInstance.selection()).toEqual({
      level: 'category',
      category: 'LEBENSUNTERHALT',
    });

    element.querySelector<HTMLButtonElement>('button.back')!.click();
    await fixture.whenStable();
    expect(fixture.componentInstance.selection()).toEqual({ level: 'rule' });
    expect(element.querySelector('button.back')).toBeNull();
  });

  // TK-5
  it('falls back to the category when the selected group has no items anymore', async () => {
    const { element } = await renderPie(breakdownsWithLiving(), [living], {
      level: 'group',
      category: 'LEBENSUNTERHALT',
      subcategory: 'Boot',
    });

    expect(element.querySelector('.selection-summary .name')?.textContent?.trim()).toBe(
      'Lebensunterhalt',
    );
    expect(element.querySelectorAll('.legend-item').length).toBe(2);
  });

  // TK-9, TK-10
  it('adds the unplanned income of savings as a grey slice of its own', async () => {
    const savings: CategoryBreakdown = {
      ...breakdown('SPAREN_INVESTIEREN', '3000.00', 100),
      unplanned: '30000.00',
      monthlyUnplanned: '2500.00',
      unplannedPercentageOfTotal: 83.33,
    };
    const { element } = await renderPie(
      [savings],
      [
        {
          category: 'SPAREN_INVESTIEREN',
          total: '6000.00',
          monthlyTotal: '500.00',
          groups: [
            {
              subcategory: 'Investieren',
              total: '6000.00',
              monthlyTotal: '500.00',
              percentageOfSection: 16.67,
              items: [expense('etf', 'ETF', '500.00', 100)],
            },
          ],
        },
      ],
      { level: 'category', category: 'SPAREN_INVESTIEREN' },
    );

    const slices = [...element.querySelectorAll<SVGElement>('.slice')];
    expect(slices.length).toBe(2);
    expect(slices[1].style.fill).toContain('--mat-sys-outline');
    const legend = [...element.querySelectorAll('.legend-item')].map((i) => i.textContent);
    expect(legend[0]).toMatch(/Investieren\s*\S*500.00\S* · 16.67 %/);
    expect(legend[1]).toMatch(/nicht verplant\s*\S*2.500.00\S* · 83.33 %/);
  });

  // TK-11
  it('shows an empty circle for a category without expenses', async () => {
    const { element } = await renderPie(breakdownsWithLiving(), [living], {
      level: 'category',
      category: 'FREIZEIT_SPASS',
    });

    expect(element.querySelectorAll('.slice').length).toBe(0);
    expect(element.textContent).toContain('Keine Ausgaben in Freizeit & Spaß');
  });
  // TK-14
  it('lights up the line in the legend of the slice under the mouse', async () => {
    const { fixture, element } = await renderPie(breakdownsWithLiving(), [living]);
    const highlighted = () =>
      [...element.querySelectorAll('.legend-item.highlighted')].map((i) =>
        i.querySelector('.name')?.textContent?.trim(),
      );

    const slice = element.querySelector<SVGElement>('.slice.SPAREN_INVESTIEREN')!;
    slice.dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();
    expect(highlighted()).toEqual(['Sparen & Investieren']);
    expect(slice.classList).toContain('highlighted');

    slice.dispatchEvent(new MouseEvent('mouseleave'));
    await fixture.whenStable();
    expect(highlighted()).toEqual([]);
  });

  // TK-14
  it('lights up groups and items too and forgets the highlight when the selection changes', async () => {
    const { fixture, element } = await renderPie(breakdownsWithLiving(), [living], {
      level: 'category',
      category: 'LEBENSUNTERHALT',
    });
    const highlighted = () =>
      [...element.querySelectorAll('.legend-item.highlighted')].map((i) =>
        i.querySelector('.name')?.textContent?.trim(),
      );

    element.querySelectorAll<SVGElement>('.slice')[1].dispatchEvent(new FocusEvent('focus'));
    await fixture.whenStable();
    expect(highlighted()).toEqual(['Miete']);

    // A tap on a touch screen enters the slice and opens it at once
    const auto = element.querySelectorAll<SVGElement>('.slice')[0];
    auto.dispatchEvent(new MouseEvent('mouseenter'));
    auto.dispatchEvent(new MouseEvent('click'));
    await fixture.whenStable();
    expect(highlighted()).toEqual([]);

    element.querySelectorAll<SVGElement>('.slice')[1].dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();
    expect(highlighted()).toEqual(['Versicherung']);
  });
});
