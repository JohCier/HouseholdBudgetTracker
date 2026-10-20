import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BudgetStore } from '../data-access/budget-store';
import { AddItemForm } from './add-item-form';

describe('AddItemForm', () => {
  let store: {
    addIncome: ReturnType<typeof vi.fn>;
    addExpense: ReturnType<typeof vi.fn>;
    subcategorySuggestions: ReturnType<typeof vi.fn>;
  };
  let snackBar: { open: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<AddItemForm>;
  let element: HTMLElement;

  beforeEach(async () => {
    store = {
      addIncome: vi.fn(async () => undefined),
      addExpense: vi.fn(async () => undefined),
      subcategorySuggestions: vi.fn((type: string) =>
        type === 'INCOME' ? ['Gehalt', 'Sonstiges'] : ['Auto', 'Wohnen', 'Sonstiges'],
      ),
    };
    snackBar = { open: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BudgetStore, useValue: store },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });
    fixture = TestBed.createComponent(AddItemForm);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

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

  async function chooseType(label: 'Ausgabe' | 'Einnahme' | 'Monatlich' | 'Jährlich') {
    const toggle = [...element.querySelectorAll<HTMLButtonElement>('mat-button-toggle button')];
    toggle.find((button) => button.textContent?.trim() === label)!.click();
    await fixture.whenStable();
  }

  function chosenPeriod(): string | undefined {
    return element
      .querySelector('[aria-label="Angabe monatlich oder jährlich"] .mat-button-toggle-checked')
      ?.textContent?.trim();
  }

  async function chooseCategory(label: string) {
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text: label });
  }

  function options(): string[] {
    return [...document.querySelectorAll('mat-option')].map((option) => option.textContent!.trim());
  }

  async function submit() {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  it('adds an expense with its category and clears the form without showing errors (PO-1)', async () => {
    await type('description', '  Miete  ');
    await type('amount', '850.5');
    await chooseCategory('Lebensunterhalt');

    await submit();

    expect(store.addExpense).toHaveBeenCalledWith({
      description: 'Miete',
      amount: '850.5',
      period: 'MONTHLY',
      category: 'LEBENSUNTERHALT',
    });
    expect(snackBar.open).toHaveBeenCalledWith('Ausgabe hinzugefügt', undefined, {
      duration: 3000,
    });
    expect(input('description').value).toBe('');
    expect(input('amount').value).toBe('');
    await fixture.whenStable();
    expect(element.querySelectorAll('mat-error')).toHaveLength(0);
  });

  // MJ-1, MJ-11
  it('starts with monthly amounts and keeps the period chosen after adding an item', async () => {
    expect(chosenPeriod()).toBe('Monatlich');

    await chooseType('Jährlich');
    await type('description', 'Versicherung');
    await type('amount', '1000');
    await chooseCategory('Lebensunterhalt');
    await submit();

    expect(store.addExpense).toHaveBeenCalledWith({
      description: 'Versicherung',
      amount: '1000',
      period: 'YEARLY',
      category: 'LEBENSUNTERHALT',
    });
    expect(chosenPeriod()).toBe('Jährlich');
  });

  it('adds an income without a category and keeps the chosen type', async () => {
    await chooseType('Einnahme');
    expect(element.querySelector('mat-select')).toBeNull();

    await type('description', 'Gehalt');
    await type('amount', '3000');
    await submit();

    expect(store.addIncome).toHaveBeenCalledWith({
      description: 'Gehalt',
      amount: '3000',
      period: 'MONTHLY',
    });
    expect(store.addExpense).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith('Einnahme hinzugefügt', undefined, {
      duration: 3000,
    });
    expect(element.querySelector('mat-select')).toBeNull();
  });

  it('adds an item with its note and clears it (PN-1, PN-3)', async () => {
    await chooseType('Einnahme');
    await type('description', 'Gehalt');
    await type('amount', '3000');
    await type('note', '  netto,\nnach Steuern  ');

    await submit();

    expect(store.addIncome).toHaveBeenCalledWith({
      description: 'Gehalt',
      amount: '3000',
      period: 'MONTHLY',
      note: 'netto,\nnach Steuern',
    });
    expect(input('note').value).toBe('');
  });

  it('adds an item with its subcategory and clears it (UK-1)', async () => {
    await type('description', 'Sprit');
    await type('amount', '80');
    await chooseCategory('Lebensunterhalt');
    await type('subcategory', '  Auto  ');
    await submit();

    expect(store.addExpense).toHaveBeenCalledWith({
      description: 'Sprit',
      amount: '80',
      period: 'MONTHLY',
      category: 'LEBENSUNTERHALT',
      subcategory: 'Auto',
    });
    expect(input('subcategory').value).toBe('');
  });

  it('sends no subcategory for a blank one, so the item goes to "Sonstiges" (UK-1)', async () => {
    await chooseType('Einnahme');
    await type('description', 'Gehalt');
    await type('amount', '3000');
    await type('subcategory', '   ');
    await submit();

    expect(store.addIncome).toHaveBeenCalledWith({
      description: 'Gehalt',
      amount: '3000',
      period: 'MONTHLY',
    });
  });

  it('suggests the subcategories of the chosen type, filtered by what has been typed (UK-6, UK-7)', async () => {
    const field = input('subcategory');
    field.focus();
    field.dispatchEvent(new Event('focusin'));
    await type('subcategory', 'o');
    expect(options()).toEqual(['Auto', 'Wohnen', 'Sonstiges']);

    await chooseType('Einnahme');
    await type('subcategory', 'geh');
    expect(options()).toEqual(['Gehalt']);
  });

  it('rejects a subcategory longer than 50 characters (UK-2)', async () => {
    await chooseType('Einnahme');
    await type('description', 'Gehalt');
    await type('amount', '3000');
    await type('subcategory', 'x'.repeat(51));
    await submit();

    expect(store.addIncome).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Höchstens 50 Zeichen.');
  });

  it('sends no note for a blank one (PN-3)', async () => {
    await chooseType('Einnahme');
    await type('description', 'Gehalt');
    await type('amount', '3000');
    await type('note', '   ');

    await submit();

    expect(store.addIncome).toHaveBeenCalledWith({
      description: 'Gehalt',
      amount: '3000',
      period: 'MONTHLY',
    });
  });

  it('rejects a note longer than 1000 characters (PN-2)', async () => {
    await chooseType('Einnahme');
    await type('description', 'Gehalt');
    await type('amount', '3000');
    await type('note', 'x'.repeat(1001));

    await submit();

    expect(store.addIncome).not.toHaveBeenCalled();
    expect(element.querySelector('mat-error')?.textContent).toContain('Höchstens 1000 Zeichen.');
  });

  it('does not send an incomplete expense', async () => {
    await type('description', '   ');
    await type('amount', '0');

    await submit();

    expect(store.addExpense).not.toHaveBeenCalled();
    const errors = [...element.querySelectorAll('mat-error')].map((e) => e.textContent?.trim());
    expect(errors).toEqual([
      'Bitte eine Beschreibung angeben.',
      'Bitte einen positiven Betrag angeben.',
      'Bitte eine Kategorie wählen.',
    ]);
  });

  it('asks for a positive amount when the amount is negative (PO-4)', async () => {
    await chooseType('Einnahme');
    await type('description', 'Zinsen');
    await type('amount', '-5');

    await submit();

    expect(store.addIncome).not.toHaveBeenCalled();
    expect(element.querySelector('mat-error')?.textContent).toContain(
      'Bitte einen positiven Betrag angeben.',
    );
  });

  it('rejects amounts with more than two decimal places', async () => {
    await chooseType('Einnahme');
    await type('description', 'Zinsen');
    await type('amount', '1.005');

    await submit();

    expect(store.addIncome).not.toHaveBeenCalled();
    expect(element.querySelector('mat-error')?.textContent).toContain(
      'Höchstens zwei Nachkommastellen.',
    );
  });

  it('shows why adding failed and keeps the input', async () => {
    store.addIncome.mockRejectedValue(new Error('Anfrage fehlgeschlagen (500).'));
    await chooseType('Einnahme');
    await type('description', 'Gehalt');
    await type('amount', '3000');

    await submit();

    expect(snackBar.open).toHaveBeenCalledWith('Anfrage fehlgeschlagen (500).', 'OK', {
      duration: 6000,
    });
    expect(input('description').value).toBe('Gehalt');
    expect(element.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });
});
