import { TestBed } from '@angular/core/testing';
import { CategoryBreakdown } from '../budget.model';
import { CategoryBreakdownCard } from './category-breakdown';

describe('CategoryBreakdownCard', () => {
  it('renders each category and flags missed targets depending on their direction', async () => {
    const breakdowns: CategoryBreakdown[] = [
      {
        category: 'LEBENSUNTERHALT',
        total: '1200.00',
        unplanned: '0.00',
        actualPercentageOfIncome: 60,
        targetPercentageOfIncome: 50,
        targetDirection: 'AT_MOST',
        targetMet: false,
      },
      {
        category: 'FREIZEIT_SPASS',
        total: '700.00',
        unplanned: '400.00',
        actualPercentageOfIncome: 35,
        targetPercentageOfIncome: 30,
        targetDirection: 'AT_MOST',
        targetMet: false,
      },
      {
        category: 'SPAREN_INVESTIEREN',
        total: '100.00',
        unplanned: '0.00',
        actualPercentageOfIncome: 5,
        targetPercentageOfIncome: 20,
        targetDirection: 'AT_LEAST',
        targetMet: false,
      },
    ];
    const fixture = TestBed.createComponent(CategoryBreakdownCard);
    fixture.componentRef.setInput('breakdowns', breakdowns);
    await fixture.whenStable();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('.row');
    expect(rows.length).toBe(3);
    expect(rows[0].textContent).toContain('Lebensunterhalt');
    expect(rows[0].textContent).toContain('max. 50 %');
    expect(rows[0].textContent).toContain('Ziel überschritten');
    expect(rows[0].classList).toContain('missed');
    expect(rows[1].textContent).toContain('Freizeit & Spaß');
    expect(rows[1].classList).toContain('missed');
    expect(rows[1].textContent).toContain('Ziel überschritten');
    expect(rows[2].textContent).toContain('mind. 20 %');
    expect(rows[2].textContent).toContain('Ziel nicht erreicht');
    expect(rows[2].classList).toContain('missed');
  });

  // AW-10
  it('shows the surplus counted towards a category as unplanned', async () => {
    const breakdown = (
      category: CategoryBreakdown['category'],
      unplanned: string,
    ): CategoryBreakdown => ({
      category,
      total: '600.00',
      unplanned,
      actualPercentageOfIncome: 30,
      targetPercentageOfIncome: 30,
      targetDirection: 'AT_MOST',
      targetMet: true,
    });
    const fixture = TestBed.createComponent(CategoryBreakdownCard);
    fixture.componentRef.setInput('breakdowns', [
      breakdown('LEBENSUNTERHALT', '0.00'),
      breakdown('FREIZEIT_SPASS', '400.00'),
    ]);
    await fixture.whenStable();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('.row');
    expect(rows[0].textContent).not.toContain('nicht verplant');
    expect(rows[1].textContent).toMatch(/davon nicht verplant: \S*400/);
  });
});
