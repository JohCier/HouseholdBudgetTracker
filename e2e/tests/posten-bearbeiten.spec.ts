import { expect, Page, test } from '@playwright/test';
import { addExpense, addIncome, categoryRow, itemRow, registerWithPlan, tile } from './support';

function editButton(page: Page, description: string) {
  return page.getByRole('button', { name: `Eintrag ${description} bearbeiten` });
}

// docs/specs/posten-bearbeiten.md
test.describe('Posten bearbeiten', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('den Betrag einer Ausgabe ändern, der Posten bleibt an seiner Stelle (PB-1, PB-4, PB-7)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    await addExpense(page, 'Kino', '20', 'Freizeit & Spaß');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('850,00 €');

    await editButton(page, 'Miete').click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Ausgabe bearbeiten');
    await expect(dialog.getByLabel('Beschreibung')).toHaveValue('Miete');
    await expect(dialog.getByLabel('Betrag')).toHaveValue('850');
    await expect(dialog.getByRole('combobox', { name: 'Kategorie' })).toContainText(
      'Lebensunterhalt',
    );
    await dialog.getByLabel('Betrag').fill('900');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemRow(page, 'Miete')).toContainText('Lebensunterhalt');
    await expect(itemRow(page, 'Miete')).toContainText('−900,00 €');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('900,00 €');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('30 % von max. 50 %');
    await expect(tile(page, 'Ausgaben')).toHaveText('920,00 €');

    // Saved on the server, and still in its original place (PB-4)
    await page.reload();
    const rows = page.locator('app-budget-item-table tr[mat-row]');
    await expect(rows).toHaveCount(3);
    await expect(rows.nth(0)).toContainText('Kino');
    await expect(rows.nth(1)).toContainText('Miete');
    await expect(rows.nth(1)).toContainText('−900,00 €');
    await expect(rows.nth(2)).toContainText('Gehalt');
  });

  test('die Kategorie einer Ausgabe ändern verschiebt ihren Betrag (PB-1, PB-7)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000');
    // Uses up the income, so no surplus counts towards savings (AW-10)
    await addExpense(page, 'Miete', '1600', 'Lebensunterhalt');
    await addExpense(page, 'Fondssparen', '400', 'Freizeit & Spaß');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('20 % von max. 30 %');

    await editButton(page, 'Fondssparen').click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('combobox', { name: 'Kategorie' }).click();
    await page.getByRole('option', { name: 'Sparen & Investieren' }).click();
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemRow(page, 'Fondssparen')).toContainText('Sparen & Investieren');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('0 % von max. 30 %');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText('400,00 €');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText('20 % von mind. 20 %');
  });

  test('bei einer Einnahme lassen sich Beschreibung und Betrag ändern, aber weder Art noch Kategorie (PB-1, PB-2)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000');

    await editButton(page, 'Gehalt').click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Einnahme bearbeiten');
    await expect(dialog.getByRole('combobox', { name: 'Kategorie' })).toHaveCount(0);
    await expect(dialog.getByRole('radio')).toHaveCount(0);
    await dialog.getByLabel('Beschreibung').fill('Nettogehalt');
    await dialog.getByLabel('Betrag').fill('3200.50');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemRow(page, 'Nettogehalt')).toContainText('Einnahme');
    await expect(itemRow(page, 'Nettogehalt')).toContainText('+3.200,50 €');
    await expect(tile(page, 'Einnahmen')).toHaveText('3.200,50 €');
  });

  test('ungültige Werte zeigen dieselben Meldungen wie beim Erfassen, ohne die Anfrage zu senden (PB-3)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    const requests: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'PUT') requests.push(request.url());
    });

    await editButton(page, 'Miete').click();
    const dialog = page.getByRole('dialog');
    const save = dialog.getByRole('button', { name: 'Speichern' });

    for (const amount of ['0', '-5']) {
      await dialog.getByLabel('Betrag').fill(amount);
      await save.click();
      await expect(dialog.getByText('Bitte einen positiven Betrag angeben.')).toBeVisible();
    }

    await dialog.getByLabel('Betrag').fill('10.005');
    await save.click();
    await expect(dialog.getByText('Höchstens zwei Nachkommastellen.')).toBeVisible();

    await dialog.getByLabel('Betrag').fill('900');
    await dialog.getByLabel('Beschreibung').fill('   ');
    await save.click();
    await expect(dialog.getByText('Bitte eine Beschreibung angeben.')).toBeVisible();

    expect(requests).toEqual([]);
    await dialog.getByRole('button', { name: 'Abbrechen' }).click();
    await expect(itemRow(page, 'Miete')).toContainText('−850,00 €');
  });

  test('Abbrechen lässt den Posten unverändert (PB-1)', async ({ page }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');

    await editButton(page, 'Miete').click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Beschreibung').fill('Warmmiete');
    await dialog.getByLabel('Betrag').fill('999');
    await dialog.getByRole('button', { name: 'Abbrechen' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemRow(page, 'Warmmiete')).toHaveCount(0);
    await expect(itemRow(page, 'Miete')).toContainText('−850,00 €');
    await page.reload();
    await expect(itemRow(page, 'Miete')).toContainText('−850,00 €');
  });

  test('eine Änderung lässt sich durch erneutes Ändern zurücknehmen (PB-8)', async ({ page }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    const dialog = page.getByRole('dialog');

    await editButton(page, 'Miete').click();
    await dialog.getByLabel('Betrag').fill('900');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();
    // No undo action, unlike deleting (PO-9)
    await expect(page.getByRole('button', { name: 'Rückgängig' })).toHaveCount(0);
    await expect(itemRow(page, 'Miete')).toContainText('−900,00 €');

    await editButton(page, 'Miete').click();
    await dialog.getByLabel('Betrag').fill('850');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();
    await expect(itemRow(page, 'Miete')).toContainText('−850,00 €');
  });
});
