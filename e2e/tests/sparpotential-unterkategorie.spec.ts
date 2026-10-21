import { expect, Locator, Page, test } from '@playwright/test';
import {
  addExpense,
  balanceSheet,
  openSavingPotential,
  registerWithPlan,
  savingPotentialOfSubcategory,
  setSavingPotential,
} from './support';

async function box(locator: Locator) {
  const result = await locator.boundingBox();
  expect(result).not.toBeNull();
  return result!;
}

/** Whether the page is wider than the screen, so it scrolls sideways (MA-1). */
async function scrollsSideways(page: Page): Promise<boolean> {
  return page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  );
}

const savingPotentials = (page: Page) => page.locator('app-saving-potential-summary');

// docs/specs/sparpotential-unterkategorie.md
test.describe('Sparpotential je Unterkategorie', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('die Liste summiert das Sparpotential je Unterkategorie, das größte oben (SU-1, SU-2, SU-4, SU-6)', async ({
    page,
  }) => {
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', 'Abonnements', 'Monatlich');
    await addExpense(page, 'Zeitung', '20', 'Freizeit & Spaß', 'abonnements', 'Monatlich');
    await addExpense(page, 'Fitnessstudio', '360', 'Freizeit & Spaß', 'Hobbys');
    await setSavingPotential(page, 'Streaming', '5');
    await setSavingPotential(page, 'Zeitung', '10');
    await setSavingPotential(page, 'Fitnessstudio');

    const list = page.getByRole('table', { name: 'Sparpotential je Unterkategorie' });
    await expect(list.getByRole('columnheader')).toHaveText(['Monat', 'Jahr']);
    await expect(list.getByRole('rowheader')).toHaveText(['Hobbys', 'Abonnements']);
    await expect(savingPotentialOfSubcategory(page, 'Hobbys', 'Monat')).toHaveText('30,00 €');
    await expect(savingPotentialOfSubcategory(page, 'Hobbys', 'Jahr')).toHaveText('360,00 €');
    await expect(savingPotentialOfSubcategory(page, 'Abonnements', 'Monat')).toHaveText('15,00 €');
    await expect(savingPotentialOfSubcategory(page, 'Abonnements', 'Jahr')).toHaveText('180,00 €');
    await expect(balanceSheet(page, 'Sparpotential', 'Monat')).toHaveText('45,00 €');
  });

  test('dieselbe Unterkategorie in zwei Kategorien ergibt eine Zeile (SU-2)', async ({ page }) => {
    await addExpense(page, 'Tanken', '100', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'Ausflüge', '50', 'Freizeit & Spaß', 'Auto', 'Monatlich');
    await setSavingPotential(page, 'Tanken', '10');
    await setSavingPotential(page, 'Ausflüge', '10');

    await expect(savingPotentials(page).getByRole('rowheader')).toHaveText(['Auto']);
    await expect(savingPotentialOfSubcategory(page, 'Auto', 'Monat')).toHaveText('20,00 €');
  });

  test('Ausgaben ohne Unterkategorie zählen zu „Sonstiges", ohne Sparpotential keine Zeile (SU-2, SU-3)', async ({
    page,
  }) => {
    await addExpense(page, 'Kleinkram', '10', 'Freizeit & Spaß', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '850', 'Lebensunterhalt', 'Miete', 'Monatlich');
    await setSavingPotential(page, 'Kleinkram', '4');

    await expect(savingPotentials(page).getByRole('rowheader')).toHaveText(['Sonstiges']);
    await expect(savingPotentialOfSubcategory(page, 'Sonstiges', 'Monat')).toHaveText('4,00 €');
  });

  test('der Kasten erscheint mit dem ersten Sparpotential und verschwindet mit dem letzten (SU-7, SU-9)', async ({
    page,
  }) => {
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', 'Abonnements', 'Monatlich');
    await expect(page.locator('app-overview-summary .balance-sheet')).toBeVisible();
    await expect(savingPotentials(page)).toHaveCount(0);

    await setSavingPotential(page, 'Streaming', '5');
    await expect(savingPotentialOfSubcategory(page, 'Abonnements', 'Monat')).toHaveText('5,00 €');

    const dialog = await openSavingPotential(page, 'Streaming');
    await dialog.getByRole('button', { name: 'Entfernen' }).click();
    await expect(dialog).toBeHidden();
    await expect(savingPotentials(page)).toHaveCount(0);
  });

  test('auf breitem Bildschirm steht der Kasten links neben der Bilanz, oben bündig (SU-6)', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1280, height: 900 });
    await addExpense(page, 'Streaming', '15', 'Freizeit & Spaß', 'Abonnements', 'Monatlich');
    await setSavingPotential(page, 'Streaming', '5');

    const list = await box(savingPotentials(page).locator('mat-card'));
    const sheet = await box(page.locator('app-overview-summary .balance-sheet'));
    expect(list.x + list.width).toBeLessThanOrEqual(sheet.x);
    expect(Math.abs(list.y - sheet.y)).toBeLessThan(2);
  });

  test('auf dem Handy steht der Kasten unter der Bilanz und passt auf den Bildschirm (SU-8)', async ({
    page,
  }) => {
    await addExpense(
      page,
      'Streaming',
      '15',
      'Freizeit & Spaß',
      'Abonnements und Mitgliedschaften',
      'Monatlich',
    );
    await setSavingPotential(page, 'Streaming', '5');
    await page.setViewportSize({ width: 390, height: 844 });

    const list = await box(savingPotentials(page).locator('mat-card'));
    const sheet = await box(page.locator('app-overview-summary .balance-sheet'));
    expect(list.y).toBeGreaterThanOrEqual(sheet.y + sheet.height);
    expect(list.x).toBeGreaterThanOrEqual(0);
    expect(list.x + list.width).toBeLessThanOrEqual(390);
    const yearly = await box(
      savingPotentialOfSubcategory(page, 'Abonnements und Mitgliedschaften', 'Jahr'),
    );
    expect(yearly.x + yearly.width).toBeLessThanOrEqual(list.x + list.width);
    await expect.poll(() => scrollsSideways(page)).toBe(false);
  });
});
