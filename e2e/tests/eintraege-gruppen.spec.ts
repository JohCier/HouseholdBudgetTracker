import { expect, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  itemDescriptions,
  itemRow,
  openEditDialog,
  registerWithPlan,
  sumRow,
} from './support';

// docs/specs/eintraege-gruppen.md
test.describe('Einträge in Gruppen', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('ein neuer Posten steht unten in seiner Gruppe, auch nach dem Umsortieren (EG-1)', async ({
    page,
  }) => {
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Steuer', '150', 'Lebensunterhalt', 'Auto');
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Wartung',
      'Steuer',
    ]);
    // Rearranged with the keyboard: Steuer on top
    await page.getByRole('button', { name: 'Eintrag Steuer verschieben' }).focus();
    const saved = page.waitForResponse((response) => response.url().endsWith('/item-order'));
    await page.keyboard.press('ArrowUp');
    await saved;

    await addExpense(page, 'Sprit', '1200', 'Lebensunterhalt', 'Auto');
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Steuer',
      'Wartung',
      'Sprit',
    ]);
  });

  test('ein Posten mit geänderter Unterkategorie steht unten in seiner neuen Gruppe (EG-2)', async ({
    page,
  }) => {
    await addExpense(page, 'Tank', '80', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Kaltmiete', '800', 'Lebensunterhalt', 'Miete');
    await addExpense(page, 'Nebenkosten', '200', 'Lebensunterhalt', 'Miete');

    const dialog = await openEditDialog(page, 'Tank');
    await dialog.getByRole('combobox', { name: 'Unterkategorie (optional)' }).fill('Miete');
    await dialog.getByLabel('Betrag').click();
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();

    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Miete')).toHaveText([
      'Kaltmiete',
      'Nebenkosten',
      'Tank',
    ]);
  });

  test('ein wiederhergestellter Posten steht unten in seiner Gruppe (EG-3)', async ({ page }) => {
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Steuer', '150', 'Lebensunterhalt', 'Auto');

    await page.getByRole('button', { name: 'Eintrag Wartung löschen' }).click();
    await expect(itemRow(page, 'Wartung')).toBeHidden();
    await page.getByRole('button', { name: 'Rückgängig' }).click();

    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Steuer',
      'Wartung',
    ]);
  });

  test('die Summen stehen in Zeilen unter Gruppe, Kategorie und Bereich (EG-5, EG-6)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '36000', 'Gehalt');
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Steuer', '150', 'Lebensunterhalt', 'Auto');

    const autoSum = sumRow(page, 'Lebensunterhalt', 'Auto');
    await expect(autoSum.locator('.summary-label')).toHaveText('Summe Auto');
    await expect(autoSum.locator('.total.monthly')).toHaveText('45,83 €');
    await expect(autoSum.locator('.total.yearly')).toHaveText('550,00 €');
    await expect(sumRow(page, 'Lebensunterhalt')).toContainText('550,00 €');
    await expect(sumRow(page, 'Ausgaben')).toContainText('−550,00 €');
    await expect(sumRow(page, 'Einnahmen')).toContainText('+36.000,00 €');
    // The summary comes after the last item of the group, and the header shows only the name
    const auto = page.locator('app-budget-entries [role="group"][aria-label="Auto"]');
    await expect(auto.locator('.item, .group-summary').last()).toHaveClass(/group-summary/);
    await expect(auto.locator('.group-header')).toHaveText('Auto');
  });

  test('einen Posten direkt in einer Gruppe erfassen, Art, Kategorie und Unterkategorie sind ausgefüllt (EG-9, EG-10, EG-11)', async ({
    page,
  }) => {
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');

    await page.getByRole('button', { name: 'Posten in Auto hinzufügen' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Neuer Eintrag in Auto');
    await expect(dialog.getByLabel('Beschreibung')).toBeFocused();
    await expect(dialog.getByRole('radio', { name: 'Ausgabe' })).toBeChecked();
    await expect(
      dialog.getByRole('combobox', { name: 'Kategorie', exact: true }),
    ).toContainText('Lebensunterhalt');
    await expect(dialog.getByRole('combobox', { name: 'Unterkategorie (optional)' })).toHaveValue(
      'Auto',
    );

    await dialog.getByLabel('Beschreibung').fill('Sprit');
    await dialog.getByLabel('Betrag').fill('100');
    await dialog.getByRole('button', { name: 'Hinzufügen', exact: true }).click();
    await expect(dialog).toBeHidden();

    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Wartung',
      'Sprit',
    ]);
    await expect(itemRow(page, 'Sprit')).toContainText('−100,00 €');
  });

  test('in „Sonstiges" der Einnahmen bleibt die Unterkategorie leer, Abbrechen legt nichts an (EG-10, EG-11)', async ({
    page,
  }) => {
    await addIncome(page, 'Bonus', '500');

    await page.getByRole('button', { name: 'Posten in Sonstiges hinzufügen' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByLabel('Beschreibung')).toBeFocused();
    await expect(dialog.getByRole('radio', { name: 'Einnahme' })).toBeChecked();
    await expect(dialog.getByRole('combobox', { name: 'Unterkategorie (optional)' })).toHaveValue(
      '',
    );
    await dialog.getByLabel('Beschreibung').fill('Zinsen');
    await dialog.getByRole('button', { name: 'Abbrechen' }).click();

    await expect(dialog).toBeHidden();
    await expect(itemDescriptions(page, 'Einnahmen', 'Sonstiges')).toHaveText(['Bonus']);
  });
});
