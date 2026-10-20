import { expect, Page, test } from '@playwright/test';
import { addExpense, addIncome, itemRow, openEditDialog, registerWithPlan } from './support';

function note(page: Page, description: string) {
  return itemRow(page, description).locator('.note');
}

async function editNote(page: Page, description: string, text: string) {
  const dialog = await openEditDialog(page, description);
  await dialog.getByLabel('Notiz (optional)').fill(text);
  await dialog.getByRole('button', { name: 'Speichern' }).click();
  await expect(dialog).toBeHidden();
}

// docs/specs/posten-notiz.md
test.describe('Notiz zum Posten', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('eine Ausgabe mit Notiz erfassen zeigt die Notiz unter der Beschreibung (PN-1, PN-4, PN-7)', async ({
    page,
  }) => {
    await page.getByLabel('Notiz (optional)').fill('Warmmiete inkl. Nebenkosten');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');

    await expect(note(page, 'Miete')).toHaveText('Warmmiete inkl. Nebenkosten');
    await expect(page.getByLabel('Notiz (optional)')).toHaveValue('');

    // Saved on the server
    await page.reload();
    await expect(note(page, 'Miete')).toHaveText('Warmmiete inkl. Nebenkosten');
  });

  test('ein Posten ohne oder mit leerer Notiz zeigt keine Notiz (PN-1, PN-3)', async ({ page }) => {
    await addIncome(page, 'Gehalt', '3000');
    await page.getByLabel('Notiz (optional)').fill('   ');
    await addIncome(page, 'Zinsen', '20');

    await expect(note(page, 'Gehalt')).toHaveCount(0);
    await expect(note(page, 'Zinsen')).toHaveCount(0);
  });

  test('die Notiz beim Bearbeiten ändern und entfernen (PN-4, PN-7)', async ({ page }) => {
    await page.getByLabel('Notiz (optional)').fill('netto');
    await addIncome(page, 'Gehalt', '3000');

    const dialog = await openEditDialog(page, 'Gehalt');
    await expect(dialog.getByLabel('Notiz (optional)')).toHaveValue('netto');
    await dialog.getByRole('button', { name: 'Abbrechen' }).click();
    await expect(dialog).toBeHidden();

    await editNote(page, 'Gehalt', 'netto, nach Steuern');
    await expect(note(page, 'Gehalt')).toHaveText('netto, nach Steuern');
    await expect(itemRow(page, 'Gehalt')).toContainText('+3.000,00 €');

    await editNote(page, 'Gehalt', '');
    await expect(note(page, 'Gehalt')).toHaveCount(0);
    await page.reload();
    await expect(itemRow(page, 'Gehalt')).toBeVisible();
    await expect(note(page, 'Gehalt')).toHaveCount(0);
  });

  test('eine Notiz mit mehr als 1000 Zeichen wird nicht gesendet (PN-2)', async ({ page }) => {
    await addIncome(page, 'Gehalt', '3000');
    const requests: string[] = [];
    page.on('request', (request) => {
      if (['POST', 'PUT'].includes(request.method())) requests.push(request.url());
    });
    const tooLong = 'x'.repeat(1001);

    await page.getByRole('radio', { name: 'Einnahme' }).click();
    await page.getByLabel('Beschreibung').fill('Zinsen');
    await page.getByLabel('Betrag').fill('20');
    await page.getByLabel('Notiz (optional)').fill(tooLong);
    await page.getByRole('button', { name: 'Hinzufügen', exact: true }).click();
    await expect(page.getByText('Höchstens 1000 Zeichen.')).toBeVisible();

    const dialog = await openEditDialog(page, 'Gehalt');
    await dialog.getByLabel('Notiz (optional)').fill(tooLong);
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog.getByText('Höchstens 1000 Zeichen.')).toBeVisible();

    expect(requests).toEqual([]);
  });

  test('Rückgängig nach dem Löschen stellt auch die Notiz wieder her (PN-8)', async ({ page }) => {
    await page.getByLabel('Notiz (optional)').fill('Warmmiete');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');

    await page.getByRole('button', { name: 'Eintrag Miete löschen' }).click();
    await expect(itemRow(page, 'Miete')).toBeHidden();
    await page.getByRole('button', { name: 'Rückgängig' }).click();

    await expect(note(page, 'Miete')).toHaveText('Warmmiete');
    await page.reload();
    await expect(note(page, 'Miete')).toHaveText('Warmmiete');
  });
});
