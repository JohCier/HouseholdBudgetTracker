import { TestBed } from '@angular/core/testing';
import { CategoryBreakdown } from '../budget.model';
import { CategoryBreakdownCard } from './category-breakdown';

describe('CategoryBreakdownCard', () => {
  it('renders each category and flags missed targets depending on their direction', async () => {
    const breakdowns: CategoryBreakdown[] = [
      {
        category: 'LEBENSUNTERHALT',
        total: '1200.00',
        actualPercentageOfIncome: 60,
        targetPercentageOfIncome: 50,
        targetDirection: 'AT_MOST',
        targetMet: false,
      },
      {
        category: 'FREIZEIT_SPASS',
        total: '300.00',
        actualPercentageOfIncome: 15,
        targetPercentageOfIncome: 30,
        targetDirection: 'AT_MOST',
        targetMet: true,
      },
      {
        category: 'SPAREN_INVESTIEREN',
        total: '100.00',
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
    expect(rows[1].classList).not.toContain('missed');
    expect(rows[2].textContent).toContain('mind. 20 %');
    expect(rows[2].textContent).toContain('Ziel nicht erreicht');
    expect(rows[2].classList).toContain('missed');
  });
});
