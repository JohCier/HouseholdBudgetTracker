import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSelectHarness } from '@angular/material/select/testing';
import { NewItemPreset } from '../budget.model';
import { BudgetStore } from '../data-access/budget-store';
import { AddItemDialog } from './add-item-dialog';

const car: NewItemPreset = {
  group: 'Auto',
  type: 'EXPENSE',
  category: 'LEBENSUNTERHALT',
  subcategory: 'Auto',
};

// EG-10, EG-11, AP-14, AP-15
describe('AddItemDialog', () => {
  let store: {
    addIncome: ReturnType<typeof vi.fn>;
    addExpense: ReturnType<typeof vi.fn>;
    subcategorySuggestions: ReturnType<typeof vi.fn>;
  };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<AddItemDialog>;
  let element: HTMLElement;

  async function open(preset: NewItemPreset | null) {
    store = {
      addIncome: vi.fn(async () => undefined),
      addExpense: vi.fn(async () => undefined),
      subcategorySuggestions: vi.fn(() => ['Auto']),
    };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BudgetStore, useValue: store },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: preset },
      ],
    });
    fixture = TestBed.createComponent(AddItemDialog);
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

  async function add() {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  const checked = (label: string) =>
    element
      .querySelector(`[aria-label="${label}"] .mat-button-toggle-checked`)
      ?.textContent?.trim();

  it('fills in type, category and subcategory of the group and adds the item', async () => {
    await open(car);
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);

    expect(element.querySelector('h2')?.textContent).toContain('Neuer Eintrag in Auto');
    expect(checked('Art des Eintrags')).toBe('Ausgabe');
    expect(await select.getValueText()).toBe('Lebensunterhalt');
    expect(input('subcategory').value).toBe('Auto');
    expect(input('description').hasAttribute('cdkFocusInitial')).toBe(true);

    await type('description', ' Sprit ');
    await type('amount', '100');
    await add();

    expect(store.addExpense).toHaveBeenCalledWith({
      description: 'Sprit',
      amount: '100',
      period: 'MONTHLY',
      category: 'LEBENSUNTERHALT',
      subcategory: 'Auto',
    });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  // AP-14
  it('starts as an empty expense when opened from "Posten erfassen"', async () => {
    await open(null);
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);

    expect(element.querySelector('h2')?.textContent?.trim()).toBe('Posten erfassen');
    expect(checked('Art des Eintrags')).toBe('Ausgabe');
    expect(checked('Angabe monatlich oder jährlich')).toBe('Monatlich');
    expect(await select.getValueText()).toBe('');
    expect(input('subcategory').value).toBe('');
    expect(input('description').value).toBe('');

    await type('description', 'Sprit');
    await type('amount', '120');
    await select.open();
    await select.clickOptions({ text: 'Lebensunterhalt' });
    await type('subcategory', 'Auto');
    await add();

    expect(store.addExpense).toHaveBeenCalledWith({
      description: 'Sprit',
      amount: '120',
      period: 'MONTHLY',
      category: 'LEBENSUNTERHALT',
      subcategory: 'Auto',
    });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('adds an income to "Sonstiges" without a subcategory', async () => {
    await open({ group: 'Sonstiges', type: 'INCOME', category: null, subcategory: '' });

    expect(checked('Art des Eintrags')).toBe('Einnahme');
    expect(element.querySelector('mat-select')).toBeNull();
    expect(input('subcategory').value).toBe('');

    await type('description', 'Bonus');
    await type('amount', '500');
    await add();

    expect(store.addIncome).toHaveBeenCalledWith({
      description: 'Bonus',
      amount: '500',
      period: 'MONTHLY',
    });
  });

  it('does not send invalid values', async () => {
    await open(car);

    await add();

    expect(store.addExpense).not.toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(element.querySelector('mat-error')?.textContent).toContain(
      'Bitte eine Beschreibung angeben.',
    );
  });

  it('stays open with the values typed and shows why adding failed', async () => {
    await open(car);
    store.addExpense.mockRejectedValue(new Error('Der Server antwortet nicht.'));

    await type('description', 'Sprit');
    await type('amount', '100');
    await add();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Der Server antwortet nicht.',
    );
    expect(input('description').value).toBe('Sprit');
  });
});
