import { expect, test } from '@playwright/test';
import { addExpense, addIncome, itemRow, openEditDialog, registerWithPlan, tile } from './support';

// docs/specs/antwort-mit-uebersicht.md
test.describe('Übersicht als Antwort auf Änderungen', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('nach Erfassen, Ändern und Löschen zeigt die Oberfläche die neue Übersicht ohne erneuten Abruf (AU-5)', async ({
    page,
  }) => {
    const overviewLoads: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'GET' && request.url().includes('/api/plans?')) {
        overviewLoads.push(request.url());
      }
    });

    await addIncome(page, 'Gehalt', '3000');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    await expect(tile(page, 'Ausgaben')).toHaveText('850,00 €');

    const dialog = await openEditDialog(page, 'Miete');
    await dialog.getByLabel('Betrag').fill('900');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(itemRow(page, 'Miete')).toContainText('−900,00 €');
    await expect(tile(page, 'Ausgaben')).toHaveText('900,00 €');

    await page.getByRole('button', { name: 'Eintrag Miete löschen' }).click();
    await expect(itemRow(page, 'Miete')).toBeHidden();
    await expect(tile(page, 'Ausgaben')).toHaveText('0,00 €');
    await expect(tile(page, 'Saldo')).toHaveText('3.000,00 €');

    expect(overviewLoads).toEqual([]);
  });

  test('Rückgängig nach einem Jahreswechsel lässt das gewählte Jahr stehen (AU-5, PO-9)', async ({
    page,
  }) => {
    const year = new Date().getFullYear();
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    await page.getByRole('button', { name: 'Eintrag Miete löschen' }).click();
    await expect(itemRow(page, 'Miete')).toBeHidden();

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await expect(page.getByText(`Für ${year + 1} gibt es noch keinen Haushaltsplan.`)).toBeVisible();
    await page.getByRole('button', { name: 'Rückgängig' }).click();

    await expect(page.getByRole('heading', { name: String(year + 1) })).toBeVisible();
    await expect(page.getByText(`Für ${year + 1} gibt es noch keinen Haushaltsplan.`)).toBeVisible();
    await expect(itemRow(page, 'Miete')).toBeHidden();

    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();
    await expect(itemRow(page, 'Miete')).toContainText('−850,00 €');
  });
});
