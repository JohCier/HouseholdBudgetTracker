import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { BudgetItem } from '../budget.model';
import { BudgetStore } from '../data-access/budget-store';
import { SavingPotentialDialog } from './saving-potential-dialog';

const gym: BudgetItem = {
  id: 'gym',
  description: 'Fitnessstudio',
  amount: '360.00',
  period: 'YEARLY',
  monthlyAmount: '30.00',
  annualAmount: '360.00',
  type: 'EXPENSE',
  category: 'FREIZEIT_SPASS',
  subcategory: null,
  note: null,
  important: false,
  savingPotential: null,
  percentageOfGroup: null,
};

// PM-2a, PM-4b
describe('SavingPotentialDialog', () => {
  let store: { changeSavingPotential: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<SavingPotentialDialog>;
  let element: HTMLElement;

  async function open(item: BudgetItem) {
    store = { changeSavingPotential: vi.fn(async () => undefined) };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BudgetStore, useValue: store },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: item },
      ],
    });
    fixture = TestBed.createComponent(SavingPotentialDialog);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  const amount = () => element.querySelector<HTMLInputElement>('input')!;
  const button = (label: string) =>
    [...element.querySelectorAll<HTMLButtonElement>('button')].find(
      (b) => b.textContent?.trim() === label,
    );

  async function type(value: string) {
    amount().value = value;
    amount().dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function save() {
    button('Speichern')!.click();
    await fixture.whenStable();
  }

  it('suggests the monthly amount of an expense without saving potential and saves it', async () => {
    await open(gym);

    expect(element.querySelector('h2')?.textContent).toContain('Sparpotential bei Fitnessstudio');
    expect(amount().value).toBe('30');
    expect(button('Entfernen')).toBeUndefined();

    await save();

    expect(store.changeSavingPotential).toHaveBeenCalledWith('gym', '30');
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('shows the saving potential an expense has and removes it', async () => {
    await open({ ...gym, savingPotential: '12.50' });

    expect(amount().value).toBe('12.5');

    button('Entfernen')!.click();
    await fixture.whenStable();

    expect(store.changeSavingPotential).toHaveBeenCalledWith('gym', null);
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('rejects more than the monthly amount, nothing positive and more than two decimals', async () => {
    await open(gym);

    for (const [value, message] of [
      ['30.01', 'Höchstens €30.00.'],
      ['0', 'Bitte einen positiven Betrag angeben.'],
      ['1.001', 'Höchstens zwei Nachkommastellen.'],
    ]) {
      await type(value);
      await save();
      expect(element.querySelector('mat-error')?.textContent?.trim()).toBe(message);
    }
    expect(store.changeSavingPotential).not.toHaveBeenCalled();
  });

  it('stays open and shows the message if saving fails', async () => {
    await open(gym);
    store.changeSavingPotential.mockRejectedValueOnce(new Error('Server nicht erreichbar.'));

    await save();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Server nicht erreichbar.',
    );
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
