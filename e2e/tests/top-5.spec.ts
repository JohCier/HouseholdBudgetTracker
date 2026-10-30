import { expect, Page, test } from '@playwright/test';
import {
  addExpense,
  registerWithPlan,
  setSavingPotential,
  showTab,
  topItemRows,
  topItemsCard,
} from './support';

type List = 'Sparpotential' | 'Ausgaben';

/** Description, monthly and yearly value of each row, e.g. "1.Miete Sonstiges | 1.000,00 € | 12.000,00 €". */
async function rowTexts(page: Page, list: List): Promise<string[][]> {
  return topItemRows(page, list).evaluateAll((rows) =>
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

  test('„Top 5 Sparpotential" zeigt die Ausgaben mit dem höchsten Sparpotential, mit Unterkategorie und Betrag der Ausgabe (TP-1, TP-3, TP-4, TP-8, TP-9, AP-11)', async ({
    page,
  }) => {
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', 'Abonnements', 'Monatlich');
    await addExpense(page, 'Fitnessstudio', '360', 'Freizeit & Spaß', 'Hobbys');
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', undefined, 'Monatlich');
    await setSavingPotential(page, 'Streaming', '5');
    await setSavingPotential(page, 'Fitnessstudio');

    await showTab(page, 'Sparhebel');
    await expect(page.getByRole('radiogroup', { name: 'Liste' })).toHaveCount(0);
    const card = topItemsCard(page, 'Sparpotential');
    await expect(card.getByRole('columnheader')).toHaveText(['Monat', 'Jahr']);
    await expect(topItemRows(page, 'Sparpotential')).toHaveCount(2);
    expect(await rowTexts(page, 'Sparpotential')).toEqual([
      ['1.Fitnessstudio Hobbys · von 30,00 €', '30,00 €', '360,00 €'],
      ['2.Streaming Abonnements · von 15,00 €', '5,00 €', '60,00 €'],
    ]);
  });

  test('„Top 5 Ausgaben" zeigt die fünf höchsten Ausgaben ohne Sparen & Investieren, jährliche nach Monatsbetrag, gleiche alphabetisch (TP-2, TP-3, TP-4)', async ({
    page,
  }) => {
    test.slow();
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Wohnen', 'Monatlich');
    await addExpense(page, 'Lebensmittel', '400', 'Lebensunterhalt', 'Essen', 'Monatlich');
    await addExpense(page, 'Kfz-Versicherung', '600', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Handy', '40', 'Lebensunterhalt', 'Abonnements', 'Monatlich');
    await addExpense(page, 'Kino', '20', 'Freizeit & Spaß', 'Ausgehen', 'Monatlich');
    await addExpense(page, 'Bücher', '20', 'Freizeit & Spaß', 'Hobbys', 'Monatlich');
    await addExpense(page, 'Zeitschrift', '10', 'Freizeit & Spaß', 'Hobbys', 'Monatlich');
    await addExpense(page, 'ETF-Sparplan', '500', 'Sparen & Investieren', undefined, 'Monatlich');

    await showTab(page, 'Sparhebel');

    await expect(topItemRows(page, 'Ausgaben')).toHaveCount(5);
    expect(await rowTexts(page, 'Ausgaben')).toEqual([
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
    await showTab(page, 'Sparhebel');
    await expect(topItemsCard(page, 'Sparpotential')).toContainText('Noch kein Sparpotential markiert');
    await expect(topItemsCard(page, 'Ausgaben')).toContainText('Noch keine Ausgaben');

    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', 'Abonnements', 'Monatlich');
    await setSavingPotential(page, 'Streaming', '5');
    await showTab(page, 'Sparhebel');

    await expect(topItemRows(page, 'Sparpotential')).toHaveCount(1);
    await expect(topItemRows(page, 'Sparpotential')).toContainText('Streaming');
  });

  test('der Tab „Sparhebel" bleibt beim Wechsel des Jahres gewählt (AP-5)', async ({ page }) => {
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', undefined, 'Monatlich');
    await page.request.post('/api/plans', { data: { year: new Date().getFullYear() + 1 } });
    await showTab(page, 'Sparhebel');

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await expect(page.getByRole('tab', { name: 'Sparhebel' })).toHaveAttribute('aria-selected', 'true');
    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();

    await expect(page.getByRole('tab', { name: 'Sparhebel' })).toHaveAttribute('aria-selected', 'true');
    await expect(topItemRows(page, 'Ausgaben')).toContainText('Miete');
  });

  test('auf dem Handy stehen die Beträge unter „Monat" und „Jahr", ohne waagerechtes Scrollen (TP-11)', async ({
    page,
  }) => {
    await addExpense(page, 'Haftpflichtversicherung für die ganze Familie', '120', 'Lebensunterhalt');
    await page.setViewportSize({ width: 390, height: 844 });
    await showTab(page, 'Sparhebel');

    const row = topItemRows(page, 'Ausgaben').first();
    const card = topItemsCard(page, 'Ausgaben');
    const monthly = (await card.getByRole('columnheader', { name: 'Monat' }).boundingBox())!;
    const yearly = (await card.getByRole('columnheader', { name: 'Jahr' }).boundingBox())!;
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
