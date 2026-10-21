import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSelectHarness } from '@angular/material/select/testing';
import { BudgetItem } from '../budget.model';
import { BudgetStore } from '../data-access/budget-store';
import { EditItemDialog } from './edit-item-dialog';

const rent: BudgetItem = {
  id: '2',
  description: 'Miete',
  amount: '850.00',
  period: 'YEARLY',
  monthlyAmount: '70.83',
  annualAmount: '850.00',
  type: 'EXPENSE',
  category: 'LEBENSUNTERHALT',
  subcategory: null,
  note: null,
  percentageOfGroup: null,
};
const salary: BudgetItem = {
  id: '1',
  description: 'Gehalt',
  amount: '3000.00',
  period: 'YEARLY',
  monthlyAmount: '250.00',
  annualAmount: '3000.00',
  type: 'INCOME',
  category: null,
  subcategory: null,
  note: null,
  percentageOfGroup: null,
};

// PB-1, PB-2, PB-3
describe('EditItemDialog', () => {
  let store: {
    changeItem: ReturnType<typeof vi.fn>;
    subcategorySuggestions: ReturnType<typeof vi.fn>;
  };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<EditItemDialog>;
  let element: HTMLElement;

  async function open(item: BudgetItem) {
    store = {
      changeItem: vi.fn(async () => undefined),
      subcategorySuggestions: vi.fn(() => ['Auto']),
    };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BudgetStore, useValue: store },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: item },
      ],
    });
    fixture = TestBed.createComponent(EditItemDialog);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function input(name: string): HTMLInputElement {
    return element.querySelector<HTMLInputElement>(
      `[formcontrolname="${name}"], input[name="${name}"]`,
    )!;
  }

  async function type(name: string, value: string) {
    input(name).value = value;
    input(name).dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function save() {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  it('shows the current values of an expense and saves the changes', async () => {
    await open(rent);
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);

    expect(element.querySelector('h2')?.textContent).toContain('Ausgabe bearbeiten');
    expect(input('description').value).toBe('Miete');
    expect(input('amount').value).toBe('850');
    expect(await select.getValueText()).toBe('Lebensunterhalt');

    await type('description', '  Warmmiete ');
    await type('amount', '900.5');
    await select.open();
    await select.clickOptions({ text: 'Sparen & Investieren' });
    await save();

    expect(store.changeItem).toHaveBeenCalledWith('2', {
      description: 'Warmmiete',
      amount: '900.5',
      period: 'YEARLY',
      category: 'SPAREN_INVESTIEREN',
    });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  // MJ-5, MJ-11
  it('shows the amount as entered with its period and switches the period without converting', async () => {
    await open({
      ...rent,
      period: 'MONTHLY',
      monthlyAmount: '850.00',
      annualAmount: '10200.00',
    });
    const checked = () =>
      element
        .querySelector('[aria-label="Angabe monatlich oder jährlich"] .mat-button-toggle-checked')
        ?.textContent?.trim();

    expect(input('amount').value).toBe('850');
    expect(checked()).toBe('Monatlich');

    [...element.querySelectorAll<HTMLButtonElement>('mat-button-toggle button')]
      .find((button) => button.textContent?.trim() === 'Jährlich')!
      .click();
    await fixture.whenStable();
    await save();

    expect(input('amount').value).toBe('850');
    expect(store.changeItem).toHaveBeenCalledWith('2', {
      description: 'Miete',
      amount: '850',
      period: 'YEARLY',
      category: 'LEBENSUNTERHALT',
    });
  });

  it('changes an income without offering a category', async () => {
    await open(salary);

    expect(element.querySelector('h2')?.textContent).toContain('Einnahme bearbeiten');
    expect(element.querySelector('mat-select')).toBeNull();

    await type('amount', '3100');
    await save();

    expect(store.changeItem).toHaveBeenCalledWith('1', {
      description: 'Gehalt',
      amount: '3100',
      period: 'YEARLY',
    });
  });

  it('does not send invalid values', async () => {
    await open(rent);

    await type('description', '   ');
    await type('amount', '10.005');
    await save();

    expect(store.changeItem).not.toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    const errors = [...element.querySelectorAll('mat-error')].map((e) => e.textContent?.trim());
    expect(errors).toEqual([
      'Bitte eine Beschreibung angeben.',
      'Höchstens zwei Nachkommastellen.',
    ]);

    await type('description', 'Miete');
    await type('amount', '0');
    await save();

    expect(store.changeItem).not.toHaveBeenCalled();
    expect(element.querySelector('mat-error')?.textContent).toContain(
      'Bitte einen positiven Betrag angeben.',
    );
  });

  it('shows the current note and saves a changed one (PN-4)', async () => {
    await open({ ...salary, note: 'netto' });

    expect(input('note').value).toBe('netto');

    await type('note', ' brutto ');
    await save();

    expect(store.changeItem).toHaveBeenCalledWith('1', {
      description: 'Gehalt',
      amount: '3000',
      period: 'YEARLY',
      note: 'brutto',
    });
  });

  it('shows the current subcategory and saves a changed one (UK-4)', async () => {
    await open({ ...rent, subcategory: 'Wohnen' });
    expect(input('subcategory').value).toBe('Wohnen');

    await type('subcategory', 'Auto');
    await save();

    expect(store.changeItem).toHaveBeenCalledWith('2', {
      description: 'Miete',
      amount: '850',
      period: 'YEARLY',
      category: 'LEBENSUNTERHALT',
      subcategory: 'Auto',
    });
    expect(store.subcategorySuggestions).toHaveBeenCalledWith('EXPENSE');
  });

  it('moves the item to "Sonstiges" when the subcategory is emptied (UK-4)', async () => {
    await open({ ...salary, subcategory: 'Gehalt' });

    await type('subcategory', '');
    await save();

    expect(store.changeItem).toHaveBeenCalledWith('1', {
      description: 'Gehalt',
      amount: '3000',
      period: 'YEARLY',
    });
  });

  it('removes the note when it is emptied (PN-4)', async () => {
    await open({ ...salary, note: 'netto' });

    await type('note', '');
    await save();

    expect(store.changeItem).toHaveBeenCalledWith('1', {
      description: 'Gehalt',
      amount: '3000',
      period: 'YEARLY',
    });
  });

  it('does not save a note longer than 1000 characters (PN-2)', async () => {
    await open(salary);

    await type('note', 'x'.repeat(1001));
    await save();

    expect(store.changeItem).not.toHaveBeenCalled();
    expect(element.querySelector('mat-error')?.textContent).toContain('Höchstens 1000 Zeichen.');
  });

  it('stays open and shows why saving failed', async () => {
    await open(rent);
    store.changeItem.mockRejectedValue(new Error('Budget item not found: 2'));

    await type('amount', '900');
    await save();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Budget item not found: 2',
    );
    expect(input('amount').value).toBe('900');
  });
});
