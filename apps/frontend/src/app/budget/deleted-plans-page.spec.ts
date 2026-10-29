import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { DeletedPlan } from './budget.model';
import { PERIOD_STORAGE_KEY } from './data-access/yearly-overview-store';
import { DeletedPlansPage } from './deleted-plans-page';
import { PurgePlanDialog } from './ui/purge-plan-dialog';

const plan = (planId: string, year: number, changes: Partial<DeletedPlan> = {}): DeletedPlan => ({
  planId,
  year,
  deletedAt: '2026-10-29T19:15:00Z',
  itemCount: 12,
  totalIncome: '38400.00',
  totalExpense: '34200.00',
  balance: '4200.00',
  monthlyTotalIncome: '3200.00',
  monthlyTotalExpense: '2850.00',
  monthlyBalance: '350.00',
  expensesExceedIncome: false,
  ...changes,
});

// docs/specs/geloeschte-plaene.md
describe('DeletedPlansPage', () => {
  let http: HttpTestingController;
  let snackBar: { open: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    snackBar = { open: vi.fn() };
    dialog = { open: vi.fn(() => ({ afterClosed: () => of(true) })) };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function render(plans: DeletedPlan[]) {
    const fixture = TestBed.createComponent(DeletedPlansPage);
    fixture.detectChanges();
    http.expectOne('/api/plans/deleted').flush({ plans });
    await settle(fixture);
    return fixture.nativeElement as HTMLElement;
  }

  function rowOf(element: HTMLElement, planId: string): HTMLElement {
    return element.querySelector<HTMLElement>(`tr[data-plan-id="${planId}"]`)!;
  }

  function button(element: HTMLElement, label: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button')].find(
      (b) => b.textContent?.trim() === label,
    )!;
  }

  // GP-22, GP-23
  it('lists each deleted plan with when it was deleted, its items and its monthly amounts', async () => {
    const element = await render([
      plan('a', 2026),
      plan('b', 2023, { expensesExceedIncome: true }),
    ]);

    const row = rowOf(element, 'a');
    expect(row.textContent).toContain('2026');
    expect(row.textContent).toMatch(/29\.10\.2026, \d\d:15/);
    expect(row.textContent).toContain('12');
    expect(row.textContent).toContain('€3,200.00');
    expect(row.textContent).toContain('€350.00');
    expect(rowOf(element, 'b').textContent).toContain('Defizit');
    expect(element.querySelectorAll('tbody tr')).toHaveLength(2);
    expect(element.querySelector('a[href="/"]')!.textContent).toContain('Alle Jahre');
  });

  // GP-23
  it('shows yearly amounts if they are chosen in the yearly overview', async () => {
    localStorage.setItem(PERIOD_STORAGE_KEY, 'YEARLY');

    const element = await render([plan('a', 2026)]);

    expect(rowOf(element, 'a').textContent).toContain('€38,400.00');
  });

  // GP-25
  it('says so if there are no deleted plans', async () => {
    const element = await render([]);

    expect(element.textContent).toContain('Keine gelöschten Pläne.');
    expect(element.querySelector('table')).toBeNull();
  });

  // GP-12
  it('restores a plan right away if its year has no active plan, and stays on the page', async () => {
    const element = await render([plan('a', 2026)]);

    button(rowOf(element, 'a'), 'Wiederherstellen').click();
    const restore = http.expectOne('/api/plans/a/restore');
    expect(restore.request.body).toEqual({ overwrite: false });
    restore.flush(null, { status: 204, statusText: 'No Content' });
    await vi.waitFor(() => http.expectOne('/api/plans/deleted').flush({ plans: [] }));

    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith('Plan 2026 wiederhergestellt.', undefined, {
        duration: 3000,
      }),
    );
    expect(dialog.open).not.toHaveBeenCalled();
  });

  // GP-16, GP-17
  it('deletes a plan for good after asking', async () => {
    const element = await render([plan('a', 2026)]);

    button(rowOf(element, 'a'), 'Endgültig löschen').click();

    expect(dialog.open).toHaveBeenCalledWith(PurgePlanDialog, {
      data: expect.objectContaining({ planId: 'a', year: 2026, itemCount: 12 }),
    });
    http.expectOne('/api/plans/deleted').flush({ plans: [] });
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith('Plan 2026 endgültig gelöscht.', undefined, {
        duration: 3000,
      }),
    );
  });
});

async function settle(fixture: ComponentFixture<unknown>): Promise<void> {
  await fixture.whenStable();
  fixture.detectChanges();
}
