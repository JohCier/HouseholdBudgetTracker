import { expect, Locator, Page, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  categoryRow,
  itemGroup,
  itemRow,
  openEditDialog,
  pieLegendItem,
  registerWithPlan,
  showBreakdownAs,
  tile,
} from './support';

/** The amount of an item per month or per year in the card "Einträge" (MJ-12). */
function itemAmount(page: Page, description: string, per: 'Monat' | 'Jahr') {
  return itemRow(page, description).locator(per === 'Monat' ? '.amount.monthly' : '.amount.yearly');
}

/** The toggle "Monatlich | Jährlich" of the form or of the edit dialog (MJ-11). */
function periodToggle(scope: Page | Locator) {
  return scope.getByRole('radiogroup', {
    name: 'Angabe monatlich oder jährlich',
  });
}

// docs/specs/monats-jahresbetrag.md
test.describe('Monats- und Jahresbetrag', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('eine monatliche Ausgabe zählt zwölffach im Jahr, der eingegebene Betrag ist hervorgehoben (MJ-6, MJ-8, MJ-12)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '40000');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', undefined, 'Monatlich');

    await expect(itemAmount(page, 'Miete', 'Monat')).toHaveText('−850,00 €');
    await expect(itemAmount(page, 'Miete', 'Jahr')).toHaveText('−10.200,00 €');
    await expect(itemAmount(page, 'Miete', 'Monat')).not.toHaveClass(/derived/);
    await expect(itemAmount(page, 'Miete', 'Jahr')).toHaveClass(/derived/);
    // The 50/30/20 card shows the total per month (MJ-14)
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('850,00 €');
  });

  test('eine jährliche Ausgabe zeigt ein Zwölftel, auf den Cent gerundet, je Monat (MJ-7, MJ-12)', async ({
    page,
  }) => {
    await addExpense(page, 'Versicherung', '1000', 'Lebensunterhalt');

    await expect(itemAmount(page, 'Versicherung', 'Monat')).toHaveText('−83,33 €');
    await expect(itemAmount(page, 'Versicherung', 'Jahr')).toHaveText('−1.000,00 €');
    await expect(itemAmount(page, 'Versicherung', 'Monat')).toHaveClass(/derived/);
    await expect(itemAmount(page, 'Versicherung', 'Jahr')).not.toHaveClass(/derived/);
  });

  test('die Kacheln zeigen Einnahmen, Ausgaben und Saldo je Monat und je Jahr (MJ-9, MJ-13)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', undefined, 'Monatlich');

    await expect(tile(page, 'Einnahmen', 'Monat')).toHaveText('3.000,00 €');
    await expect(tile(page, 'Einnahmen', 'Jahr')).toHaveText('36.000,00 €');
    await expect(tile(page, 'Ausgaben', 'Monat')).toHaveText('850,00 €');
    await expect(tile(page, 'Ausgaben', 'Jahr')).toHaveText('10.200,00 €');
    await expect(tile(page, 'Saldo', 'Monat')).toHaveText('2.150,00 €');
    await expect(tile(page, 'Saldo', 'Jahr')).toHaveText('25.800,00 €');
  });

  test('Summen je Monat sind die Jahressumme durch zwölf (MJ-9, MJ-12)', async ({ page }) => {
    await addExpense(page, 'Zeitung', '100', 'Freizeit & Spaß', 'Abos');
    await addExpense(page, 'Podcast', '100', 'Freizeit & Spaß', 'Abos');
    await addExpense(page, 'Magazin', '100', 'Freizeit & Spaß', 'Abos');

    await expect(itemAmount(page, 'Podcast', 'Monat')).toHaveText('−8,33 €');
    const header = itemGroup(page, 'Freizeit & Spaß', 'Abos').locator('.group-header');
    await expect(header.locator('.total.monthly')).toHaveText('25,00 €');
    await expect(header.locator('.total.yearly')).toHaveText('300,00 €');
  });

  test('das Formular beginnt mit „Monatlich" und behält die gewählte Angabe (MJ-1, MJ-11)', async ({
    page,
  }) => {
    const toggle = periodToggle(page);
    await expect(toggle.getByRole('radio', { name: 'Monatlich' })).toBeChecked();

    await addExpense(page, 'Versicherung', '1000', 'Lebensunterhalt', undefined, 'Jährlich');

    await expect(toggle.getByRole('radio', { name: 'Jährlich' })).toBeChecked();
  });

  test('beim Bearbeiten wird die Angabe umgestellt, ohne den Betrag umzurechnen (MJ-4, MJ-5, MJ-11)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', undefined, 'Monatlich');

    const dialog = await openEditDialog(page, 'Miete');
    await expect(dialog.getByLabel('Betrag')).toHaveValue('850');
    const toggle = periodToggle(dialog);
    await expect(toggle.getByRole('radio', { name: 'Monatlich' })).toBeChecked();
    await toggle.getByRole('radio', { name: 'Jährlich' }).click();
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemAmount(page, 'Miete', 'Jahr')).toHaveText('−850,00 €');
    await expect(itemAmount(page, 'Miete', 'Monat')).toHaveText('−70,83 €');
    await expect(tile(page, 'Ausgaben', 'Jahr')).toHaveText('850,00 €');

    // Saved on the server, with its new period
    await page.reload();
    await expect(itemAmount(page, 'Miete', 'Jahr')).not.toHaveClass(/derived/);
    await expect(itemAmount(page, 'Miete', 'Jahr')).toHaveText('−850,00 €');
  });

  test('Rückgängig nach dem Löschen stellt auch die Angabe wieder her (MJ-4)', async ({ page }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', undefined, 'Monatlich');

    await page.getByRole('button', { name: 'Eintrag Miete löschen' }).click();
    await expect(itemRow(page, 'Miete')).toBeHidden();
    await page.getByRole('button', { name: 'Rückgängig' }).click();

    await expect(itemAmount(page, 'Miete', 'Monat')).toHaveText('−850,00 €');
    await expect(itemAmount(page, 'Miete', 'Monat')).not.toHaveClass(/derived/);
    await expect(itemAmount(page, 'Miete', 'Jahr')).toHaveText('−10.200,00 €');
  });

  test('die 50/30/20-Karte zeigt Monatswerte in Balken und Torte (MJ-14)', async ({ page }) => {
    await addIncome(page, 'Gehalt', '36000', undefined, 'Jährlich');
    await addExpense(page, 'Miete', '1500', 'Lebensunterhalt', undefined, 'Monatlich');

    await expect(
      page.getByText('Beträge je Monat, Anteil am Einkommen je Kategorie'),
    ).toBeVisible();
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('1.500,00 €');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('50 % von max. 50 %');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText(
      'davon nicht verplant: 1.500,00 €',
    );

    await showBreakdownAs(page, 'Torte');
    await expect(pieLegendItem(page, 'Lebensunterhalt')).toContainText('1.500,00 €');
    await expect(pieLegendItem(page, 'Sparen & Investieren')).toContainText(
      'davon nicht verplant: 1.500,00 €',
    );
    await expect(
      page.getByRole('img', {
        name: 'Lebensunterhalt: 1.500,00 € im Monat, 50 % des Einkommens',
      }),
    ).toBeVisible();
  });
});
