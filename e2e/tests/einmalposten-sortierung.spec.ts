import { expect, Page, test } from '@playwright/test';
import {
  addOneTimeItem,
  drag,
  fillOneTimeItemDialog,
  oneTimeArea,
  oneTimeDescriptions,
  oneTimeItemRow,
  oneTimeTotal,
  registerWithPlan,
  showOneTimeItems,
  type OneTimeItemInput,
} from './support';

const year = new Date().getFullYear();

const backPayment: OneTimeItemInput = {
  type: 'Ausgabe',
  description: 'Nebenkostennachzahlung',
  amount: '1200',
  date: `${year}-03-15`,
};
const taxRefund: OneTimeItemInput = {
  type: 'Einnahme',
  description: 'Steuererstattung',
  amount: '800',
  date: `${year}-07-10`,
};
const repair: OneTimeItemInput = {
  type: 'Ausgabe',
  description: 'Autoreparatur',
  amount: '650',
  date: `${year}-09-22`,
};

/** Moves an item by dragging its handle onto the place of another item of its area (ES-4). */
async function dragOnto(page: Page, description: string, target: string): Promise<void> {
  await drag(
    page,
    page.getByRole('button', {
      name: `Einmalposten ${description} verschieben`,
    }),
    oneTimeItemRow(page, target),
    /\/position$/,
  );
}

