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

async function render(list: 'savingPotential' | 'expenses', items: TopItem[]) {
  const fixture = TestBed.createComponent(TopItems);
  fixture.componentRef.setInput('list', list);
  fixture.componentRef.setInput('items', items);
  await fixture.whenStable();
  return fixture;
}

function rows(element: HTMLElement) {
  return [...element.querySelectorAll('tbody tr')].map((row) =>
    [...row.querySelectorAll('th, td')].map(text),
  );
}

describe('TopItems', () => {
  // AP-11, TP-8, TP-9
  it('shows the saving potentials with rank, subcategory and the amount of the expense', async () => {
    const fixture = await render('savingPotential', savingPotentials);
    const element = fixture.nativeElement as HTMLElement;

    expect(text(element.querySelector('mat-card-title')!)).toBe('Top 5 Sparpotential');
    expect(element.querySelector('mat-button-toggle')).toBeNull();
    expect([...element.querySelectorAll('thead th')].map(text)).toEqual(['Monat', 'Jahr']);
    expect(rows(element)).toEqual([
      ['1.Fitnessstudio Hobbys · von €30.00', '€30.00', '€360.00'],
      ['2.Streaming Abonnements · von €15.00', '€5.00', '€60.00'],
    ]);
  });

  // AP-11, TP-8
  it('shows the highest expenses without the amount of the expense', async () => {
    const fixture = await render('expenses', expenses);
    const element = fixture.nativeElement as HTMLElement;

    expect(text(element.querySelector('mat-card-title')!)).toBe('Top 5 Ausgaben');
    expect(rows(element)).toEqual([['1.Miete Sonstiges', '€1,000.00', '€12,000.00']]);
  });

  // TP-10
  it('shows a hint instead of an empty list', async () => {
    const saving = (await render('savingPotential', [])).nativeElement as HTMLElement;
    expect(saving.querySelector('table')).toBeNull();
    expect(text(saving.querySelector('.empty')!)).toBe('Noch kein Sparpotential markiert');

    const expense = (await render('expenses', [])).nativeElement as HTMLElement;
    expect(text(expense.querySelector('.empty')!)).toBe('Noch keine Ausgaben');
  });
});
