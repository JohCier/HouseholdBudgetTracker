import { expect, Page, test } from '@playwright/test';
import { addExpense, addIncome, register, registerWithPlan, showAmounts, yearRow } from './support';

/** Cells of a row in the yearly overview: income, expenses, balance. */
function cells(page: Page, year: number) {
  const row = yearRow(page, year);
  return {
    income: row.locator('td').nth(0),
    expense: row.locator('td').nth(1),
    balance: row.locator('td').nth(2),
  };
}

/** The change shown in a cell, e.g. "+3,00 %" (JU-7). */
function change(cell: ReturnType<typeof cells>['income']) {
  return cell.locator('app-year-change span');
}

/** Creates the plan of the year before the one shown and stays there. */
async function createPreviousYearPlan(page: Page, year: number): Promise<void> {
  await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();
  await expect(page.getByRole('heading', { name: String(year), exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();
  await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
}

async function showAllYears(page: Page): Promise<void> {
  await page.getByRole('link', { name: 'Alle Jahre' }).click();
  await expect(page).toHaveURL('/');
}

// docs/specs/jahresuebersicht.md
test.describe('Jahresübersicht', () => {
  const year = new Date().getFullYear();

  test('nach der Registrierung zeigt die Jahresübersicht, dass es noch keinen Plan gibt; „Haushaltsplan anlegen" öffnet den neuen Plan (JU-10, JU-12, JU-13)', async ({
    page,
  }) => {
    await register(page);

    await expect(page).toHaveURL('/');
    await expect(page.getByRole('heading', { name: 'Haushaltspläne' })).toBeVisible();
    await expect(page.getByText('Noch keine Haushaltspläne.')).toBeVisible();

    await page.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();

    await expect(page).toHaveURL(`/jahre/${year}`);
    await expect(page.getByRole('heading', { name: String(year), exact: true })).toBeVisible();
    await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();

    await showAllYears(page);
    await expect(yearRow(page, year)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Haushaltsplan anlegen' })).toBeHidden();
  });

  test('zeigt je Jahr Einnahmen, Ausgaben und Überschuss mit der Veränderung zum Vorjahr, das neueste Jahr oben (JU-1, JU-2, JU-3, JU-5, JU-7, JU-8)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '30900');
    await addExpense(page, 'Miete', '24250', 'Lebensunterhalt');
    await createPreviousYearPlan(page, year - 1);
    await addIncome(page, 'Gehalt', '30000');
    await addExpense(page, 'Miete', '25000', 'Lebensunterhalt');
    await createPreviousYearPlan(page, year - 2);

    await showAllYears(page);
    await showAmounts(page, 'Jährlich');

    await expect(page.locator('app-yearly-overview-page tbody th')).toHaveText([
      String(year),
      String(year - 1),
      String(year - 2),
    ]);
    const current = cells(page, year);
    await expect(current.income).toContainText('30.900,00');
    await expect(change(current.income)).toHaveText('+3,00 %');
    await expect(change(current.income)).toHaveClass('good');
    await expect(current.expense).toContainText('24.250,00');
    await expect(change(current.expense)).toHaveText('−3,00 %');
    await expect(change(current.expense)).toHaveClass('good');
    await expect(current.balance).toContainText('6.650,00');
    await expect(change(current.balance)).toHaveText('+33,00 %');
    await expect(change(current.balance)).toHaveClass('good');

    // Nothing to compare with an empty plan in the year before (JU-6)
    await expect(change(cells(page, year - 1).income)).toHaveText('–');

    // The empty plan before has nothing to compare with and shows zeros
    const empty = cells(page, year - 2);
    await expect(empty.income).toContainText('0,00');
    await expect(empty.expense).toContainText('0,00');
    await expect(empty.balance).toContainText('0,00');
    await expect(change(empty.income)).toHaveText('–');
  });

  test('ohne Plan für das unmittelbare Vorjahr zeigt eine Zeile keine Veränderung (JU-4)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '2000');
    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();
    await createPreviousYearPlan(page, year - 2);
    await addIncome(page, 'Gehalt', '1000');

    await showAllYears(page);

    await expect(page.locator('app-yearly-overview-page tbody th')).toHaveText([
      String(year),
      String(year - 2),
    ]);
    for (const shown of [year, year - 2]) {
      const row = cells(page, shown);
      await expect(change(row.income)).toHaveText('–');
      await expect(change(row.expense)).toHaveText('–');
      await expect(change(row.balance)).toHaveText('–');
    }
  });

  test('ein Defizit wird gekennzeichnet; ein kleineres Defizit als im Vorjahr ist ein Plus, ohne Einnahmen im Vorjahr gibt es keine Veränderung (JU-2, JU-5, JU-6, JU-8)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '1000');
    await addExpense(page, 'Miete', '1500', 'Lebensunterhalt');
    await createPreviousYearPlan(page, year - 1);
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt');

    await showAllYears(page);
    await showAmounts(page, 'Jährlich');

    const current = cells(page, year);
    await expect(current.balance).toContainText('-500,00');
    await expect(current.balance).toContainText('Defizit');
    await expect(change(current.balance)).toHaveText('+50,00 %');
    await expect(change(current.balance)).toHaveClass('good');
    await expect(change(current.expense)).toHaveText('+50,00 %');
    await expect(change(current.expense)).toHaveClass('bad');
    await expect(change(current.income)).toHaveText('–');
    await expect(change(current.income)).toHaveClass('neutral');
  });

  test('ein Klick auf eine Zeile öffnet den Plan des Jahres, Änderungen zeigt die Jahresübersicht danach (JU-11)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await createPreviousYearPlan(page, year - 1);
    await showAllYears(page);
    await showAmounts(page, 'Jährlich');
    await expect(cells(page, year - 1).income).toContainText('0,00');

    await yearRow(page, year - 1).click();

    await expect(page).toHaveURL(`/jahre/${year - 1}`);
    await expect(page.getByRole('heading', { name: String(year - 1), exact: true })).toBeVisible();
    await addIncome(page, 'Gehalt', '1234.5');
    await showAllYears(page);

    await expect(cells(page, year - 1).income).toContainText('1.234,50');
  });

  test('ein Benutzer sieht nur seine eigenen Pläne (JU-1)', async ({ browser }) => {
    const alice = await browser.newPage();
    await registerWithPlan(alice);
    await addIncome(alice, 'Gehalt von A', '3000');

    // Separate context, so B has its own session cookie
    const bob = await (await browser.newContext()).newPage();
    await register(bob);

    await expect(bob.getByText('Noch keine Haushaltspläne.')).toBeVisible();
    await expect(yearRow(bob, year)).toBeHidden();
  });
});
