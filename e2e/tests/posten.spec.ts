import { expect, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  itemIn,
  itemDescriptions,
  itemRow,
  registerWithPlan,
  openAddItemDialog,
} from './support';

// docs/specs/posten.md
test.describe('Posten', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('eine Ausgabe mit Kategorie und eine Einnahme ohne Kategorie erfassen (PO-1, PO-5)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    await addIncome(page, 'Gehalt', '3000');

    await expect(itemIn(page, 'Lebensunterhalt', 'Miete')).toBeVisible();
    await expect(itemRow(page, 'Miete')).toContainText('−850,00 €');
    await expect(itemIn(page, 'Einnahmen', 'Gehalt')).toBeVisible();
    await expect(itemRow(page, 'Gehalt')).toContainText('+3.000,00 €');
  });

  test('„Posten erfassen" öffnet einen leeren Dialog für eine monatliche Ausgabe (PO-1, AP-14)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');

    await expect(page.locator('app-add-item-form')).toHaveCount(0);
    const dialog = await openAddItemDialog(page);
    await expect(dialog.getByRole('heading')).toHaveText('Posten erfassen');
    await expect(dialog.getByLabel('Beschreibung')).toHaveValue('');
    await expect(dialog.getByLabel('Betrag')).toHaveValue('');
    await expect(dialog.locator('mat-error')).toHaveCount(0);
    await expect(dialog.getByRole('radio', { name: 'Ausgabe', exact: true })).toBeChecked();
    await expect(dialog.getByRole('radio', { name: 'Monatlich' })).toBeChecked();
  });

  test('„Abbrechen" im Dialog „Posten erfassen" legt nichts an (AP-15)', async ({ page }) => {
    const dialog = await openAddItemDialog(page);
    await dialog.getByLabel('Beschreibung').fill('Miete');
    await dialog.getByRole('button', { name: 'Abbrechen' }).click();

    await expect(dialog).toBeHidden();
    await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
  });

  test('der neueste Posten steht unten in seiner Gruppe (PO-10, EG-1)', async ({ page }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    await addExpense(page, 'Strom', '60', 'Lebensunterhalt');
    await addExpense(page, 'Wasser', '30', 'Lebensunterhalt');

    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Sonstiges')).toHaveText([
      'Miete',
      'Strom',
      'Wasser',
    ]);
  });

  test('ungültige Beträge werden abgelehnt, ohne die Anfrage zu senden (PO-4)', async ({
    page,
  }) => {
    const requests: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST') requests.push(request.url());
    });
    const dialog = await openAddItemDialog(page);
    await dialog.getByRole('radio', { name: 'Einnahme' }).click();
    await dialog.getByLabel('Beschreibung').fill('Gehalt');

    for (const amount of ['0', '-5']) {
      await dialog.getByLabel('Betrag').fill(amount);
      await dialog.getByRole('button', { name: 'Hinzufügen', exact: true }).click();
      await expect(page.getByText('Bitte einen positiven Betrag angeben.')).toBeVisible();
    }

    await dialog.getByLabel('Betrag').fill('10.005');
    await dialog.getByRole('button', { name: 'Hinzufügen', exact: true }).click();
    await expect(page.getByText('Höchstens zwei Nachkommastellen.')).toBeVisible();

    expect(requests).toEqual([]);
    await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
  });

  test('eine Ausgabe braucht eine Kategorie (PO-5)', async ({ page }) => {
    const dialog = await openAddItemDialog(page);
    await dialog.getByLabel('Beschreibung').fill('Miete');
    await dialog.getByLabel('Betrag').fill('850');
    await dialog.getByRole('button', { name: 'Hinzufügen', exact: true }).click();

    await expect(page.getByText('Bitte eine Kategorie wählen.')).toBeVisible();
    await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
  });

  test('einen Posten löschen und das Löschen rückgängig machen (PO-9)', async ({ page }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');

    await page.getByRole('button', { name: 'Eintrag Miete löschen' }).click();
    await expect(itemRow(page, 'Miete')).toBeHidden();
    await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();

    await page.getByRole('button', { name: 'Rückgängig' }).click();
    await expect(itemIn(page, 'Lebensunterhalt', 'Miete')).toBeVisible();
    await expect(itemRow(page, 'Miete')).toContainText('−850,00 €');

    // Restored on the server, not only in the browser
    await page.reload();
    await expect(itemRow(page, 'Miete')).toBeVisible();
  });

  test('gelöschte Posten bleiben nach dem Neuladen gelöscht', async ({ page }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    await page.getByRole('button', { name: 'Eintrag Miete löschen' }).click();
    await expect(itemRow(page, 'Miete')).toBeHidden();

    await page.reload();
    await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
  });
});
