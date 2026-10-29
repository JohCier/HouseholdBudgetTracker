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

// docs/specs/einmalposten.md; amounts use the default locale of the tests
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

  // EP-8, EP-9
  it('lists the items in the given order with day, note, type and signed amount, and their totals', async () => {
    await render(overview);

    const items = [...element.querySelectorAll('[role="listitem"]')];
    expect(items.map((item) => item.querySelector('.date')!.textContent!.trim())).toEqual([
      '15.03.2026',
      '10.07.2026',
    ]);
    expect(items[0].textContent).toContain('Nebenkostennachzahlung');
    expect(items[0].querySelector('.note')!.textContent).toContain('Abrechnung 2025');
    expect(items[0].textContent).toContain('Ausgabe');
    expect(items[0].querySelector('.amount')!.textContent!.trim()).toBe('−€1,200.00');
    expect(items[1].querySelector('.note')).toBeNull();
    expect(items[1].querySelector('.amount')!.textContent!.trim()).toBe('+€800.00');
    const totals = element.querySelector('.totals')!;
    expect(totals.textContent).toContain('+€800.00');
    expect(totals.textContent).toContain('−€1,200.00');
    expect(totals.querySelector('.balance .amount')!.classList).toContain('negative');
    expect(totals.querySelector('.balance')!.textContent).toContain('-€400.00');
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
    expect(element.querySelector('.totals')).toBeNull();
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

  // EP-7, EP-17
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
