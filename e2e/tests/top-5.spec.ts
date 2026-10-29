import { expect, Page, test } from '@playwright/test';
import { addExpense, registerWithPlan, setSavingPotential, showTopItems, topItemRows } from './support';

const topItems = (page: Page) => page.locator('app-top-items');

/** Description, monthly and yearly value of each row, e.g. "1.Miete Sonstiges | 1.000,00 € | 12.000,00 €". */
async function rowTexts(page: Page): Promise<string[][]> {
  return topItemRows(page).evaluateAll((rows) =>
    rows.map((row) =>
      [...row.querySelectorAll('th, td')].map((cell) =>
        (cell.textContent ?? '').replace(/\s+/g, ' ').trim(),
      ),
    ),
  );
}

// docs/specs/top-5.md
test.describe('Top 5', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('zeigt zuerst die Ausgaben mit dem höchsten Sparpotential, mit Unterkategorie und Betrag der Ausgabe (TP-1, TP-3, TP-4, TP-7, TP-8, TP-9)', async ({
    page,
  }) => {
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', 'Abonnements', 'Monatlich');
    await addExpense(page, 'Fitnessstudio', '360', 'Freizeit & Spaß', 'Hobbys');
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', undefined, 'Monatlich');
    await setSavingPotential(page, 'Streaming', '5');
    await setSavingPotential(page, 'Fitnessstudio');

    await expect(
      page.getByRole('radiogroup', { name: 'Liste' }).getByRole('radio', { name: 'Sparpotential' }),
    ).toBeChecked();
    await expect(topItems(page).getByRole('columnheader')).toHaveText(['Monat', 'Jahr']);
    await expect(topItemRows(page)).toHaveCount(2);
    expect(await rowTexts(page)).toEqual([
      ['1.Fitnessstudio Hobbys · von 30,00 €', '30,00 €', '360,00 €'],
      ['2.Streaming Abonnements · von 15,00 €', '5,00 €', '60,00 €'],
    ]);
  });

  test('„Ausgaben" zeigt die fünf höchsten Ausgaben ohne Sparen & Investieren, jährliche nach Monatsbetrag, gleiche alphabetisch (TP-2, TP-3, TP-4)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Wohnen', 'Monatlich');
    await addExpense(page, 'Lebensmittel', '400', 'Lebensunterhalt', 'Essen', 'Monatlich');
    await addExpense(page, 'Kfz-Versicherung', '600', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Handy', '40', 'Lebensunterhalt', 'Abonnements', 'Monatlich');
    await addExpense(page, 'Kino', '20', 'Freizeit & Spaß', 'Ausgehen', 'Monatlich');
    await addExpense(page, 'Bücher', '20', 'Freizeit & Spaß', 'Hobbys', 'Monatlich');
    await addExpense(page, 'Zeitschrift', '10', 'Freizeit & Spaß', 'Hobbys', 'Monatlich');
    await addExpense(page, 'ETF-Sparplan', '500', 'Sparen & Investieren', undefined, 'Monatlich');

    await showTopItems(page, 'Ausgaben');

    await expect(topItemRows(page)).toHaveCount(5);
    expect(await rowTexts(page)).toEqual([
      ['1.Miete Wohnen', '1.000,00 €', '12.000,00 €'],
      ['2.Lebensmittel Essen', '400,00 €', '4.800,00 €'],
      ['3.Kfz-Versicherung Auto', '50,00 €', '600,00 €'],
      ['4.Handy Abonnements', '40,00 €', '480,00 €'],
      ['5.Bücher Hobbys', '20,00 €', '240,00 €'],
    ]);
  });

  test('ohne Sparpotential ein Hinweis, ein neues Sparpotential erscheint sofort (TP-10, TP-12)', async ({
    page,
  }) => {
    await expect(topItems(page)).toContainText('Noch kein Sparpotential markiert');
    await showTopItems(page, 'Ausgaben');
    await expect(topItems(page)).toContainText('Noch keine Ausgaben');

    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', 'Abonnements', 'Monatlich');
    await showTopItems(page, 'Sparpotential');
    await setSavingPotential(page, 'Streaming', '5');

    await expect(topItemRows(page)).toHaveCount(1);
    await expect(topItemRows(page)).toContainText('Streaming');
  });

  test('die gewählte Liste bleibt beim Wechsel des Jahres erhalten (TP-7)', async ({ page }) => {
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', undefined, 'Monatlich');
    await showTopItems(page, 'Ausgaben');

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();

    await expect(
      page.getByRole('radiogroup', { name: 'Liste' }).getByRole('radio', { name: 'Ausgaben' }),
    ).toBeChecked();
    await expect(topItemRows(page)).toContainText('Miete');
  });

  test('auf dem Handy stehen die Beträge unter „Monat" und „Jahr", ohne waagerechtes Scrollen (TP-11)', async ({
    page,
  }) => {
    await addExpense(page, 'Haftpflichtversicherung für die ganze Familie', '120', 'Lebensunterhalt');
    await page.setViewportSize({ width: 390, height: 844 });
    await showTopItems(page, 'Ausgaben');

    const row = topItemRows(page).first();
    const monthly = (await topItems(page).getByRole('columnheader', { name: 'Monat' }).boundingBox())!;
    const yearly = (await topItems(page).getByRole('columnheader', { name: 'Jahr' }).boundingBox())!;
    const monthlyValue = (await row.locator('td.monthly').boundingBox())!;
    const yearlyValue = (await row.locator('td.yearly').boundingBox())!;
    expect(monthlyValue.x + monthlyValue.width).toBeCloseTo(monthly.x + monthly.width, 0);
    expect(yearlyValue.x + yearlyValue.width).toBeCloseTo(yearly.x + yearly.width, 0);
    expect(yearlyValue.x + yearlyValue.width).toBeLessThanOrEqual(390);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
      ),
    ).toBe(false);
  });
});
