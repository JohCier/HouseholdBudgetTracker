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
        monthlyTotal: '100.00',
        monthlyUnplanned: '0.00',
        unplannedPercentageOfTotal: 0,
        actualPercentageOfIncome: 60,
        targetPercentageOfIncome: 50,
        targetDirection: 'AT_MOST',
        targetMet: false,
      },
      {
        category: 'FREIZEIT_SPASS',
        total: '700.00',
        unplanned: '0.00',
        monthlyTotal: '58.33',
        monthlyUnplanned: '0.00',
        unplannedPercentageOfTotal: 0,
        actualPercentageOfIncome: 35,
        targetPercentageOfIncome: 30,
        targetDirection: 'AT_MOST',
        targetMet: false,
      },
      {
        category: 'SPAREN_INVESTIEREN',
        total: '100.00',
        unplanned: '0.00',
        monthlyTotal: '8.33',
        monthlyUnplanned: '0.00',
        unplannedPercentageOfTotal: 0,
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

  // MJ-14
  it('shows the totals per month', async () => {
    const fixture = TestBed.createComponent(CategoryBreakdownCard);
    fixture.componentRef.setInput('breakdowns', [
      {
        category: 'LEBENSUNTERHALT',
        total: '18000.00',
        unplanned: '0.00',
        monthlyTotal: '1500.00',
        monthlyUnplanned: '0.00',
        unplannedPercentageOfTotal: 0,
        actualPercentageOfIncome: 50,
        targetPercentageOfIncome: 50,
        targetDirection: 'AT_MOST',
        targetMet: true,
      },
    ]);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('mat-card-subtitle')?.textContent).toContain('je Monat');
    expect(element.querySelector('.row .total')?.textContent).toMatch(/1.500/);
    expect(element.textContent).not.toMatch(/18.000/);
  });

  // AW-10, MJ-14
  it('shows the surplus counted towards a category as unplanned', async () => {
    const breakdown = (
      category: CategoryBreakdown['category'],
      monthlyUnplanned: string,
    ): CategoryBreakdown => ({
      category,
      total: '600.00',
      unplanned: (Number(monthlyUnplanned) * 12).toFixed(2),
      monthlyTotal: '50.00',
      monthlyUnplanned,
      unplannedPercentageOfTotal: 0,
      actualPercentageOfIncome: 30,
      targetPercentageOfIncome: 30,
      targetDirection: 'AT_MOST',
      targetMet: true,
    });
    const fixture = TestBed.createComponent(CategoryBreakdownCard);
    fixture.componentRef.setInput('breakdowns', [
      breakdown('FREIZEIT_SPASS', '0.00'),
      breakdown('SPAREN_INVESTIEREN', '400.00'),
    ]);
    await fixture.whenStable();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('.row');
    expect(rows[0].textContent).not.toContain('nicht verplant');
    expect(rows[1].textContent).toMatch(/davon nicht verplant: \S*400/);
  });

  // TD-1
  it('shows the bars by default and switches to the pie chart and back', async () => {
    const fixture = TestBed.createComponent(CategoryBreakdownCard);
    fixture.componentRef.setInput('breakdowns', [
      {
        category: 'LEBENSUNTERHALT',
        total: '100.00',
        unplanned: '0.00',
        monthlyTotal: '8.33',
        monthlyUnplanned: '0.00',
        unplannedPercentageOfTotal: 0,
        actualPercentageOfIncome: 10,
        targetPercentageOfIncome: 50,
        targetDirection: 'AT_MOST',
        targetMet: true,
      },
    ]);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const toggle = (label: string) =>
      [...element.querySelectorAll<HTMLButtonElement>('mat-button-toggle button')].find(
        (b) => b.textContent?.trim() === label,
      )!;

    expect(toggle('Balken').getAttribute('aria-checked')).toBe('true');
    expect(element.querySelector('.row')).not.toBeNull();
    expect(element.querySelector('app-category-pie')).toBeNull();

    toggle('Torte').click();
    await fixture.whenStable();
    expect(fixture.componentInstance.view()).toBe('pie');
    expect(element.querySelector('app-category-pie')).not.toBeNull();
    expect(element.querySelector('.row')).toBeNull();

    toggle('Balken').click();
    await fixture.whenStable();
    expect(element.querySelector('.row')).not.toBeNull();
  });
});
