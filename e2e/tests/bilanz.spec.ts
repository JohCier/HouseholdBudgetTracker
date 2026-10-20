import { expect, Locator, test } from '@playwright/test';
import { addExpense, addIncome, balanceSheet, registerWithPlan } from './support';

async function box(locator: Locator) {
  const result = await locator.boundingBox();
  expect(result).not.toBeNull();
  return result!;
}

// docs/specs/bilanz.md
test.describe('Bilanz in der Übersicht', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('der Kasten „Bilanz" zeigt Gesamteinnahmen, Gesamtausgaben und Saldo je Monat und je Jahr (BI-1, BI-4)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', undefined, 'Monatlich');

    const sheet = page.getByRole('table', { name: 'Bilanz' });
    await expect(sheet.getByRole('columnheader')).toHaveText(['Monat', 'Jahr']);
    await expect(sheet.getByRole('rowheader')).toHaveText([
      'Gesamteinnahmen',
      'Gesamtausgaben',
      'Saldo',
    ]);
    await expect(balanceSheet(page, 'Gesamteinnahmen', 'Monat')).toHaveText('3.000,00 €');
    await expect(balanceSheet(page, 'Gesamtausgaben', 'Monat')).toHaveText('850,00 €');
    await expect(balanceSheet(page, 'Saldo', 'Monat')).toHaveText('2.150,00 €');
    await expect(balanceSheet(page, 'Gesamteinnahmen', 'Jahr')).toHaveText('36.000,00 €');
    await expect(balanceSheet(page, 'Gesamtausgaben', 'Jahr')).toHaveText('10.200,00 €');
    await expect(balanceSheet(page, 'Saldo', 'Jahr')).toHaveText('25.800,00 €');
  });

  test('jeder Jahreswert steht rechts neben seinem Monatswert, die Zeilen untereinander (BI-1, BI-2)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', undefined, 'Monatlich');

    let previousY = -Infinity;
    for (const label of ['Gesamteinnahmen', 'Gesamtausgaben', 'Saldo'] as const) {
      const monthly = await box(balanceSheet(page, label, 'Monat'));
      const yearly = await box(balanceSheet(page, label, 'Jahr'));
      expect(Math.abs(monthly.y - yearly.y)).toBeLessThan(4);
      expect(yearly.x).toBeGreaterThan(monthly.x + monthly.width - 1);
      expect(monthly.y).toBeGreaterThan(previousY);
      previousY = monthly.y;
    }
  });

  test('ein negativer Saldo steht in beiden Spalten in der Fehlerfarbe, darunter die Warnung (BI-3, BI-6)', async ({
    page,
  }) => {
    await addExpense(page, 'Kino', '120', 'Freizeit & Spaß');

    await expect(balanceSheet(page, 'Saldo', 'Monat')).toHaveText('-10,00 €');
    await expect(balanceSheet(page, 'Saldo', 'Monat')).toHaveClass(/expense/);
    await expect(balanceSheet(page, 'Saldo', 'Jahr')).toHaveText('-120,00 €');
    await expect(balanceSheet(page, 'Saldo', 'Jahr')).toHaveClass(/expense/);
    const warning = page.getByRole('alert').filter({
      hasText: 'Deine Ausgaben übersteigen deine Einnahmen.',
    });
    const saldo = await box(balanceSheet(page, 'Saldo', 'Jahr'));
    expect((await box(warning)).y).toBeGreaterThan(saldo.y + saldo.height);
  });

  test('auf dem Handy stehen Monat und Jahr nebeneinander, der Kasten passt auf den Bildschirm (BI-5)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '123456.78', undefined, 'Monatlich');
    await page.setViewportSize({ width: 390, height: 844 });

    const monthly = await box(balanceSheet(page, 'Gesamteinnahmen', 'Monat'));
    const yearly = await box(balanceSheet(page, 'Gesamteinnahmen', 'Jahr'));
    expect(Math.abs(monthly.y - yearly.y)).toBeLessThan(4);
    expect(yearly.x).toBeGreaterThan(monthly.x + monthly.width - 1);
    const sheet = await box(page.locator('app-overview-summary mat-card'));
    expect(sheet.x).toBeGreaterThanOrEqual(0);
    expect(sheet.x + sheet.width).toBeLessThanOrEqual(390);
    expect(yearly.x + yearly.width).toBeLessThanOrEqual(sheet.x + sheet.width);
  });
});
