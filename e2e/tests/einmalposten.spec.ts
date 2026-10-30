import { expect, Page, test } from '@playwright/test';
import {
  addIncome,
  addOneTimeItem,
  balanceSheet,
  fillOneTimeItemDialog,
  oneTimeCells,
  oneTimeItemRow,
  oneTimeTotal,
  openYear,
  showOneTimeItems,
  register,
  registerWithPlan,
  showAmounts,
  yearRow,
} from './support';

const year = new Date().getFullYear();

const backPayment = {
  type: 'Ausgabe',
  description: 'Nebenkostennachzahlung',
  amount: '1200',
  date: `${year}-03-15`,
  note: 'Abrechnung Vorjahr',
} as const;
const taxRefund = {
  type: 'Einnahme',
  description: 'Steuererstattung',
  amount: '800',
  date: `${year}-07-10`,
} as const;

async function showAllYears(page: Page): Promise<void> {
  await page.getByRole('link', { name: 'Alle Jahre' }).click();
  await expect(page).toHaveURL('/');
}

// docs/specs/einmalposten.md
test.describe('Einmalposten', () => {
  test('erfasst Einmalposten im eigenen Abschnitt, Einnahmen und Ausgaben getrennt, mit Summen, ohne den Plan zu ändern (EP-1, EP-2, EP-6, EP-8, EP-9, ES-1)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');

    await addOneTimeItem(page, taxRefund);
    await addOneTimeItem(page, backPayment);

    const items = page.locator('app-one-time-items [role="listitem"]');
    await expect(items).toHaveCount(2);
    await expect(items.nth(0)).toContainText('Steuererstattung');
    await expect(items.nth(0)).toContainText('+800,00 €');
    await expect(items.nth(1)).toContainText('15.03.' + year);
    await expect(items.nth(1)).toContainText('Nebenkostennachzahlung');
    await expect(items.nth(1)).toContainText('Abrechnung Vorjahr');
    await expect(items.nth(1)).toContainText('−1.200,00 €');
    await expect(oneTimeTotal(page, 'Einnahmen')).toHaveText('+800,00 €');
    await expect(oneTimeTotal(page, 'Ausgaben')).toHaveText('−1.200,00 €');
    await expect(oneTimeTotal(page, 'Saldo')).toHaveText('-400,00 €');
    await expect(oneTimeTotal(page, 'Saldo')).toHaveClass(/negative/);
    await expect(balanceSheet(page, 'Gesamteinnahmen', 'Monat')).toHaveText('3.000,00 €');
    await expect(balanceSheet(page, 'Gesamteinnahmen')).toHaveText('36.000,00 €');
    await expect(balanceSheet(page, 'Gesamtausgaben')).toHaveText('0,00 €');
  });

  test('zeigt die einmaligen Summen in der Jahresübersicht in eigenen Spalten, auch bei Monatsbeträgen als Jahreswerte (EP-13, EP-14, EP-15)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addOneTimeItem(page, backPayment);
    await addOneTimeItem(page, taxRefund);
    await showAllYears(page);

    await expect(page.locator('thead .group-head')).toContainText('Haushaltsplan');
    await expect(page.locator('thead .group-head')).toContainText('Einmalig (Jahr)');
    await showAmounts(page, 'Jährlich');
    const row = yearRow(page, year);
    await expect(row.locator('td').nth(0)).toContainText('36.000,00 €');
    await expect(row.locator('td').nth(2)).toContainText('36.000,00 €');
    const oneTime = oneTimeCells(page, year);
    await expect(oneTime.income).toHaveText('800,00 €');
    await expect(oneTime.expense).toHaveText('1.200,00 €');
    await expect(oneTime.balance).toContainText('-400,00 €');
    await expect(oneTime.balance).toContainText('Defizit');
    await expect(oneTime.balance.locator('app-year-change')).toHaveCount(0);

    await showAmounts(page, 'Monatlich');
    await expect(row.locator('td').nth(0)).toContainText('3.000,00 €');
    await expect(oneTime.income).toHaveText('800,00 €');
    await expect(oneTime.balance).toContainText('-400,00 €');
  });

  test('erfasst Einmalposten in einem Jahr ohne Plan; die Jahresübersicht zeigt das Jahr ohne Zahlen und Knöpfe des Plans (EP-5, EP-10, EP-12)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await openYear(page, year + 1);
    await expect(
      page.getByText(`Für ${year + 1} gibt es noch keinen Haushaltsplan.`),
    ).toBeVisible();
    await showOneTimeItems(page);
    await expect(page.locator('app-one-time-items')).toContainText('Keine Einmalposten.');

    await addOneTimeItem(page, {
      type: 'Einnahme',
      description: 'Bonus',
      amount: '2000',
      date: `${year + 1}-12-01`,
      note: 'Jahresprämie',
    });
    await showAllYears(page);

    const row = yearRow(page, year + 1);
    await expect(row.locator('td').nth(0)).toHaveText('–');
    await expect(row.locator('td').nth(1)).toHaveText('–');
    await expect(row.locator('td').nth(2)).toHaveText('–');
    const oneTime = oneTimeCells(page, year + 1);
    await expect(oneTime.income).toHaveText('2.000,00 €');
    await expect(oneTime.expense).toHaveText('0,00 €');
    await expect(oneTime.balance).toHaveText('2.000,00 €');
    await expect(row.getByRole('button')).toHaveCount(0);
    await expect(oneTimeCells(page, year).income).toHaveText('0,00 €');

    await row.click();
    await expect(page).toHaveURL(`/jahre/${year + 1}`);
    await showOneTimeItems(page);
    await expect(oneTimeItemRow(page, 'Bonus')).toBeVisible();
  });

  test('löschen und übernehmen des Plans lässt die Einmalposten, wie sie sind (EP-5)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addOneTimeItem(page, backPayment);

    await page.getByRole('button', { name: 'Als Vorlage übernehmen' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Übernehmen' }).click();
    await expect(page).toHaveURL(`/jahre/${year + 1}`);
    await showOneTimeItems(page);
    await expect(page.locator('app-one-time-items')).toContainText('Keine Einmalposten.');

    await openYear(page, year);
    await page.getByRole('button', { name: 'Plan löschen' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click();
    await expect(page).toHaveURL('/');
    await openYear(page, year);

    await expect(page.getByText(`Für ${year} gibt es noch keinen Haushaltsplan.`)).toBeVisible();
    await showOneTimeItems(page);
    await expect(oneTimeItemRow(page, 'Nebenkostennachzahlung')).toBeVisible();
  });

  test('ändert einen Einmalposten, auch in ein anderes Jahr (EP-7)', async ({ page }) => {
    await registerWithPlan(page);
    await addOneTimeItem(page, taxRefund);

    await page.getByRole('button', { name: 'Einmalposten Steuererstattung bearbeiten' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByLabel('Beschreibung')).toHaveValue('Steuererstattung');
    await expect(dialog.getByLabel('Datum')).toHaveValue(`${year}-07-10`);
    await fillOneTimeItemDialog(page, {
      ...taxRefund,
      amount: '950',
      note: 'Bescheid',
    });
    await expect(oneTimeItemRow(page, 'Steuererstattung')).toContainText('+950,00 €');
    await expect(oneTimeItemRow(page, 'Steuererstattung')).toContainText('Bescheid');

    await page.getByRole('button', { name: 'Einmalposten Steuererstattung bearbeiten' }).click();
    await fillOneTimeItemDialog(page, {
      ...taxRefund,
      date: `${year + 1}-01-05`,
    });
    await expect(oneTimeItemRow(page, 'Steuererstattung')).toBeHidden();
    await expect(page.locator('app-one-time-items')).toContainText('Keine Einmalposten.');

    await openYear(page, year + 1);
    await showOneTimeItems(page);
    await expect(oneTimeItemRow(page, 'Steuererstattung')).toContainText(`05.01.${year + 1}`);
  });

  test('beim Erfassen ist das Datum im aktuellen Jahr heute, in einem anderen Jahr der 1. Januar (EP-11)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    const today = new Date();
    const iso = `${year}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    await showOneTimeItems(page);
    await page.getByRole('button', { name: 'Einmalposten erfassen' }).click();
    await expect(page.getByRole('dialog').getByLabel('Datum')).toHaveValue(iso);
    await page.getByRole('dialog').getByRole('button', { name: 'Abbrechen' }).click();

    await openYear(page, year - 3);
    await showOneTimeItems(page);
    await page.getByRole('button', { name: 'Einmalposten erfassen' }).click();
    await expect(page.getByRole('dialog').getByLabel('Datum')).toHaveValue(`${year - 3}-01-01`);
  });

  test('lehnt einen Betrag von 0,00 € und ein Datum vor 1900 ab (EP-1, EP-3)', async ({ page }) => {
    await register(page);
    await openYear(page, year);
    await showOneTimeItems(page);
    await page.getByRole('button', { name: 'Einmalposten erfassen' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Beschreibung').fill('Null');
    await dialog.getByLabel('Betrag').fill('0');
    await dialog.getByLabel('Datum').fill('1899-12-31');
    await dialog.getByRole('button', { name: 'Erfassen' }).click();

    await expect(dialog).toContainText('Bitte einen positiven Betrag angeben.');
    await expect(dialog).toContainText('Bitte ein Datum von 1900 bis 2200 angeben.');
  });

  test('zeigt die einmaligen Summen auf dem Handy in einer eigenen Zeile unter dem Plan (EP-18)', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await registerWithPlan(page);
    await addOneTimeItem(page, taxRefund);
    await showAllYears(page);

    const oneTimeRow = page.locator(`tr[data-one-time-year="${year}"]`);
    await expect(oneTimeRow).toBeVisible();
    await expect(oneTimeRow).toContainText('Einmalig (Jahr)');
    await expect(oneTimeRow).toContainText('800,00 €');
    await expect(oneTimeCells(page, year).income).toBeHidden();
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
      390,
    );
  });
});
