import { expect, test } from '@playwright/test';
import { addExpense, addIncome, categoryRow, registerWithPlan, tile } from './support';

// docs/specs/auswertung-50-30-20.md
test.describe('Auswertung nach der 50/30/20-Regel', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('ein leerer Plan hat nur Nullen, und alle Ziele gelten als erreicht (AW-7, AW-8)', async ({
    page,
  }) => {
    await expect(tile(page, 'Einnahmen')).toHaveText('0,00 €');
    await expect(tile(page, 'Ausgaben')).toHaveText('0,00 €');
    await expect(tile(page, 'Saldo')).toHaveText('0,00 €');

    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('0 % von max. 50 %');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('0 % von max. 30 %');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText('0 % von mind. 20 %');
    await expect(page.getByText(/Ziel (überschritten|nicht erreicht)/)).toHaveCount(0);
  });

  test('ein ausgeglichener Plan hält alle drei Ziele ein (AW-1, AW-2, AW-5, AW-6)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000');
    await addExpense(page, 'Miete', '1500', 'Lebensunterhalt');
    await addExpense(page, 'Urlaub', '600', 'Freizeit & Spaß');
    await addExpense(page, 'ETF-Sparplan', '900', 'Sparen & Investieren');

    await expect(tile(page, 'Einnahmen')).toHaveText('3.000,00 €');
    await expect(tile(page, 'Ausgaben')).toHaveText('3.000,00 €');
    await expect(tile(page, 'Saldo')).toHaveText('0,00 €');
    // Equal income and expenses are no deficit (AW-3)
    await expect(page.getByText('Deine Ausgaben übersteigen deine Einnahmen.')).toBeHidden();

    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('1.500,00 €');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('50 % von max. 50 %');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('20 % von max. 30 %');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText('30 % von mind. 20 %');
    await expect(page.getByText(/Ziel (überschritten|nicht erreicht)/)).toHaveCount(0);
  });

  test('zu hohe Kosten überschreiten das Ziel, fehlendes Sparen verfehlt es (AW-6)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000');
    await addExpense(page, 'Miete', '1200', 'Lebensunterhalt');

    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText(
      '60 % von max. 50 % · Ziel überschritten',
    );
    await expect(categoryRow(page, 'Freizeit & Spaß')).not.toContainText('Ziel');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText(
      '0 % von mind. 20 % · Ziel nicht erreicht',
    );
  });

  test('Ausgaben ohne Einnahmen sind ein Defizit (AW-2, AW-3, AW-7)', async ({ page }) => {
    await addExpense(page, 'Kino', '100', 'Freizeit & Spaß');

    await expect(page.getByText('Deine Ausgaben übersteigen deine Einnahmen.')).toBeVisible();
    await expect(tile(page, 'Saldo')).toHaveText('-100,00 €');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText(
      '0 % von max. 30 % · Ziel überschritten',
    );
  });
});