// docs/specs/einmalposten-sortierung.md
test.describe('Einmalposten getrennt und frei sortiert', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('Einnahmen oben, Ausgaben darunter, je mit Summe, darunter der Saldo (ES-1, ES-2, ES-6)', async ({
    page,
  }) => {
    await addOneTimeItem(page, backPayment);
    await addOneTimeItem(page, taxRefund);
    await addOneTimeItem(page, repair);

    const areas = page.locator('app-one-time-items section h3');
    await expect(areas).toHaveText(['Einnahmen', 'Ausgaben']);
    await expect(oneTimeDescriptions(page, 'Einnahmen')).toHaveText(['Steuererstattung']);
    await expect(oneTimeDescriptions(page, 'Ausgaben')).toHaveText([
      'Nebenkostennachzahlung',
      'Autoreparatur',
    ]);
    await expect(page.locator('app-one-time-items .type')).toHaveCount(0);
    await expect(oneTimeTotal(page, 'Einnahmen')).toHaveText('+800,00 €');
    await expect(oneTimeTotal(page, 'Ausgaben')).toHaveText('−1.850,00 €');
    await expect(oneTimeTotal(page, 'Saldo')).toHaveText('-1.050,00 €');
    await expect(oneTimeTotal(page, 'Saldo')).toHaveClass(/negative/);
  });

  test('fehlt eine Art, zeigt ihr Bereich das mit der Summe 0,00 € (ES-3)', async ({ page }) => {
    await addOneTimeItem(page, { ...taxRefund, description: 'Bonus' });

    await expect(oneTimeArea(page, 'Ausgaben')).toContainText('Keine einmaligen Ausgaben.');
    await expect(oneTimeTotal(page, 'Ausgaben')).toHaveText('0,00 €');
    await expect(page.getByRole('button', { name: 'Einmalposten Bonus verschieben' })).toHaveCount(
      0,
    );
  });

  test('per Ziehen am Griff sortiert, die Reihenfolge bleibt nach dem Neuladen (ES-4, ES-5)', async ({
    page,
  }) => {
    await addOneTimeItem(page, backPayment);
    await addOneTimeItem(page, repair);

    await dragOnto(page, 'Autoreparatur', 'Nebenkostennachzahlung');
    await expect(oneTimeDescriptions(page, 'Ausgaben')).toHaveText([
      'Autoreparatur',
      'Nebenkostennachzahlung',
    ]);

    await page.reload();
    await showOneTimeItems(page);
    await expect(oneTimeDescriptions(page, 'Ausgaben')).toHaveText([
      'Autoreparatur',
      'Nebenkostennachzahlung',
    ]);
  });

  test('mit der Pfeiltaste auf dem Griff sortiert (ES-4)', async ({ page }) => {
    await addOneTimeItem(page, backPayment);
    await addOneTimeItem(page, repair);

    const saved = page.waitForResponse(
      (response) => response.request().method() === 'PUT' && /\/position$/.test(response.url()),
    );
    await page
      .getByRole('button', { name: 'Einmalposten Autoreparatur verschieben' })
      .press('ArrowUp');
    await expect(oneTimeDescriptions(page, 'Ausgaben')).toHaveText([
      'Autoreparatur',
      'Nebenkostennachzahlung',
    ]);
    expect((await saved).ok()).toBe(true);
  });

  test('ein neuer Einmalposten steht unten; ein geändertes Datum behält den Platz, eine andere Art nicht (ES-7, ES-8)', async ({
    page,
  }) => {
    test.slow();
    await addOneTimeItem(page, backPayment);
    await addOneTimeItem(page, repair);
    await dragOnto(page, 'Autoreparatur', 'Nebenkostennachzahlung');

    await addOneTimeItem(page, {
      ...repair,
      description: 'Kaution',
      date: `${year}-01-01`,
    });
    await expect(oneTimeDescriptions(page, 'Ausgaben')).toHaveText([
      'Autoreparatur',
      'Nebenkostennachzahlung',
      'Kaution',
    ]);

    await page.getByRole('button', { name: 'Einmalposten Autoreparatur bearbeiten' }).click();
    await fillOneTimeItemDialog(page, {
      ...repair,
      amount: '700',
      date: `${year}-02-01`,
    });
    await expect(oneTimeItemRow(page, 'Autoreparatur')).toContainText('−700,00 €');
    await expect(oneTimeDescriptions(page, 'Ausgaben')).toHaveText([
      'Autoreparatur',
      'Nebenkostennachzahlung',
      'Kaution',
    ]);

    await addOneTimeItem(page, taxRefund);
    await page.getByRole('button', { name: 'Einmalposten Autoreparatur bearbeiten' }).click();
    await fillOneTimeItemDialog(page, { ...repair, type: 'Einnahme' });
    await expect(oneTimeDescriptions(page, 'Einnahmen')).toHaveText([
      'Steuererstattung',
      'Autoreparatur',
    ]);
  });

  test('Löschen geht sofort, „Rückgängig" legt den Einmalposten unten wieder an (ES-10, ES-11)', async ({
    page,
  }) => {
    await addOneTimeItem(page, { ...repair, note: 'Werkstatt' });
    await addOneTimeItem(page, backPayment);

    await page.getByRole('button', { name: 'Einmalposten Autoreparatur löschen' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(oneTimeItemRow(page, 'Autoreparatur')).toBeHidden();
    const snackBar = page.locator('mat-snack-bar-container').filter({ hasText: 'gelöscht' });
    await expect(snackBar).toContainText('„Autoreparatur" gelöscht');

    await snackBar.getByRole('button', { name: 'Rückgängig' }).click();
    await expect(oneTimeItemRow(page, 'Autoreparatur')).toContainText(`22.09.${year}`);
    await expect(oneTimeItemRow(page, 'Autoreparatur')).toContainText('Werkstatt');
    await expect(oneTimeArea(page, 'Ausgaben').locator('[role="listitem"]')).toHaveCount(2);
    await expect(oneTimeArea(page, 'Ausgaben').locator('[role="listitem"]').last()).toContainText(
      'Autoreparatur',
    );
  });

  test('auf dem Handy passen die Bereiche auf den Bildschirm (ES-1, MA-1)', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await addOneTimeItem(page, backPayment);
    await addOneTimeItem(page, repair);

    await expect(
      page.getByRole('button', {
        name: 'Einmalposten Autoreparatur verschieben',
      }),
    ).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
      390,
    );
  });
});
