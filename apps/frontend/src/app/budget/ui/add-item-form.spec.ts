import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BudgetStore } from '../data-access/budget-store';
import { AddItemForm } from './add-item-form';

describe('AddItemForm', () => {
  let store: { addIncome: ReturnType<typeof vi.fn>; addExpense: ReturnType<typeof vi.fn> };
  let snackBar: { open: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<AddItemForm>;
  let element: HTMLElement;

  beforeEach(async () => {
    store = { addIncome: vi.fn(async () => undefined), addExpense: vi.fn(async () => undefined) };
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
    return element.querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`)!;
  }

  async function type(name: string, value: string) {
    input(name).value = value;
    input(name).dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function chooseType(label: 'Ausgabe' | 'Einnahme') {
    const toggle = [...element.querySelectorAll<HTMLButtonElement>('mat-button-toggle button')];
    toggle.find((button) => button.textContent?.trim() === label)!.click();
    await fixture.whenStable();
  }

  async function chooseCategory(label: string) {
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text: label });
  }

  async function submit() {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  it('adds an expense with its category and clears the form', async () => {
    await type('description', '  Miete  ');
    await type('amount', '850.5');
    await chooseCategory('Lebensunterhalt');

    await submit();

    expect(store.addExpense).toHaveBeenCalledWith({
      description: 'Miete',
      amount: '850.5',
      category: 'LEBENSUNTERHALT',
    });
    expect(snackBar.open).toHaveBeenCalledWith('Ausgabe hinzugefügt', undefined, {
      duration: 3000,
    });
    expect(input('description').value).toBe('');
    expect(input('amount').value).toBe('');
  });

  it('adds an income without a category and keeps the chosen type', async () => {
    await chooseType('Einnahme');
    expect(element.querySelector('mat-select')).toBeNull();

    await type('description', 'Gehalt');
    await type('amount', '3000');
    await submit();

    expect(store.addIncome).toHaveBeenCalledWith({ description: 'Gehalt', amount: '3000' });
    expect(store.addExpense).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith('Einnahme hinzugefügt', undefined, {
      duration: 3000,
    });
    expect(element.querySelector('mat-select')).toBeNull();
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
