import { TestBed } from '@angular/core/testing';
import { TopItem } from '../budget.model';
import { TopItems } from './top-items';

const text = (cell: Element) => cell.textContent?.replace(/\s+/g, ' ').trim();

const savingPotentials: TopItem[] = [
  {
    id: 'a',
    description: 'Fitnessstudio',
    subcategory: 'Hobbys',
    monthlyAmount: '30.00',
    amount: '360.00',
    itemMonthlyAmount: '30.00',
  },
  {
    id: 'b',
    description: 'Streaming',
    subcategory: 'Abonnements',
    monthlyAmount: '5.00',
    amount: '60.00',
    itemMonthlyAmount: '15.00',
  },
];

const expenses: TopItem[] = [
  {
    id: 'c',
    description: 'Miete',
    subcategory: 'Sonstiges',
    monthlyAmount: '1000.00',
    amount: '12000.00',
    itemMonthlyAmount: '1000.00',
  },
];

async function render(saving: TopItem[], expense: TopItem[]) {
  const fixture = TestBed.createComponent(TopItems);
  fixture.componentRef.setInput('savingPotentials', saving);
  fixture.componentRef.setInput('expenses', expense);
  await fixture.whenStable();
  return fixture;
}

function rows(element: HTMLElement) {
  return [...element.querySelectorAll('tbody tr')].map((row) =>
    [...row.querySelectorAll('th, td')].map(text),
  );
}

function toggle(element: HTMLElement, label: string) {
  return [...element.querySelectorAll<HTMLButtonElement>('mat-button-toggle button')].find(
    (button) => text(button) === label,
  )!;
}

describe('TopItems', () => {
  // TP-7, TP-8, TP-9
  it('shows the saving potentials first, with rank, subcategory and the amount of the expense', async () => {
    const fixture = await render(savingPotentials, expenses);
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('mat-card-title')?.textContent).toBe('Top 5');
    expect(toggle(element, 'Sparpotential').getAttribute('aria-checked')).toBe('true');
    expect([...element.querySelectorAll('thead th')].map(text)).toEqual(['Monat', 'Jahr']);
    expect(rows(element)).toEqual([
      ['1.Fitnessstudio Hobbys · von €30.00', '€30.00', '€360.00'],
      ['2.Streaming Abonnements · von €15.00', '€5.00', '€60.00'],
    ]);
  });

  // TP-7, TP-8
  it('switches to the highest expenses without the amount of the expense', async () => {
    const fixture = await render(savingPotentials, expenses);
    const element = fixture.nativeElement as HTMLElement;

    toggle(element, 'Ausgaben').click();
    await fixture.whenStable();

    expect(fixture.componentInstance.view()).toBe('expenses');
    expect(rows(element)).toEqual([['1.Miete Sonstiges', '€1,000.00', '€12,000.00']]);
  });

  // TP-10
  it('shows a hint instead of an empty list', async () => {
    const fixture = await render([], []);
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('table')).toBeNull();
    expect(text(element.querySelector('.empty')!)).toBe('Noch kein Sparpotential markiert');

    fixture.componentRef.setInput('view', 'expenses');
    await fixture.whenStable();

    expect(text(element.querySelector('.empty')!)).toBe('Noch keine Ausgaben');
  });
});
