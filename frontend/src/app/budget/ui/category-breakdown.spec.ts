import { TestBed } from '@angular/core/testing';
import { CategoryBreakdown } from '../budget.model';
import { CategoryBreakdownCard } from './category-breakdown';

describe('CategoryBreakdownCard', () => {
  it('renders each category and flags exceeded targets', async () => {
    const breakdowns: CategoryBreakdown[] = [
      { category: 'LEBENSUNTERHALT', total: 1200, actualPercentageOfIncome: 60, targetPercentageOfIncome: 50, withinTarget: false },
      { category: 'FREIZEIT_SPASS', total: 300, actualPercentageOfIncome: 15, targetPercentageOfIncome: 30, withinTarget: true },
    ];
    const fixture = TestBed.createComponent(CategoryBreakdownCard);
    fixture.componentRef.setInput('breakdowns', breakdowns);
    await fixture.whenStable();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('.row');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Lebensunterhalt');
    expect(rows[0].textContent).toContain('Ziel überschritten');
    expect(rows[0].classList).toContain('exceeded');
    expect(rows[1].textContent).toContain('Freizeit & Spaß');
    expect(rows[1].classList).not.toContain('exceeded');
  });
});
