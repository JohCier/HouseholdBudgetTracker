import { expect, test } from '@playwright/test';
import { addExpense, addIncome, categoryRow, registerWithPlan, balanceSheet } from './support';

// docs/specs/auswertung-50-30-20.md
test.describe('Auswertung nach der 50/30/20-Regel', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('ein leerer Plan hat nur Nullen, und alle Ziele gelten als erreicht (AW-7, AW-8)', async ({
    page,
  }) => {
    await expect(balanceSheet(page, 'Gesamteinnahmen')).toHaveText('0,00 €');
    await expect(balanceSheet(page, 'Gesamtausgaben')).toHaveText('0,00 €');
    await expect(balanceSheet(page, 'Saldo')).toHaveText('0,00 €');

    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('0 % von max. 50 %');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('0 % von max. 30 %');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText('0 % von mind. 20 %');
    await expect(page.getByText(/Ziel (überschritten|nicht erreicht)/)).toHaveCount(0);
  });

  test('ein ausgeglichener Plan hält alle drei Ziele ein (AW-1, AW-2, AW-5, AW-6)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '1500', 'Lebensunterhalt', undefined, 'Monatlich');
    await addExpense(page, 'Urlaub', '600', 'Freizeit & Spaß', undefined, 'Monatlich');
    await addExpense(page, 'ETF-Sparplan', '900', 'Sparen & Investieren', undefined, 'Monatlich');

    await expect(balanceSheet(page, 'Gesamteinnahmen', 'Monat')).toHaveText('3.000,00 €');
    await expect(balanceSheet(page, 'Gesamtausgaben', 'Monat')).toHaveText('3.000,00 €');
    await expect(balanceSheet(page, 'Saldo', 'Monat')).toHaveText('0,00 €');
    // Equal income and expenses are no deficit (AW-3)
    await expect(page.getByText('Deine Ausgaben übersteigen deine Einnahmen.')).toBeHidden();

    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('1.500,00 €');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('50 % von max. 50 %');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('20 % von max. 30 %');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText('30 % von mind. 20 %');
    await expect(page.getByText(/Ziel (überschritten|nicht erreicht)/)).toHaveCount(0);
  });

  test('zu hohe Kosten überschreiten das Ziel, der Rest zählt als Freizeit & Spaß (AW-6, AW-10)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '1200', 'Lebensunterhalt', undefined, 'Monatlich');

    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText(
      '60 % von max. 50 % · Ziel überschritten',
    );
    // The 800,00 € not spent count towards leisure
    const freizeit = categoryRow(page, 'Freizeit & Spaß');
    await expect(freizeit).toContainText('davon nicht verplant: 800,00 €');
    await expect(freizeit).toContainText('40 % von max. 30 % · Ziel überschritten');
    const sparen = categoryRow(page, 'Sparen & Investieren');
    await expect(sparen).toContainText('0 % von mind. 20 % · Ziel nicht erreicht');
    await expect(sparen).not.toContainText('nicht verplant:');
  });

  test('der Überschuss zählt zu Freizeit & Spaß und wird als nicht verplant ausgewiesen (AW-10)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', undefined, 'Monatlich');
    await addExpense(page, 'Urlaub', '300', 'Freizeit & Spaß', undefined, 'Monatlich');
    await addExpense(page, 'ETF-Sparplan', '400', 'Sparen & Investieren', undefined, 'Monatlich');

    await expect(balanceSheet(page, 'Gesamtausgaben', 'Monat')).toHaveText('1.700,00 €');
    await expect(balanceSheet(page, 'Saldo', 'Monat')).toHaveText('300,00 €');
    const freizeit = categoryRow(page, 'Freizeit & Spaß');
    await expect(freizeit).toContainText('600,00 €');
    await expect(freizeit).toContainText('davon nicht verplant: 300,00 €');
    await expect(freizeit).toContainText('30 % von max. 30 %');
    await expect(freizeit).not.toContainText('Ziel überschritten');
    const sparen = categoryRow(page, 'Sparen & Investieren');
    await expect(sparen).toContainText('400,00 €');
    await expect(sparen).toContainText('20 % von mind. 20 %');
    await expect(sparen).not.toContainText('nicht verplant');
  });

  test('Ausgaben ohne Einnahmen sind ein Defizit (AW-2, AW-3, AW-7)', async ({ page }) => {
    await addExpense(page, 'Kino', '100', 'Freizeit & Spaß');

    await expect(page.getByText('Deine Ausgaben übersteigen deine Einnahmen.')).toBeVisible();
    await expect(balanceSheet(page, 'Saldo')).toHaveText('-100,00 €');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText(
      '0 % von max. 30 % · Ziel überschritten',
    );
  });
});
