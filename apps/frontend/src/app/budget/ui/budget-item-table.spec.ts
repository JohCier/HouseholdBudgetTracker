import { TestBed } from '@angular/core/testing';
import { BudgetItem } from '../budget.model';
import { BudgetItemTable } from './budget-item-table';

const salary: BudgetItem = {
  id: '1',
  description: 'Gehalt',
  amount: '3000.00',
  type: 'INCOME',
  category: null,
};
const rent: BudgetItem = {
  id: '2',
  description: 'Miete',
  amount: '850.00',
  type: 'EXPENSE',
  category: 'LEBENSUNTERHALT',
};

describe('BudgetItemTable', () => {
  async function render(items: BudgetItem[]) {
    const fixture = TestBed.createComponent(BudgetItemTable);
    fixture.componentRef.setInput('items', items);
    await fixture.whenStable();
    return fixture;
  }

  it('lists incomes and expenses with sign, category and amount', async () => {
    const fixture = await render([salary, rent]);

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(2);
    const cells = (row: Element) =>
      [...row.querySelectorAll('td')].map((cell) => cell.textContent?.trim());
    expect(cells(rows[0])).toEqual(['Einnahme', 'Gehalt', '–', '+€3,000.00', '']);
    expect(cells(rows[1])).toEqual(['Ausgabe', 'Miete', 'Lebensunterhalt', '−€850.00', '']);
    expect(rows[0].querySelector('.amount')!.classList).toContain('income');
    expect(rows[1].querySelector('.amount')!.classList).toContain('expense');
  });

  it('asks to remove the item whose delete button was clicked', async () => {
    const fixture = await render([salary, rent]);
    const removed: BudgetItem[] = [];
    fixture.componentInstance.remove.subscribe((item) => removed.push(item));

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('button[aria-label="Eintrag Miete löschen"]')!
      .click();

    expect(removed).toEqual([rent]);
  });

  it('asks to edit the item whose edit button was clicked', async () => {
    const fixture = await render([salary, rent]);
    const edited: BudgetItem[] = [];
    fixture.componentInstance.edit.subscribe((item) => edited.push(item));

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('button[aria-label="Eintrag Gehalt bearbeiten"]')!
      .click();

    expect(edited).toEqual([salary]);
  });

  it('explains an empty plan instead of showing an empty table', async () => {
    const fixture = await render([]);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('table')).toBeNull();
    expect(element.querySelector('.empty')?.textContent).toContain(
      'Noch keine Einträge in diesem Haushaltsplan.',
    );
  });
});
