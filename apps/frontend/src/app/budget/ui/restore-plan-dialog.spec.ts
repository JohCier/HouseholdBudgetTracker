import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { HouseholdPlanOverview } from '../budget.model';
import { BudgetApi } from '../data-access/budget-api';
import { RestorePlanDialog, restorePlan, restoredMessage } from './restore-plan-dialog';

const deleted = { planId: 'plan-old', year: 2026, itemCount: 12 };
const conflict = new HttpErrorResponse({
  status: 409,
  error: { type: 'urn:hbt:plan-already-exists' },
});

function active(items: number): HouseholdPlanOverview {
  return {
    id: 'plan-new',
    year: 2026,
    items: Array.from({ length: items }),
  } as HouseholdPlanOverview;
}

// GP-13, GP-14
describe('RestorePlanDialog', () => {
  let api: { getOverview: ReturnType<typeof vi.fn>; restorePlan: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<RestorePlanDialog>;
  let element: HTMLElement;

  beforeEach(async () => {
    api = {
      getOverview: vi.fn(() => of(active(3))),
      restorePlan: vi.fn(() => of(undefined)),
    };
    dialogRef = { close: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: BudgetApi, useValue: api },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: deleted },
      ],
    });
    fixture = TestBed.createComponent(RestorePlanDialog);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function button(label: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button')].find(
      (b) => b.textContent?.trim() === label,
    )!;
  }

  it('names the items of the active and the deleted plan', () => {
    expect(api.getOverview).toHaveBeenCalledWith(2026);
    expect(element.querySelector('h2')!.textContent).toContain('Plan 2026 wiederherstellen');
    expect(element.textContent).toContain(
      'Für 2026 gibt es bereits einen Haushaltsplan mit 3 Posten. Er wird durch den gelöschten Plan mit 12 Posten ersetzt und unter „Gelöschte Pläne" abgelegt.',
    );
    expect(button('Abbrechen')).toBeTruthy();
  });

  it('restores the plan over the active one and closes', async () => {
    button('Überschreiben').click();
    await fixture.whenStable();

    expect(api.restorePlan).toHaveBeenCalledWith('plan-old', { overwrite: true });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('stays open and shows the message if restoring fails', async () => {
    api.restorePlan.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' })),
    );

    button('Überschreiben').click();
    await fixture.whenStable();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
  });
});

// GP-12 to GP-14
describe('restorePlan', () => {
  it('restores right away if the year has no active plan', async () => {
    const api = { restorePlan: vi.fn(() => of(undefined)) };
    const dialog = { open: vi.fn() };

    expect(await restorePlan(api as never, dialog as never, deleted)).toBe(true);
    expect(api.restorePlan).toHaveBeenCalledWith('plan-old', { overwrite: false });
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('asks before replacing the active plan of the year', async () => {
    const api = { restorePlan: vi.fn(() => throwError(() => conflict)) };
    const dialog = { open: vi.fn(() => ({ afterClosed: () => of(undefined) })) };

    expect(await restorePlan(api as never, dialog as unknown as MatDialog, deleted)).toBe(false);
    expect(dialog.open).toHaveBeenCalledWith(RestorePlanDialog, { data: deleted });
  });

  it('rejects with a message on other errors', async () => {
    const api = {
      restorePlan: vi.fn(() =>
        throwError(
          () => new HttpErrorResponse({ status: 404, error: { type: 'urn:hbt:plan-not-found' } }),
        ),
      ),
    };

    await expect(restorePlan(api as never, { open: vi.fn() } as never, deleted)).rejects.toThrow(
      'Diesen Haushaltsplan gibt es nicht (mehr).',
    );
  });

  it('confirms the restoring with the year', () => {
    expect(restoredMessage(2026)).toBe('Plan 2026 wiederhergestellt.');
  });
});
