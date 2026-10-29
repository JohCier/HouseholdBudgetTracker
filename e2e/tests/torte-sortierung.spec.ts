import { expect, Page, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  pieNames,
  pieSlices,
  registerWithPlan,
  showBreakdownAs,
  showInPie,
  sortPie,
} from './support';

const PHONE = { width: 360, height: 780 };

// docs/specs/torte-sortierung.md
test.describe('Sortierung der Torte', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  /** Lebensunterhalt: "Auto" 250 €, "Miete" 1.000 € and "Versicherungen" 80 € per month. */
  async function addLiving(page: Page) {
    await addExpense(page, 'Sprit', '250', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'Kaltmiete', '1000', 'Lebensunterhalt', 'Miete', 'Monatlich');
    await addExpense(page, 'Haftpflicht', '80', 'Lebensunterhalt', 'Versicherungen', 'Monatlich');
  }

  test('zuerst gilt die Reihenfolge der Einträge (TS-1)', async ({ page }) => {
    await addLiving(page);
    await showBreakdownAs(page, 'Torte');

    await showInPie(page, 'Lebensunterhalt');

    await expect(page.getByRole('combobox', { name: 'Sortierung' })).toContainText('Einträge');
    await expect(pieNames(page)).toHaveText(['Auto', 'Miete', 'Versicherungen']);
    await expect(page.getByRole('button', { name: /^Richtung/ })).toHaveCount(0);
  });

  test('die 50/30/20-Regel hat keine Sortierung (TS-1)', async ({ page }) => {
    await addLiving(page);

    await showBreakdownAs(page, 'Torte');

    await expect(page.getByRole('combobox', { name: 'Sortierung' })).toHaveCount(0);
  });

  test('nach Betrag steht das größte Stück vorne (TS-1, TS-2, TS-4)', async ({ page }) => {
    await addLiving(page);
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Lebensunterhalt');

    await sortPie(page, 'Betrag');

    await expect(pieNames(page)).toHaveText(['Miete', 'Auto', 'Versicherungen']);
    await expect(pieSlices(page).first()).toHaveAttribute('aria-label', /^Miete:/);
    await expect(page.getByRole('button', { name: 'Richtung: absteigend' })).toBeVisible();
  });

  test('der Pfeil kehrt die Richtung um (TS-2)', async ({ page }) => {
    await addLiving(page);
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Lebensunterhalt');
    await sortPie(page, 'Betrag');

    await page.getByRole('button', { name: 'Richtung: absteigend' }).click();

    await expect(pieNames(page)).toHaveText(['Versicherungen', 'Auto', 'Miete']);
    await expect(page.getByRole('button', { name: 'Richtung: aufsteigend' })).toBeVisible();
  });

  test('nach Name ohne Rücksicht auf Groß- und Kleinschreibung (TS-1, TS-3)', async ({ page }) => {
    await addExpense(page, 'Versicherung', '50', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'sprit', '200', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'Kfz-Steuer', '120', 'Lebensunterhalt', 'Auto', 'Jährlich');
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Lebensunterhalt', 'Auto');

    await sortPie(page, 'Name');

    await expect(pieNames(page)).toHaveText(['Kfz-Steuer', 'sprit', 'Versicherung']);
  });

  test('„nicht verplant" bleibt am Ende (TS-5)', async ({ page }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Reise', '200', 'Freizeit & Spaß', 'Urlaub', 'Monatlich');
    await addExpense(page, 'Verein', '300', 'Freizeit & Spaß', 'Hobby', 'Monatlich');
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Freizeit & Spaß');
    await sortPie(page, 'Betrag');

    await page.getByRole('button', { name: 'Richtung: absteigend' }).click();

    await expect(pieNames(page)).toHaveText(['Urlaub', 'Hobby', 'nicht verplant']);
  });

  test('die Sortierung bleibt beim Wechsel von Ausschnitt, Darstellung und Jahr (TS-6)', async ({
    page,
  }) => {
    await addLiving(page);
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Lebensunterhalt');
    await sortPie(page, 'Betrag');
    const sort = page.getByRole('combobox', { name: 'Sortierung' });

    await showInPie(page, 'Lebensunterhalt', 'Auto');
    await expect(sort).toContainText('Betrag');

    await showBreakdownAs(page, 'Balken');
    await showBreakdownAs(page, 'Torte');
    await expect(sort).toContainText('Betrag');

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();
    await showInPie(page, 'Lebensunterhalt');
    await expect(sort).toContainText('Betrag');
    await expect(pieNames(page)).toHaveText(['Miete', 'Auto', 'Versicherungen']);
  });

  test('die Einträge und die Auswahl „Ausschnitt" behalten ihre Reihenfolge (TS-7)', async ({
    page,
  }) => {
    await addLiving(page);
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Lebensunterhalt');

    await sortPie(page, 'Betrag');

    await page.getByRole('combobox', { name: 'Ausschnitt' }).click();
    await expect(page.getByRole('option')).toHaveText([
      '50/30/20-Regel',
      'Lebensunterhalt',
      'Auto',
      'Miete',
      'Versicherungen',
      'Freizeit & Spaß',
      'Sparen & Investieren',
    ]);
  });

  test('auf dem Handy passt die Sortierung auf den Bildschirm (TS-8, MA-1)', async ({ page }) => {
    await addLiving(page);
    await page.setViewportSize(PHONE);
    await showBreakdownAs(page, 'Torte');
    await showInPie(page, 'Lebensunterhalt');

    await sortPie(page, 'Betrag');

    await expect(page.getByRole('button', { name: 'Richtung: absteigend' })).toBeVisible();
    const scrollsSideways = await page.evaluate(
      () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
    );
    expect(scrollsSideways).toBe(false);
  });
});
