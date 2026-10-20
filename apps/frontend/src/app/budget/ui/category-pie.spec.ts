import { TestBed } from '@angular/core/testing';
import { CategoryBreakdown, ExpenseCategory } from '../budget.model';
import { CategoryPieChart } from './category-pie';

const breakdown = (
  category: ExpenseCategory,
  total: string,
  actualPercentageOfIncome: number,
  targetMet = true,
): CategoryBreakdown => ({
  category,
  total,
  unplanned: '0.00',
  actualPercentageOfIncome,
  targetPercentageOfIncome:
    category === 'LEBENSUNTERHALT' ? 50 : category === 'FREIZEIT_SPASS' ? 30 : 20,
  targetDirection: category === 'SPAREN_INVESTIEREN' ? 'AT_LEAST' : 'AT_MOST',
  targetMet,
});

async function render(breakdowns: CategoryBreakdown[]): Promise<HTMLElement> {
  const fixture = TestBed.createComponent(CategoryPieChart);
  fixture.componentRef.setInput('breakdowns', breakdowns);
  await fixture.whenStable();
  return fixture.nativeElement as HTMLElement;
}

describe('CategoryPieChart', () => {
  // TD-2, TD-3, TD-5, TD-9
  it('draws one labelled slice per category and lists shares and targets in the legend', async () => {
    const element = await render([
      breakdown('LEBENSUNTERHALT', '1500.00', 50),
      breakdown('FREIZEIT_SPASS', '600.00', 20),
      breakdown('SPAREN_INVESTIEREN', '900.00', 30),
    ]);

    const slices = [...element.querySelectorAll('.slice')];
    const labels = slices.map((s) => s.getAttribute('aria-label'));
    expect(labels[0]).toMatch(/^Lebensunterhalt: \S*1.500\S*, 50 % des Einkommens$/);
    expect(labels[1]).toMatch(/^Freizeit & Spaß: \S*600\S*, 20 % des Einkommens$/);
    expect(labels[2]).toMatch(/^Sparen & Investieren: \S*900\S*, 30 % des Einkommens$/);
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
      { ...breakdown('SPAREN_INVESTIEREN', '800.00', 40), unplanned: '800.00' },
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
});
