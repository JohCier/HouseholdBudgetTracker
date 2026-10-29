import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { BudgetApi } from '../data-access/budget-api';
import { PurgePlanDialog, purgedMessage } from './purge-plan-dialog';

// GP-16, GP-17
describe('PurgePlanDialog', () => {
  let api: { purgePlan: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<PurgePlanDialog>;
  let element: HTMLElement;

  beforeEach(async () => {
    api = { purgePlan: vi.fn(() => of(undefined)) };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BudgetApi, useValue: api },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { planId: 'plan-old', year: 2026, itemCount: 12 } },
      ],
    });
    fixture = TestBed.createComponent(PurgePlanDialog);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function button(label: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button')].find(
      (b) => b.textContent?.trim() === label,
    )!;
  }

  it('names the year and the number of items', () => {
    expect(element.querySelector('h2')!.textContent).toContain('Plan 2026 endgültig löschen');
    expect(element.textContent).toContain(
      'Der gelöschte Haushaltsplan 2026 mit 12 Posten wird endgültig gelöscht. Das lässt sich nicht rückgängig machen.',
    );
    expect(button('Abbrechen')).toBeTruthy();
  });

  it('deletes the plan for good and closes', async () => {
    button('Endgültig löschen').click();
    await fixture.whenStable();

    expect(api.purgePlan).toHaveBeenCalledWith('plan-old');
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('stays open and shows the message if it fails', async () => {
    api.purgePlan.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' })),
    );

    button('Endgültig löschen').click();
    await fixture.whenStable();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
  });

  it('confirms with the year', () => {
    expect(purgedMessage(2026)).toBe('Plan 2026 endgültig gelöscht.');
  });
});
