import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { Router, provideRouter } from '@angular/router';
import { YearlyOverviewEntry } from './budget.model';
import { PERIOD_STORAGE_KEY } from './data-access/yearly-overview-store';
import { currentYear } from './year';
import { YearlyOverviewPage } from './yearly-overview-page';

const entry = (year: number, changes: Partial<YearlyOverviewEntry> = {}): YearlyOverviewEntry => ({
  planId: `plan-${year}`,
  year,
  totalIncome: '30900.00',
  totalExpense: '24250.00',
  balance: '6650.00',
  monthlyTotalIncome: '2575.00',
  monthlyTotalExpense: '2020.83',
  monthlyBalance: '554.17',
  expensesExceedIncome: false,
  incomeChange: null,
  expenseChange: null,
  balanceChange: null,
  ...changes,
});

// docs/specs/jahresuebersicht.md
describe('YearlyOverviewPage', () => {
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.fn>;
  let snackBar: { open: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    snackBar = { open: vi.fn() };
    dialog = {
      open: vi.fn(() => ({
        afterClosed: () => of({ sourceYear: currentYear(), targetYear: currentYear() + 1 }),
      })),
    };
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
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true) as never;
  });

  afterEach(() => http.verify());

  async function render(years: YearlyOverviewEntry[]) {
    const fixture = TestBed.createComponent(YearlyOverviewPage);
    fixture.detectChanges();
    http.expectOne('/api/plans/years').flush({ years });
    await settle(fixture);
    return fixture.nativeElement as HTMLElement;
  }

  function periodToggle(element: HTMLElement, label: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('mat-button-toggle button')].find(
      (button) => button.textContent!.trim() === label,
    )!;
  }

  function row(element: HTMLElement, year: number): HTMLTableRowElement {
    return element.querySelector<HTMLTableRowElement>(`tr[data-year="${year}"]`)!;
  }

  // JU-1, JU-2, JU-5, JU-7, JU-8; amounts use the default locale of the tests
  it('lists the plans with their totals and changes from the previous year', async () => {
    localStorage.setItem(PERIOD_STORAGE_KEY, 'YEARLY');
    const element = await render([
      entry(currentYear(), { incomeChange: 3, expenseChange: -3, balanceChange: 33 }),
      entry(currentYear() - 1),
    ]);

    const rows = [...element.querySelectorAll('tbody tr')];
    expect(rows.map((r) => r.querySelector('th')!.textContent!.trim())).toEqual([
      String(currentYear()),
      String(currentYear() - 1),
    ]);
    const cells = [...row(element, currentYear()).querySelectorAll('td')];
    expect(cells[0].textContent).toContain('30,900.00');
    expect(cells[0].textContent).toContain('+3,00 %');
    expect(cells[1].textContent).toContain('24,250.00');
    expect(cells[1].textContent).toContain('−3,00 %');
    expect(cells[2].textContent).toContain('6,650.00');
    expect(cells[2].textContent).toContain('+33,00 %');
    expect(
      cells.slice(0, 3).map((cell) => cell.querySelector('app-year-change span')!.className),
    ).toEqual(['good', 'good', 'good']);
    expect(row(element, currentYear() - 1).textContent).toContain('–');
  });

  // JU-2
  it('marks a deficit', async () => {
    const element = await render([
      entry(currentYear(), { balance: '-500.00', expensesExceedIncome: true }),
    ]);

    expect(row(element, currentYear()).textContent).toContain('Defizit');
  });

  // JM-1, JM-3, JM-5, JM-7
  it('shows monthly amounts with the same changes unless chosen otherwise', async () => {
    const element = await render([
      entry(currentYear(), { incomeChange: 3, expenseChange: -3, balanceChange: 33 }),
    ]);

    expect(periodToggle(element, 'Monatlich').getAttribute('aria-checked')).toBe('true');
    const cells = [...row(element, currentYear()).querySelectorAll('td')];
    expect(cells[0].textContent).toContain('2,575.00');
    expect(cells[0].textContent).toContain('+3,00 %');
    expect(cells[1].textContent).toContain('2,020.83');
    expect(cells[1].textContent).toContain('−3,00 %');
    expect(cells[2].textContent).toContain('554.17');
    expect(cells[2].textContent).toContain('+33,00 %');
  });

  // JM-2, JM-7
  it('switches to yearly amounts and remembers the choice', async () => {
    const element = await render([entry(currentYear())]);

    periodToggle(element, 'Jährlich').click();
    await TestBed.inject(ApplicationRef).whenStable();

    expect(row(element, currentYear()).querySelector('td')!.textContent).toContain('30,900.00');
    expect(localStorage.getItem(PERIOD_STORAGE_KEY)).toBe('YEARLY');
  });

  // JM-4
  it('marks a deficit in monthly amounts', async () => {
    const element = await render([
      entry(currentYear(), {
        balance: '-100.00',
        monthlyBalance: '-8.33',
        expensesExceedIncome: true,
      }),
    ]);

    const balance = row(element, currentYear()).querySelectorAll('td')[2];
    expect(balance.textContent).toContain('-€8.33');
    expect(balance.textContent).toContain('Defizit');
  });

  // JM-1
  it('offers no switch without a plan', async () => {
    const element = await render([]);

    expect(element.querySelector('mat-button-toggle-group')).toBeNull();
  });

  // JU-11
  it('opens the plan of a year by clicking its row', async () => {
    const element = await render([entry(currentYear())]);

    row(element, currentYear()).click();

    expect(navigate).toHaveBeenCalledWith(['/jahre', currentYear()]);
    expect(row(element, currentYear()).querySelector('a')!.getAttribute('href')).toBe(
      `/jahre/${currentYear()}`,
    );
  });

  // PU-12, PU-15
  it('takes over the plan of a row as template without opening it, then opens the target year', async () => {
    const element = await render([entry(currentYear())]);

    row(element, currentYear())
      .querySelector<HTMLButtonElement>(
        `button[aria-label="Plan ${currentYear()} als Vorlage übernehmen"]`,
      )!
      .click();

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), {
      data: { sourceYear: currentYear() },
    });
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith(
        `Plan ${currentYear()} nach ${currentYear() + 1} übernommen.`,
        undefined,
        { duration: 3000 },
      ),
    );
    expect(navigate).toHaveBeenCalledExactlyOnceWith(['/jahre', currentYear() + 1]);
  });

  // PL-7, PL-9
  it('deletes the plan of a row after asking without opening it, and stays on the overview', async () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    const element = await render([entry(currentYear()), entry(currentYear() - 1)]);

    row(element, currentYear())
      .querySelector<HTMLButtonElement>(`button[aria-label="Plan ${currentYear()} löschen"]`)!
      .click();

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), {
      data: { year: currentYear() },
    });
    http.expectOne('/api/plans/years').flush({ years: [entry(currentYear() - 1)] });
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith(`Plan ${currentYear()} gelöscht.`, undefined, {
        duration: 3000,
      }),
    );
    expect(navigate).not.toHaveBeenCalled();
    expect(element.querySelectorAll('tbody tr')).toHaveLength(1);
  });

  // PL-8
  it('changes nothing if deleting a plan is cancelled', async () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(undefined) });
    const element = await render([entry(currentYear())]);

    row(element, currentYear())
      .querySelector<HTMLButtonElement>(`button[aria-label="Plan ${currentYear()} löschen"]`)!
      .click();

    expect(snackBar.open).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  // JU-13, JU-12
  it('offers to create the plan of the current year if the user has none yet', async () => {
    const element = await render([]);

    expect(element.textContent).toContain('Noch keine Haushaltspläne.');
    expect(element.querySelector('table')).toBeNull();

    button(element, 'Haushaltsplan anlegen').click();
    const req = http.expectOne('/api/plans');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ year: currentYear() });
    req.flush({ id: 'plan', overview: {} });
    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/jahre', currentYear()]));
  });

  // JU-12
  it('offers to create the plan of the current year only while there is none', async () => {
    const withCurrent = await render([entry(currentYear())]);
    expect(withCurrent.textContent).not.toContain('Haushaltsplan anlegen');
  });

  it('offers to create the plan of the current year next to older plans', async () => {
    const element = await render([entry(currentYear() - 1)]);

    expect(element.textContent).toContain(
      `Für ${currentYear()} gibt es noch keinen Haushaltsplan.`,
    );
    expect(element.textContent).not.toContain('Noch keine Haushaltspläne.');
  });

  it('shows why creating the plan failed', async () => {
    const element = await render([]);

    button(element, 'Haushaltsplan anlegen').click();
    http
      .expectOne('/api/plans')
      .flush({ type: 'urn:hbt:plan-already-exists' }, { status: 409, statusText: 'Conflict' });
    await vi.waitFor(() =>
      expect(snackBar.open).toHaveBeenCalledWith(
        'Für dieses Jahr gibt es bereits einen Haushaltsplan.',
        'OK',
        { duration: 6000 },
      ),
    );
    expect(navigate).not.toHaveBeenCalled();
  });

  it('shows a load error and loads again on request', async () => {
    const fixture = TestBed.createComponent(YearlyOverviewPage);
    fixture.detectChanges();
    http.expectOne('/api/plans/years').flush(null, { status: 500, statusText: 'Error' });
    await settle(fixture);
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Daten konnten nicht geladen werden',
    );
    expect(element.textContent).not.toContain('Haushaltsplan anlegen');

    button(element, 'Erneut versuchen').click();
    http.expectOne('/api/plans/years').flush({ years: [] });
    await settle(fixture);
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });
});

/** Lets the store take in the response, then renders it. */
async function settle(fixture: ComponentFixture<unknown>): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve));
  await fixture.whenStable();
}

function button(element: HTMLElement, text: string): HTMLButtonElement {
  return [...element.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
    b.textContent?.includes(text),
  )!;
}
