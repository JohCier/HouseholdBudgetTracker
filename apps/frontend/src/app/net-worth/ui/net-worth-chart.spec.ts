import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NetWorthHistoryEntry } from '../net-worth.model';
import { NetWorthChart, niceTicks } from './net-worth-chart';

const entry = (
  year: number,
  netWorth: string,
  change: NetWorthHistoryEntry['change'] = null,
): NetWorthHistoryEntry => ({
  year,
  totalAssets: netWorth,
  totalLiabilities: '0.00',
  netWorth,
  overindebted: Number(netWorth) < 0,
  change,
});

// docs/specs/vermoegen.md
describe('NetWorthChart', () => {
  let fixture: ComponentFixture<NetWorthChart>;

  function render(years: NetWorthHistoryEntry[]): HTMLElement {
    fixture = TestBed.createComponent(NetWorthChart);
    fixture.componentRef.setInput('years', years);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  // VS-20: the example of the spec
  it('draws a line with a point per year and a bar per change', () => {
    const element = render([
      entry(2026, '13900.00', { amount: '3400.00', percentage: 32.38 }),
      entry(2025, '10500.00', { amount: '6700.00', percentage: 176.32 }),
      entry(2024, '3800.00', { amount: '-2200.00', percentage: -36.67 }),
      entry(2023, '6000.00'),
    ]);

    expect(element.querySelectorAll('circle.point')).toHaveLength(4);
    expect(element.querySelectorAll('polyline.line')).toHaveLength(1);
    expect(element.querySelectorAll('path.bar.gain')).toHaveLength(2);
    expect(element.querySelectorAll('path.bar.loss')).toHaveLength(1);
    expect(
      element
        .querySelector('text.label')!
        .textContent!.trim()
        .replace(/\u00a0/g, ' '),
    ).toBe('13.900 €');
  });

  // VS-21
  it('interrupts the line where a year is missing but keeps the year on the axis', () => {
    const element = render([entry(2025, '10500.00'), entry(2023, '6000.00')]);

    expect(element.querySelectorAll('polyline.line')).toHaveLength(2);
    expect(element.querySelectorAll('circle.point')).toHaveLength(2);
    expect(
      [...element.querySelectorAll('text.axis')].map((text) => text.textContent!.trim()),
    ).toContain('2024');
  });

  // VS-22
  it('shows the figures of the year pointed at', () => {
    const element = render([
      entry(2025, '10500.00'),
      entry(2026, '13900.00', { amount: '3400.00', percentage: 32.38 }),
    ]);

    element.querySelector('rect.hit[data-year="2026"]')!.dispatchEvent(new Event('pointerenter'));
    fixture.detectChanges();

    const tooltip = element.querySelector('.tooltip')!.textContent!.replace(/\u00a0/g, ' ');
    expect(tooltip).toContain('2026');
    expect(tooltip).toContain('Nettovermögen 13.900,00 €');
    expect(tooltip).toContain('+3.400,00 € (+32,38 %) zum Vorjahr');
  });

  // VS-23
  it('hints that the course needs two years', () => {
    const element = render([entry(2026, '13900.00')]);

    expect(element.querySelectorAll('circle.point')).toHaveLength(1);
    expect(element.textContent).toContain('Ab zwei Jahren zeigt das Diagramm den Verlauf.');
  });

  it('chooses round ticks including zero', () => {
    expect(niceTicks(-2200, 13900)).toEqual([-5000, 0, 5000, 10000, 15000]);
    expect(niceTicks(0, 0)).toEqual([0, 1000]);
    expect(niceTicks(0, 950)).toEqual([0, 250, 500, 750, 1000]);
  });
});
