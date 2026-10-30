import { TestBed } from '@angular/core/testing';
import { SavingPotentialSummary } from './saving-potential-summary';

const text = (cell: Element) => cell.textContent?.replace(/\s+/g, ' ').trim();

describe('SavingPotentialSummary', () => {
  // SU-1, SU-4, SU-6
  it('lists the subcategories in the given order with their saving potential per month and per year', async () => {
    const fixture = TestBed.createComponent(SavingPotentialSummary);
    fixture.componentRef.setInput('sums', [
      { subcategory: 'Hobbys', monthlyAmount: '30.00', amount: '360.00' },
      { subcategory: 'Sonstiges', monthlyAmount: '4.00', amount: '48.00' },
    ]);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('mat-card-title')?.textContent).toBe(
      'Sparpotential je Unterkategorie',
    );
    const table = element.querySelector('table')!;
    expect(table.getAttribute('aria-labelledby')).toBe('saving-potential-title');
    expect([...table.querySelectorAll('thead th')].map(text)).toEqual(['Monat', 'Jahr']);
    expect(
      [...table.querySelectorAll('tbody tr')].map((row) =>
        [...row.querySelectorAll('th, td')].map(text),
      ),
    ).toEqual([
      ['Hobbys', '€30.00', '€360.00'],
      ['Sonstiges', '€4.00', '€48.00'],
    ]);
  });

  // AP-12
  it('scrolls a long list within the box and shows a hint instead of an empty one', async () => {
    const fixture = TestBed.createComponent(SavingPotentialSummary);
    fixture.componentRef.setInput(
      'sums',
      Array.from({ length: 12 }, (_, i) => ({
        subcategory: `Gruppe ${i + 1}`,
        monthlyAmount: '10.00',
        amount: '120.00',
      })),
    );
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const scroll = element.querySelector<HTMLElement>('.scroll')!;
    expect(getComputedStyle(scroll).overflowY).toBe('auto');
    expect(getComputedStyle(scroll).maxHeight).toBe('320px');

    fixture.componentRef.setInput('sums', []);
    await fixture.whenStable();
    expect(element.querySelector('table')).toBeNull();
    expect(text(element.querySelector('.empty')!)).toBe('Noch kein Sparpotential markiert');
  });
});
