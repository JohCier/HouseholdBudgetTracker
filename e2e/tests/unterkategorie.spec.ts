import { expect, Locator, Page, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  categoryRow,
  entrySection,
  groupNames,
  itemDescriptions,
  itemGroup,
  itemRow,
  openEditDialog,
  pieLegendItem,
  registerWithPlan,
  showBreakdownAs,
} from './support';

/** Total shown in the header of a subcategory group. */
function groupTotal(page: Page, section: Parameters<typeof itemGroup>[1], subcategory: string) {
  return itemGroup(page, section, subcategory).locator('.group-header .total');
}

/**
 * Drags an entry by its handle onto the place of another one, in small steps, as the drag and
 * drop of the page only starts after the pointer has moved a bit (UK-14). Waits until the new
 * order has been saved.
 */
async function drag(page: Page, handle: Locator, target: Locator): Promise<void> {
  const from = (await handle.boundingBox())!;
  const to = (await target.boundingBox())!;
  const saved = page.waitForResponse(
    (response) => response.request().method() === 'PUT' && /-order$/.test(response.url()),
  );
  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
  await page.mouse.down();
  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2 + 10, { steps: 5 });
  await page.mouse.move(from.x + from.width / 2, to.y + 4, { steps: 20 });
  await page.mouse.up();
  expect((await saved).ok()).toBe(true);
}

