import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { OneTimeItem } from '../budget.model';
import { OneTimeItemStore } from '../data-access/one-time-item-store';
import { DeleteOneTimeItemDialog } from './delete-one-time-item-dialog';

const bonus: OneTimeItem = {
  id: '5',
  type: 'INCOME',
  description: 'Bonus',
  amount: '2000.00',
  date: '2027-12-01',
  note: null,
};

// EP-17
describe('DeleteOneTimeItemDialog', () => {
  let store: { deleteItem: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<DeleteOneTimeItemDialog>;
  let element: HTMLElement;

  beforeEach(async () => {
    store = { deleteItem: vi.fn(async () => undefined) };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: OneTimeItemStore, useValue: store },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: bonus },
      ],
    });
    fixture = TestBed.createComponent(DeleteOneTimeItemDialog);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function deleteButton(): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button')].find(
      (button) => button.textContent!.trim() === 'Löschen',
    )!;
  }

  it('names the item and deletes it only once confirmed', async () => {
    expect(element.textContent).toContain('Einmalposten „Bonus" löschen?');
    expect(store.deleteItem).not.toHaveBeenCalled();

    deleteButton().click();
    await fixture.whenStable();

    expect(store.deleteItem).toHaveBeenCalledWith('5');
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('stays open and shows why deleting failed', async () => {
    store.deleteItem.mockRejectedValue(new Error('Server nicht erreichbar.'));

    deleteButton().click();
    await fixture.whenStable();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')!.textContent).toContain(
      'Server nicht erreichbar.',
    );
  });
});
