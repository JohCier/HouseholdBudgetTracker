import { expect, Page, test } from '@playwright/test';
import { addExpense, addIncome, balanceSheet, itemRow, openEditDialog, registerWithPlan } from './support';

/** Marks an item as important or no longer with the star on its row (PM-4a). */
async function toggleImportant(page: Page, description: string, important: boolean) {
  const name = important
    ? `${description} als wichtig markieren`
    : `Markierung wichtig bei ${description} entfernen`;
  await page.getByRole('button', { name, exact: true }).click();
  if (important) {
    await expect(itemRow(page, description)).toHaveClass(/important/);
  } else {
    await expect(itemRow(page, description)).not.toHaveClass(/important/);
  }
}

/** Opens the dialog of the saving potential of an expense (PM-4b). */
async function openSavingPotential(page: Page, description: string) {
  await page.getByRole('button', { name: `Sparpotential bei ${description}`, exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading')).toHaveText(`Sparpotential bei ${description}`);
  return dialog;
}

async function setSavingPotential(page: Page, description: string, monthly?: string) {
  const dialog = await openSavingPotential(page, description);
  if (monthly !== undefined) {
    await dialog.getByLabel('Betrag pro Monat').fill(monthly);
  }
  await dialog.getByRole('button', { name: 'Speichern' }).click();
  await expect(dialog).toBeHidden();
}

const savingPotentialOf = (page: Page, description: string) =>
  itemRow(page, description).locator('.saving-potential');

// docs/specs/posten-markierungen.md
test.describe('Posten markieren: wichtig und Sparpotential', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('ein Posten wird mit dem Stern als wichtig hervorgehoben und wieder zurückgesetzt (PM-1, PM-4, PM-9)', async ({
    page,
  }) => {
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', undefined, 'Monatlich');
    await expect(itemRow(page, 'Miete')).not.toHaveClass(/important/);

    await toggleImportant(page, 'Miete', true);
    await page.reload();
    await expect(itemRow(page, 'Miete')).toHaveClass(/important/);
    await expect(
      page.getByRole('button', { name: 'Markierung wichtig bei Miete entfernen' }),
    ).toHaveAttribute('aria-pressed', 'true');

    await toggleImportant(page, 'Miete', false);
    await page.reload();
    await expect(itemRow(page, 'Miete')).not.toHaveClass(/important/);
  });

  test('Formular und Dialoge haben keine Felder für die Markierungen (PM-4)', async ({ page }) => {
    const form = page.locator('app-add-item-form');
    await expect(form.getByText(/wichtig|Sparpotential/i)).toHaveCount(0);

    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', 'Wohnen');
    const edit = await openEditDialog(page, 'Miete');
    await expect(edit.getByText(/wichtig|Sparpotential/i)).toHaveCount(0);
    await edit.getByRole('button', { name: 'Abbrechen' }).click();

    await page.getByRole('button', { name: 'Posten in Wohnen hinzufügen' }).click();
    const add = page.getByRole('dialog');
    await expect(add.getByLabel('Beschreibung')).toBeFocused();
    await expect(add.getByText(/wichtig|Sparpotential/i)).toHaveCount(0);
  });

  test('der Dialog schlägt den Monatsbetrag der Ausgabe vor, auch bei jährlicher Angabe (PM-2a, PM-4b)', async ({
    page,
  }) => {
    await addExpense(page, 'Fitnessstudio', '360', 'Freizeit & Spaß');

    const dialog = await openSavingPotential(page, 'Fitnessstudio');

    await expect(dialog.getByLabel('Betrag pro Monat')).toHaveValue('30');
    await expect(dialog.getByRole('button', { name: 'Entfernen' })).toHaveCount(0);
  });

  test('die Bilanz summiert das Sparpotential je Monat und Jahr, Saldo bleibt gleich (PM-2, PM-8, PM-10, PM-13, PM-14)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', undefined, 'Monatlich');
    await addExpense(page, 'Fitnessstudio', '360', 'Freizeit & Spaß');
    await expect(balanceSheet(page, 'Sparpotential', 'Monat')).toHaveText('0,00 €');

    await setSavingPotential(page, 'Streaming', '5');
    await setSavingPotential(page, 'Fitnessstudio');

    await expect(savingPotentialOf(page, 'Streaming')).toHaveText('Sparpotential: 5,00 € im Monat');
    await expect(savingPotentialOf(page, 'Fitnessstudio')).toHaveText(
      'Sparpotential: 30,00 € im Monat',
    );
    await expect(balanceSheet(page, 'Sparpotential', 'Monat')).toHaveText('35,00 €');
    await expect(balanceSheet(page, 'Sparpotential', 'Jahr')).toHaveText('420,00 €');
    await expect(balanceSheet(page, 'Saldo', 'Monat')).toHaveText('2.955,00 €');
  });

  test('„Entfernen" löscht das Sparpotential einer Ausgabe (PM-4b, PM-13)', async ({ page }) => {
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', undefined, 'Monatlich');
    await addExpense(page, 'Fitnessstudio', '360', 'Freizeit & Spaß');
    await setSavingPotential(page, 'Streaming', '5');
    await setSavingPotential(page, 'Fitnessstudio');

    const dialog = await openSavingPotential(page, 'Streaming');
    await expect(dialog.getByLabel('Betrag pro Monat')).toHaveValue('5');
    await dialog.getByRole('button', { name: 'Entfernen' }).click();
    await expect(dialog).toBeHidden();

    await expect(savingPotentialOf(page, 'Streaming')).toHaveCount(0);
    await expect(balanceSheet(page, 'Sparpotential', 'Monat')).toHaveText('30,00 €');
    await expect(balanceSheet(page, 'Sparpotential', 'Jahr')).toHaveText('360,00 €');
  });

  test('mehr als den Monatsbetrag lehnt der Dialog ab (PM-2a)', async ({ page }) => {
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', undefined, 'Monatlich');
    const requests: string[] = [];
    page.on('request', (request) => {
      if (request.url().endsWith('/saving-potential')) requests.push(request.url());
    });

    const dialog = await openSavingPotential(page, 'Streaming');
    await dialog.getByLabel('Betrag pro Monat').fill('20');
    await dialog.getByRole('button', { name: 'Speichern' }).click();

    await expect(dialog.getByText('Höchstens 15,00 €.')).toBeVisible();
    expect(requests).toEqual([]);
  });

  test('Einnahmen haben nur den Stern, kein Sparpotential (PM-2, PM-4)', async ({ page }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');

    await expect(page.getByRole('button', { name: 'Gehalt als wichtig markieren' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sparpotential bei Gehalt' })).toHaveCount(0);
  });

  test('beim Bearbeiten bleibt das Sparpotential, sinkt aber mit dem Betrag (PM-5)', async ({
    page,
  }) => {
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', undefined, 'Monatlich');
    await toggleImportant(page, 'Streaming', true);
    await setSavingPotential(page, 'Streaming', '5');

    let dialog = await openEditDialog(page, 'Streaming');
    await dialog.getByLabel('Betrag').fill('20');
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();
    await expect(savingPotentialOf(page, 'Streaming')).toHaveText('Sparpotential: 5,00 € im Monat');
    await expect(itemRow(page, 'Streaming')).toHaveClass(/important/);

    dialog = await openEditDialog(page, 'Streaming');
    await dialog.getByLabel('Betrag').fill('36');
    await dialog
      .getByRole('radiogroup', { name: 'Angabe monatlich oder jährlich' })
      .getByRole('radio', { name: 'Jährlich' })
      .click();
    await dialog.getByRole('button', { name: 'Speichern' }).click();
    await expect(dialog).toBeHidden();
    await expect(savingPotentialOf(page, 'Streaming')).toHaveText('Sparpotential: 3,00 € im Monat');
    await expect(balanceSheet(page, 'Sparpotential', 'Monat')).toHaveText('3,00 €');
  });

  test('Rückgängig nach dem Löschen stellt auch die Markierungen wieder her (PM-6)', async ({
    page,
  }) => {
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', undefined, 'Monatlich');
    await toggleImportant(page, 'Streaming', true);
    await setSavingPotential(page, 'Streaming', '5');

    await page.getByRole('button', { name: 'Eintrag Streaming löschen' }).click();
    await expect(itemRow(page, 'Streaming')).toBeHidden();
    await page.getByRole('button', { name: 'Rückgängig' }).click();

    await expect(itemRow(page, 'Streaming')).toHaveClass(/important/);
    await expect(savingPotentialOf(page, 'Streaming')).toHaveText('Sparpotential: 5,00 € im Monat');
    await expect(balanceSheet(page, 'Sparpotential', 'Monat')).toHaveText('5,00 €');
  });

  test('beim Übernehmen als Vorlage gehen die Markierungen mit (PM-6)', async ({ page }) => {
    const year = new Date().getFullYear();
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', undefined, 'Monatlich');
    await toggleImportant(page, 'Gehalt', true);
    await setSavingPotential(page, 'Streaming', '5');

    await page.getByRole('button', { name: 'Als Vorlage übernehmen' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByText(/Posten werden übernommen\./)).toBeVisible();
    await dialog.getByRole('button', { name: 'Übernehmen' }).click();

    await expect(page).toHaveURL(`/jahre/${year + 1}`);
    await expect(itemRow(page, 'Gehalt')).toHaveClass(/important/);
    await expect(savingPotentialOf(page, 'Streaming')).toHaveText('Sparpotential: 5,00 € im Monat');
    await expect(balanceSheet(page, 'Sparpotential', 'Jahr')).toHaveText('60,00 €');
  });

  test('auf dem Handy sind beide Knöpfe zu sehen, neben den Beträgen, die Seite ist nicht breiter als der Bildschirm (PM-12)', async ({
    page,
  }) => {
    await addExpense(
      page,
      'Grundsteuer und Gebäudeversicherung',
      '123456.78',
      'Lebensunterhalt',
      'Haus',
    );

    for (const width of [360, 390]) {
      await page.setViewportSize({ width, height: 844 });
      const row = itemRow(page, 'Grundsteuer und Gebäudeversicherung');
      await row.scrollIntoViewIfNeeded();
      const star = row.getByRole('button', { name: /als wichtig markieren/ });
      const piggy = row.getByRole('button', { name: /^Sparpotential bei/ });
      await expect(star).toBeInViewport();
      await expect(piggy).toBeInViewport();
      const starBox = (await star.boundingBox())!;
      const piggyBox = (await piggy.boundingBox())!;
      // Where the right-aligned text of the monthly amount starts, not its wider column
      const monthlyTextX = await row.locator('.amount.monthly').evaluate((amount) => {
        const range = document.createRange();
        range.selectNodeContents(amount);
        return range.getBoundingClientRect().x;
      });
      expect(starBox.x).toBeGreaterThanOrEqual(0);
      expect(piggyBox.x + piggyBox.width).toBeLessThanOrEqual(monthlyTextX);
      await expect
        .poll(() =>
          page.evaluate(
            () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
          ),
        )
        .toBe(false);
    }
  });
});
