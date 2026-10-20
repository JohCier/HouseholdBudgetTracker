import { expect, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  categoryRow,
  itemIn,
  itemDescriptions,
  itemRow,
  openEditDialog,
  registerWithPlan,
  tile,
} from './support';

// docs/specs/posten-bearbeiten.md
test.describe('Posten bearbeiten', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('den Betrag einer Ausgabe ändern, der Posten bleibt an seiner Stelle (PB-1, PB-4, PB-7)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000');
    await addExpense(page, 'Wasser', '20', 'Lebensunterhalt');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    await addExpense(page, 'Strom', '40', 'Lebensunterhalt');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('910,00 €');

    const dialog = await openEditDialog(page, 'Miete');
    await expect(dialog).toContainText('Ausgabe bearbeiten');
    await expect(dialog.getByLabel('Beschreibung')).toHaveValue('Miete');
    await expect(dialog.getByLabel('Betrag')).toHaveValue('850');
    await expect(dialog.getByRole('combobox', { name: 'Kategorie', exact: true })).toContainText(
      'Lebensunterhalt',
    );
    await dialog.getByLabel('Betrag').fill('900');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemIn(page, 'Lebensunterhalt', 'Miete')).toBeVisible();
    await expect(itemRow(page, 'Miete')).toContainText('−900,00 €');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('960,00 €');
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('32 % von max. 50 %');
    await expect(tile(page, 'Ausgaben')).toHaveText('960,00 €');

    // Saved on the server, and still in its original place (PB-4)
    await page.reload();
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Sonstiges')).toHaveText([
      'Strom',
      'Miete',
      'Wasser',
    ]);
    await expect(itemRow(page, 'Miete')).toContainText('−900,00 €');
  });

  test('die Kategorie einer Ausgabe ändern verschiebt ihren Betrag (PB-1, PB-7)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000');
    // Uses up the income, so no surplus counts towards savings (AW-10)
    await addExpense(page, 'Miete', '1600', 'Lebensunterhalt');
    await addExpense(page, 'Fondssparen', '400', 'Freizeit & Spaß');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('20 % von max. 30 %');

    const dialog = await openEditDialog(page, 'Fondssparen');
    await dialog.getByRole('combobox', { name: 'Kategorie', exact: true }).click();
    await page.getByRole('option', { name: 'Sparen & Investieren' }).click();
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemIn(page, 'Sparen & Investieren', 'Fondssparen')).toBeVisible();
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('0 % von max. 30 %');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText('400,00 €');
    await expect(categoryRow(page, 'Sparen & Investieren')).toContainText('20 % von mind. 20 %');
  });

  test('bei einer Einnahme lassen sich Beschreibung und Betrag ändern, aber weder Art noch Kategorie (PB-1, PB-2)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000');

    const dialog = await openEditDialog(page, 'Gehalt');
    await expect(dialog).toContainText('Einnahme bearbeiten');
    await expect(dialog.getByRole('combobox', { name: 'Kategorie', exact: true })).toHaveCount(0);
    await expect(dialog.getByRole('radio')).toHaveCount(0);
    await dialog.getByLabel('Beschreibung').fill('Nettogehalt');
    await dialog.getByLabel('Betrag').fill('3200.50');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemIn(page, 'Einnahmen', 'Nettogehalt')).toBeVisible();
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

    const dialog = await openEditDialog(page, 'Miete');
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

    const dialog = await openEditDialog(page, 'Miete');
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

    await openEditDialog(page, 'Miete');
    await dialog.getByLabel('Betrag').fill('900');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();
    // No undo action, unlike deleting (PO-9)
    await expect(page.getByRole('button', { name: 'Rückgängig' })).toHaveCount(0);
    await expect(itemRow(page, 'Miete')).toContainText('−900,00 €');

    await openEditDialog(page, 'Miete');
    await dialog.getByLabel('Betrag').fill('850');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();
    await expect(itemRow(page, 'Miete')).toContainText('−850,00 €');
  });
});