// docs/specs/unterkategorie.md
test.describe('Unterkategorie', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('ein Posten mit Unterkategorie steht in ihrer Gruppe mit deren Summe (UK-1, UK-10, UK-11, UK-12)', async ({
    page,
  }) => {
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');

    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText(['Wartung']);
    await expect(groupTotal(page, 'Lebensunterhalt', 'Auto')).toHaveText('400,00 €');
  });

  test('Gruppen, Kategorien und Ausgaben zeigen ihre Summen, die Gruppen alphabetisch (UK-12, UK-16)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '9600', 'Lebensunterhalt', 'Miete');
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Steuer', '150', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Sprit', '1200', 'Lebensunterhalt', 'Auto');

    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Auto', 'Miete']);
    await expect(groupTotal(page, 'Lebensunterhalt', 'Auto')).toHaveText('1.750,00 €');
    await expect(groupTotal(page, 'Lebensunterhalt', 'Miete')).toHaveText('9.600,00 €');
    await expect(entrySection(page, 'Lebensunterhalt').locator('.section-header')).toContainText(
      '11.350,00 €',
    );
    await expect(
      page.locator('app-budget-entries section[aria-label="Ausgaben"] .area-header'),
    ).toContainText('−11.350,00 €');
  });

  test('Groß- und Kleinschreibung unterscheidet keine Unterkategorien (UK-5)', async ({ page }) => {
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Steuer', '150', 'Lebensunterhalt', 'auto');

    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Auto']);
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Steuer',
      'Wartung',
    ]);
  });

  test('dieselbe Unterkategorie in einer anderen Kategorie ist eine eigene Gruppe (UK-3)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000');
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Ausflug', '200', 'Freizeit & Spaß', 'Auto');

    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText(['Wartung']);
    await expect(itemDescriptions(page, 'Freizeit & Spaß', 'Auto')).toHaveText(['Ausflug']);
    await expect(categoryRow(page, 'Lebensunterhalt')).toContainText('20 % von max. 50 %');
    await expect(categoryRow(page, 'Freizeit & Spaß')).toContainText('10 % von max. 30 %');
  });

  test('Einnahmen stehen über den Ausgaben, die Kategorien in fester Reihenfolge und Farbe (UK-9, UK-10, UK-19)', async ({
    page,
  }) => {
    await addExpense(page, 'ETF', '500', 'Sparen & Investieren');
    await addExpense(page, 'Kino', '20', 'Freizeit & Spaß');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt');
    await addIncome(page, 'Gehalt', '3000', 'Gehalt');

    const sections = page.locator('app-budget-entries section[aria-label]');
    expect(await sections.evaluateAll((all) => all.map((s) => s.getAttribute('aria-label')))).toEqual([
      'Einnahmen',
      'Ausgaben',
      'Lebensunterhalt',
      'Freizeit & Spaß',
      'Sparen & Investieren',
    ]);

    const colors = {
      'Lebensunterhalt': 'rgb(160, 122, 90)',
      'Freizeit & Spaß': 'rgb(224, 160, 32)',
      'Sparen & Investieren': 'rgb(42, 120, 214)',
    } as const;
    await expect(entrySection(page, 'Einnahmen').locator('.section')).toHaveCSS(
      'border-left-color',
      'rgb(27, 109, 54)',
    );
    for (const [category, color] of Object.entries(colors)) {
      await expect(entrySection(page, category as keyof typeof colors)).toHaveCSS(
        'border-left-color',
        color,
      );
    }

    await showBreakdownAs(page, 'Torte');
    for (const [category, color] of Object.entries(colors)) {
      await expect(
        pieLegendItem(page, category as keyof typeof colors).locator('.swatch'),
      ).toHaveCSS('background-color', color);
    }
  });

  test('Posten ohne Unterkategorie und mit „sonstiges" stehen in „Sonstiges" am Ende (UK-1, UK-8, UK-16)', async ({
    page,
  }) => {
    await addExpense(page, 'Strom', '60', 'Lebensunterhalt');
    await addExpense(page, 'Gebühren', '10', 'Lebensunterhalt', 'sonstiges');
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Zuhause');

    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Zuhause', 'Sonstiges']);
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Sonstiges')).toHaveText([
      'Gebühren',
      'Strom',
    ]);
  });

  test('das Feld schlägt Unterkategorien vor und übernimmt eigene Namen (UK-6, UK-7)', async ({
    page,
  }) => {
    await page.getByRole('radio', { name: 'Ausgabe' }).click();
    await page.getByRole('combobox', { name: 'Unterkategorie (optional)' }).fill('Au');
    await expect(page.getByRole('option', { name: 'Auto', exact: true })).toBeVisible();
    await page.getByRole('combobox', { name: 'Unterkategorie (optional)' }).fill('');
    await page.keyboard.press('Tab');

    await addExpense(page, 'Liegeplatz', '300', 'Freizeit & Spaß', 'Boot');
    await expect(itemDescriptions(page, 'Freizeit & Spaß', 'Boot')).toHaveText(['Liegeplatz']);

    await page.getByRole('combobox', { name: 'Unterkategorie (optional)' }).fill('bo');
    await expect(page.getByRole('option', { name: 'Boot', exact: true })).toBeVisible();
  });

  test('die Unterkategorie ändern und entfernen (UK-1, UK-4)', async ({ page }) => {
    await addExpense(page, 'Tank', '80', 'Lebensunterhalt', 'Auto');

    let dialog = await openEditDialog(page, 'Tank');
    await expect(dialog.getByRole('combobox', { name: 'Unterkategorie (optional)' })).toHaveValue('Auto');
    await dialog.getByRole('combobox', { name: 'Unterkategorie (optional)' }).fill('Mobilität');
    await dialog.getByLabel('Betrag').click();
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();
    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Mobilität']);

    dialog = await openEditDialog(page, 'Tank');
    await dialog.getByRole('combobox', { name: 'Unterkategorie (optional)' }).fill('');
    await dialog.getByLabel('Betrag').click();
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();
    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Sonstiges']);
  });

  test('ein wiederhergestellter Posten steht wieder in seiner Gruppe (UK-4)', async ({ page }) => {
    await addExpense(page, 'Tank', '80', 'Lebensunterhalt', 'Auto');

    await page.getByRole('button', { name: 'Eintrag Tank löschen' }).click();
    await expect(itemRow(page, 'Tank')).toBeHidden();
    await page.getByRole('button', { name: 'Rückgängig' }).click();

    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText(['Tank']);
  });

  test('eine Unterkategorie mit mehr als 50 Zeichen wird abgelehnt, ohne die Anfrage zu senden (UK-2)', async ({
    page,
  }) => {
    const requests: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST') requests.push(request.url());
    });
    await page.getByRole('radio', { name: 'Einnahme' }).click();
    await page.getByLabel('Beschreibung').fill('Gehalt');
    await page.getByLabel('Betrag').fill('3000');
    await page.getByRole('combobox', { name: 'Unterkategorie (optional)' }).fill('x'.repeat(51));
    await page.keyboard.press('Tab');
    await page.getByRole('button', { name: 'Hinzufügen' }).click();

    await expect(page.getByText('Höchstens 50 Zeichen.')).toBeVisible();
    expect(requests).toEqual([]);
  });

  test('Gruppen frei sortieren, die Reihenfolge bleibt nach dem Neuladen (UK-14, UK-15)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '9600', 'Lebensunterhalt', 'Miete');
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Auto', 'Miete']);

    await drag(
      page,
      page.getByRole('button', { name: 'Unterkategorie Miete verschieben' }),
      itemGroup(page, 'Lebensunterhalt', 'Auto'),
    );
    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Miete', 'Auto']);

    await page.reload();
    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Miete', 'Auto']);
  });

  test('Posten in ihrer Gruppe frei sortieren, die Reihenfolge bleibt nach dem Neuladen (UK-14, UK-15)', async ({
    page,
  }) => {
    await addExpense(page, 'Sprit', '1200', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Steuer', '150', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Wartung',
      'Steuer',
      'Sprit',
    ]);

    await drag(
      page,
      page.getByRole('button', { name: 'Eintrag Sprit verschieben' }),
      itemRow(page, 'Wartung'),
    );
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Sprit',
      'Wartung',
      'Steuer',
    ]);

    await page.reload();
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Sprit',
      'Wartung',
      'Steuer',
    ]);
  });

  test('neue Posten stehen oben in ihrer Gruppe, neue Gruppen vor „Sonstiges" (UK-17)', async ({
    page,
  }) => {
    await addExpense(page, 'Steuer', '150', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Wartung', '400', 'Lebensunterhalt', 'Auto');
    await addExpense(page, 'Strom', '60', 'Lebensunterhalt');
    // Rearranged with the keyboard: Steuer on top
    await page.getByRole('button', { name: 'Eintrag Steuer verschieben' }).focus();
    const saved = page.waitForResponse((response) => response.url().endsWith('/item-order'));
    await page.keyboard.press('ArrowUp');
    await saved;
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Steuer',
      'Wartung',
    ]);

    await addExpense(page, 'Sprit', '1200', 'Lebensunterhalt', 'Auto');
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Auto')).toHaveText([
      'Sprit',
      'Steuer',
      'Wartung',
    ]);

    await addExpense(page, 'Ferienwohnung', '700', 'Lebensunterhalt', 'Urlaub');
    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Auto', 'Urlaub', 'Sonstiges']);
  });

  test('ein Posten mit geänderter Unterkategorie steht oben in seiner neuen Gruppe (UK-17)', async ({
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

    await expect(groupNames(page, 'Lebensunterhalt')).toHaveText(['Miete']);
    await expect(itemDescriptions(page, 'Lebensunterhalt', 'Miete')).toHaveText([
      'Tank',
      'Nebenkosten',
      'Kaltmiete',
    ]);
  });

  test('die Reihenfolge gehört zum Plan eines Jahres (UK-15)', async ({ page }) => {
    const year = new Date().getFullYear();
    await addIncome(page, 'Gehalt', '3000', 'Gehalt');
    await addIncome(page, 'Miete', '500', 'Mieteinnahmen');
    await page.getByRole('button', { name: 'Unterkategorie Gehalt verschieben' }).focus();
    const saved = page.waitForResponse((response) => response.url().endsWith('/subcategory-order'));
    await page.keyboard.press('ArrowDown');
    await saved;
    await expect(groupNames(page, 'Einnahmen')).toHaveText(['Mieteinnahmen', 'Gehalt']);

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await expect(page.getByRole('heading', { name: String(year + 1) })).toBeVisible();
    await page.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();
    await addIncome(page, 'Gehalt', '3000', 'Gehalt');
    await addIncome(page, 'Miete', '500', 'Mieteinnahmen');
    await expect(groupNames(page, 'Einnahmen')).toHaveText(['Gehalt', 'Mieteinnahmen']);

    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();
    await expect(groupNames(page, 'Einnahmen')).toHaveText(['Mieteinnahmen', 'Gehalt']);
  });
});
