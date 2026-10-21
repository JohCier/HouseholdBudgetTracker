import { expect, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  itemRow,
  pieEntry,
  pieLegendItem,
  pieSlices,
  pieSummary,
  registerWithPlan,
  showBreakdownAs,
  showInPie,
} from './support';

const PHONE = { width: 360, height: 780 };

// docs/specs/torte-ausschnitt.md
test.describe('Ausschnitt der Torte', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('die Torte zeigt zuerst die 50/30/20-Regel (TK-1)', async ({ page }) => {
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Miete', 'Monatlich');

    await showBreakdownAs(page, 'Torte');

    await expect(page.getByRole('combobox', { name: 'Ausschnitt' })).toContainText(
      '50/30/20-Regel',
    );
    await expect(pieLegendItem(page, 'Lebensunterhalt')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Zurück' })).toHaveCount(0);
  });

  test('eine Kategorie zeigt ihre Unterkategorien mit Anteil (TK-1, TK-2, TK-6 bis TK-8)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Miete', 'Monatlich');
    await addExpense(page, 'Sprit', '200', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'Versicherung', '50', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await showBreakdownAs(page, 'Torte');

    await showInPie(page, 'Lebensunterhalt');

    await expect(pieSlices(page)).toHaveCount(2);
    await expect(page.locator('app-category-pie .legend-item .name')).toHaveText(['Auto', 'Miete']);
    await expect(pieEntry(page, 'Auto')).toContainText('250,00 € · 20 %');
    await expect(pieEntry(page, 'Miete')).toContainText('1.000,00 € · 80 %');
    await expect(pieSummary(page)).toContainText('Lebensunterhalt');
    await expect(pieSummary(page)).toContainText('1.250,00 €');
    await expect(pieSummary(page)).toContainText('41,67 % des Einkommens von max. 50 %');
  });

  test('eine Unterkategorie zeigt ihre Posten mit Anteil (TK-1, TK-6, TK-8)', async ({ page }) => {
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Miete', 'Monatlich');
    await addExpense(page, 'Sprit', '200', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'Versicherung', '50', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await showBreakdownAs(page, 'Torte');

    await showInPie(page, 'Lebensunterhalt', 'Auto');

    await expect(pieSlices(page)).toHaveCount(2);
    await expect(pieSummary(page)).toContainText('Lebensunterhalt › Auto');
    await expect(pieEntry(page, 'Sprit')).toContainText('200,00 € · 80 %');
    await expect(pieEntry(page, 'Versicherung')).toContainText('50,00 € · 20 %');
  });

  test('jährliche Posten zählen mit ihrem Monatsbetrag (TK-6, TK-8)', async ({ page }) => {
    await addExpense(page, 'Kfz-Steuer', '120', 'Lebensunterhalt', 'Auto', 'Jährlich');
    await addExpense(page, 'Sprit', '30', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await showBreakdownAs(page, 'Torte');

    await showInPie(page, 'Lebensunterhalt', 'Auto');

    await expect(pieEntry(page, 'Kfz-Steuer')).toContainText('10,00 € · 25 %');
    await expect(pieEntry(page, 'Sprit')).toContainText('30,00 € · 75 %');
  });

  test('ein Klick auf ein Stück springt hinein, „Zurück" eine Ebene hinauf (TK-4, TK-12)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Miete', 'Monatlich');
    await addExpense(page, 'Sprit', '200', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await showBreakdownAs(page, 'Torte');
    const selection = page.getByRole('combobox', { name: 'Ausschnitt' });

    await page.getByRole('button', { name: /Lebensunterhalt anzeigen$/ }).click();
    await expect(selection).toContainText('Lebensunterhalt');
    await page.getByRole('button', { name: /Lebensunterhalt › Auto anzeigen$/ }).click();
    await expect(selection).toContainText('Lebensunterhalt › Auto');
    await expect(pieEntry(page, 'Sprit')).toBeVisible();

    await page.getByRole('button', { name: 'Zurück' }).click();
    await expect(selection).toHaveText(/^\s*Lebensunterhalt\s*$/);
    await page.getByRole('button', { name: 'Zurück' }).click();
    await expect(selection).toContainText('50/30/20-Regel');
  });

  test('dieselbe Unterkategorie in zwei Kategorien sind zwei Ausschnitte (TK-2, TK-3)', async ({
    page,
  }) => {
    await addExpense(page, 'Sprit', '200', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'Ausflug', '50', 'Freizeit & Spaß', 'Auto', 'Monatlich');
    await showBreakdownAs(page, 'Torte');

    await showInPie(page, 'Lebensunterhalt', 'Auto');
    await expect(pieEntry(page, 'Sprit')).toBeVisible();
    await expect(pieEntry(page, 'Ausflug')).toHaveCount(0);

    await showInPie(page, 'Freizeit & Spaß', 'Auto');
    await expect(pieEntry(page, 'Ausflug')).toContainText('50,00 € · 100 %');
    await expect(pieEntry(page, 'Sprit')).toHaveCount(0);
  });

  test('bei Sparen & Investieren ist der Überschuss ein graues Stück (TK-9, TK-10)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'ETF', '500', 'Sparen & Investieren', 'Investieren', 'Monatlich');
    await showBreakdownAs(page, 'Torte');

    await showInPie(page, 'Sparen & Investieren');

    await expect(pieSlices(page)).toHaveCount(2);
    await expect(pieEntry(page, 'Investieren')).toContainText('500,00 € · 16,67 %');
    await expect(pieEntry(page, 'nicht verplant')).toContainText('2.500,00 € · 83,33 %');
    await expect(pieSummary(page)).toContainText('3.000,00 €');
  });

  test('der Ausschnitt bleibt beim Erfassen und springt zurück, wenn die Gruppe leer ist (TK-5)', async ({
    page,
  }) => {
    await addExpense(page, 'Sprit', '200', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Miete', 'Monatlich');
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Lebensunterhalt', 'Auto');
    await expect(pieSlices(page)).toHaveCount(1);

    await addExpense(page, 'Wartung', '40', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await expect(pieSummary(page)).toContainText('Lebensunterhalt › Auto');
    await expect(pieSlices(page)).toHaveCount(2);

    await page.getByRole('button', { name: 'Eintrag Sprit löschen' }).click();
    await expect(itemRow(page, 'Sprit')).toBeHidden();
    await page.getByRole('button', { name: 'Eintrag Wartung löschen' }).click();
    await expect(itemRow(page, 'Wartung')).toBeHidden();

    await expect(page.getByRole('combobox', { name: 'Ausschnitt' })).toHaveText(
      /^\s*Lebensunterhalt\s*$/,
    );
    await expect(pieEntry(page, 'Miete')).toBeVisible();
  });

  test('der Ausschnitt bleibt beim Wechsel der Darstellung und des Jahres (TK-5)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Miete', 'Monatlich');
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Lebensunterhalt');

    await showBreakdownAs(page, 'Balken');
    await showBreakdownAs(page, 'Torte');
    await expect(pieSummary(page)).toContainText('Lebensunterhalt');

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();
    await expect(pieSummary(page)).toContainText('Lebensunterhalt');
    await expect(pieEntry(page, 'Miete')).toBeVisible();
  });

  test('eine Kategorie ohne Ausgaben zeigt einen leeren Kreis (TK-11)', async ({ page }) => {
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', 'Miete', 'Monatlich');
    await showBreakdownAs(page, 'Torte');

    await showInPie(page, 'Freizeit & Spaß');

    await expect(pieSlices(page)).toHaveCount(0);
    await expect(page.getByText('Keine Ausgaben in Freizeit & Spaß')).toBeVisible();
  });

  test('auf dem Handy passt ein Ausschnitt auf den Bildschirm (TK-13, MA-1)', async ({ page }) => {
    await addExpense(
      page,
      'Versicherung für das Auto mit Vollkasko',
      '50',
      'Lebensunterhalt',
      'Auto',
      'Monatlich',
    );
    await page.setViewportSize(PHONE);
    await showBreakdownAs(page, 'Torte');

    await showInPie(page, 'Lebensunterhalt', 'Auto');

    await expect(pieEntry(page, 'Versicherung für das Auto mit Vollkasko')).toBeVisible();
    const scrollsSideways = await page.evaluate(
      () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
    );
    expect(scrollsSideways).toBe(false);
  });
});
