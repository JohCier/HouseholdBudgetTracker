import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { OneTimeItem } from '../budget.model';
import { OneTimeItemStore } from '../data-access/one-time-item-store';
import { OneTimeItemDialog, OneTimeItemDialogData } from './one-time-item-dialog';

const backPayment: OneTimeItem = {
  id: '1',
  type: 'EXPENSE',
  description: 'Nebenkostennachzahlung',
  amount: '1200.00',
  date: '2026-03-15',
  note: 'Abrechnung 2025',
};

// docs/specs/einmalposten.md
describe('OneTimeItemDialog', () => {
  let store: { createItem: ReturnType<typeof vi.fn>; changeItem: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<OneTimeItemDialog>;
  let element: HTMLElement;

  afterEach(() => vi.useRealTimers());

  async function open(data: OneTimeItemDialogData) {
    store = { createItem: vi.fn(async () => undefined), changeItem: vi.fn(async () => undefined) };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: OneTimeItemStore, useValue: store },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(OneTimeItemDialog);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function input(name: string): HTMLInputElement {
    return element.querySelector<HTMLInputElement>(`[formcontrolname="${name}"]`)!;
  }

  async function type(name: string, value: string) {
    input(name).value = value;
    input(name).dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function choose(label: string) {
    [...element.querySelectorAll<HTMLButtonElement>('mat-button-toggle button')]
      .find((button) => button.textContent!.trim() === label)!
      .click();
    await fixture.whenStable();
  }

  async function save() {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  // EP-1, EP-11
  it('enters a new item, starting with today if it lies in the year shown', async () => {
    vi.useFakeTimers({ now: new Date(2026, 9, 29), toFake: ['Date'] });
    await open({ year: 2026 });

    expect(element.querySelector('h2')!.textContent).toContain('Einmalposten erfassen');
    expect(input('date').value).toBe('2026-10-29');
    await choose('Einnahme');
    await type('description', ' Bonus ');
    await type('amount', '2000');
    await type('note', 'Jahresprämie');
    await save();

    expect(store.createItem).toHaveBeenCalledWith({
      type: 'INCOME',
      description: 'Bonus',
      amount: '2000',
      date: '2026-10-29',
      note: 'Jahresprämie',
    });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  // EP-11
  it('starts with 1 January of a year shown that is not the current one', async () => {
    vi.useFakeTimers({ now: new Date(2026, 9, 29), toFake: ['Date'] });
    await open({ year: 2027 });

    expect(input('date').value).toBe('2027-01-01');
  });

  // EP-7
  it('changes an item, showing what it has, also into another year and without its note', async () => {
    await open({ year: 2026, item: backPayment });

    expect(element.querySelector('h2')!.textContent).toContain('Einmalposten bearbeiten');
    expect(input('description').value).toBe('Nebenkostennachzahlung');
    expect(input('amount').value).toBe('1200');
    expect(input('date').value).toBe('2026-03-15');
    expect(input('note').value).toBe('Abrechnung 2025');
    await type('date', '2027-01-05');
    await type('note', '  ');
    await save();

    expect(store.changeItem).toHaveBeenCalledWith('1', {
      type: 'EXPENSE',
      description: 'Nebenkostennachzahlung',
      amount: '1200',
      date: '2027-01-05',
    });
  });

  // EP-1, EP-3
  it('saves nothing without description, with an amount that is not positive or a day out of range', async () => {
    await open({ year: 2026 });

    await type('amount', '0');
    await save();
    expect(element.textContent).toContain('Bitte eine Beschreibung angeben.');
    expect(element.textContent).toContain('Bitte einen positiven Betrag angeben.');

    await type('description', 'Bonus');
    await type('amount', '10');
    await type('date', '2201-01-01');
    await save();
    expect(element.textContent).toContain('Bitte ein Datum von 1900 bis 2200 angeben.');

    expect(store.createItem).not.toHaveBeenCalled();
  });

  it('stays open and shows why saving failed', async () => {
    await open({ year: 2026, item: backPayment });
    store.changeItem.mockRejectedValue(new Error('Diesen Einmalposten gibt es nicht (mehr).'));

    await save();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')!.textContent).toContain(
      'Diesen Einmalposten gibt es nicht (mehr).',
    );
  });
});
