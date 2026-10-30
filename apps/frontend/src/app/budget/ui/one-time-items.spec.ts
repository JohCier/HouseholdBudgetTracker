import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OneTimeItemOverview } from '../budget.model';
import { OneTimeItems } from './one-time-items';

const overview: OneTimeItemOverview = {
  year: 2026,
  items: [
    {
      id: '1',
      type: 'EXPENSE',
      description: 'Nebenkostennachzahlung',
      amount: '1200.00',
      date: '2026-03-15',
      note: 'Abrechnung 2025',
    },
    {
      id: '2',
      type: 'INCOME',
      description: 'Steuererstattung',
      amount: '800.00',
      date: '2026-07-10',
      note: null,
    },
  ],
  totalIncome: '800.00',
  totalExpense: '1200.00',
  balance: '-400.00',
  expensesExceedIncome: true,
};

// docs/specs/einmalposten.md, docs/specs/einmalposten-sortierung.md; amounts use the default locale of the tests
describe('OneTimeItems', () => {
  let fixture: ComponentFixture<OneTimeItems>;
  let element: HTMLElement;

  async function render(value: OneTimeItemOverview | null, loadError: string | null = null) {
    fixture = TestBed.createComponent(OneTimeItems);
    fixture.componentRef.setInput('overview', value);
    fixture.componentRef.setInput('loadError', loadError);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function button(label: string): HTMLButtonElement {
    return element.querySelector<HTMLButtonElement>(`button[aria-label="${label}"]`)!;
  }

  // ES-1, ES-2, EP-8, EP-9
  it('shows the incomes above and the expenses below, each with its sum, and the balance under both', async () => {
    await render(overview);

    const areas = [...element.querySelectorAll('section.area')];
    expect(areas.map((area) => area.querySelector('h3')!.textContent!.trim())).toEqual([
      'Einnahmen',
      'Ausgaben',
    ]);
    const [income, expense] = areas.map((area) => [...area.querySelectorAll('[role="listitem"]')]);
    expect(income[0].textContent).toContain('Steuererstattung');
    expect(income[0].querySelector('.date')!.textContent!.trim()).toBe('10.07.2026');
    expect(income[0].querySelector('.note')).toBeNull();
    expect(income[0].querySelector('.amount')!.textContent!.trim()).toBe('+€800.00');
    expect(expense[0].textContent).toContain('Nebenkostennachzahlung');
    expect(expense[0].textContent).not.toContain('Ausgabe');
    expect(expense[0].querySelector('.note')!.textContent).toContain('Abrechnung 2025');
    expect(expense[0].querySelector('.amount')!.textContent!.trim()).toBe('−€1,200.00');
    expect(areas[0].querySelector('.sum')!.textContent).toContain('Summe Einnahmen');
    expect(areas[0].querySelector('.sum')!.textContent).toContain('+€800.00');
    expect(areas[1].querySelector('.sum')!.textContent).toContain('Summe Ausgaben');
    expect(areas[1].querySelector('.sum')!.textContent).toContain('−€1,200.00');
    const balance = element.querySelector('.balance-row')!;
    expect(balance.textContent).toContain('Saldo');
    expect(balance.textContent).toContain('-€400.00');
    expect(balance.querySelector('.amount')!.classList).toContain('negative');
  });

  // ES-3
  it('says so if only one kind of item is missing, with a sum of 0', async () => {
    await render({
      ...overview,
      items: [overview.items[1]],
      totalExpense: '0.00',
      balance: '800.00',
      expensesExceedIncome: false,
    });

    const expense = element.querySelectorAll('section.area')[1];
    expect(expense.textContent).toContain('Keine einmaligen Ausgaben.');
    expect(expense.querySelector('.sum')!.textContent).toContain('€0.00');
    expect(expense.querySelector('.sum')!.textContent).not.toContain('−');
  });

  // ES-4
  it('has a handle only in an area with at least two items', async () => {
    const second = { ...overview.items[0], id: '3', description: 'Autoreparatur' };
    await render({ ...overview, items: [...overview.items, second] });

    expect(button('Einmalposten Steuererstattung verschieben')).toBeNull();
    expect(button('Einmalposten Nebenkostennachzahlung verschieben')).not.toBeNull();
    expect(button('Einmalposten Autoreparatur verschieben')).not.toBeNull();
  });

  // ES-4
  it('asks to move an item within its area with the arrow keys on its handle', async () => {
    const second = { ...overview.items[0], id: '3', description: 'Autoreparatur' };
    await render({ ...overview, items: [...overview.items, second] });
    const move = vi.fn();
    fixture.componentInstance.move.subscribe(move);

    const handle = button('Einmalposten Autoreparatur verschieben');
    handle.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
    handle.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown' }));

    expect(move).toHaveBeenCalledTimes(1);
    expect(move).toHaveBeenCalledWith({ item: second, from: 1, to: 0 });
  });

  // EP-10
  it('says so if the year has no one-time items and offers to enter one', async () => {
    await render({
      ...overview,
      items: [],
      totalIncome: '0.00',
      totalExpense: '0.00',
      balance: '0.00',
      expensesExceedIncome: false,
    });

    expect(element.textContent).toContain('Keine Einmalposten.');
    expect(element.querySelector('section.area')).toBeNull();
    expect(element.textContent).toContain('Einmalposten erfassen');
  });

  it('shows nothing of the items while they load, and why loading failed', async () => {
    await render(null);
    expect(element.textContent).not.toContain('Einmalposten erfassen');

    await render(null, 'Server nicht erreichbar.');
    expect(element.querySelector('[role="alert"]')!.textContent).toContain(
      'Server nicht erreichbar.',
    );
  });

  // EP-7, ES-10
  it('asks the dashboard to enter, change or delete an item', async () => {
    await render(overview);
    const add = vi.fn();
    const edit = vi.fn();
    const remove = vi.fn();
    fixture.componentInstance.add.subscribe(add);
    fixture.componentInstance.edit.subscribe(edit);
    fixture.componentInstance.remove.subscribe(remove);

    [...element.querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent!.includes('Einmalposten erfassen'))!
      .click();
    button('Einmalposten Steuererstattung bearbeiten').click();
    button('Einmalposten Nebenkostennachzahlung löschen').click();

    expect(add).toHaveBeenCalled();
    expect(edit).toHaveBeenCalledWith(overview.items[1]);
    expect(remove).toHaveBeenCalledWith(overview.items[0]);
  });
});
