import { expect, Locator, Page, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  openYear,
  registerWithPlan,
  showTab,
  type PlanTab,
} from './support';

async function box(locator: Locator) {
  const result = await locator.boundingBox();
  expect(result).not.toBeNull();
  return result!;
}

const tabs = (page: Page) => page.getByRole('tab');

const tab = (page: Page, name: PlanTab) => page.getByRole('tab', { name, exact: true });

/** Whether the page is wider than the screen, so it scrolls sideways (MA-1). */
async function scrollsSideways(page: Page): Promise<boolean> {
  return page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  );
}

// docs/specs/planansicht-aufbau.md
test.describe('Aufbau der Planansicht', () => {
  test('oben stehen Bilanz und 50/30/20-Regel nebeneinander und gleich hoch, darunter die Tabs (AP-1, AP-5)', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1280, height: 900 });
    await registerWithPlan(page);

    const summary = await box(page.locator('app-overview-summary'));
    const rule = await box(page.locator('app-category-breakdown mat-card'));
    expect(summary.x + summary.width).toBeLessThan(rule.x);
    expect(Math.abs(summary.y - rule.y)).toBeLessThan(2);
    expect(Math.abs(summary.height - rule.height)).toBeLessThan(2);

    await expect(tabs(page)).toHaveText([
      'Regelmäßige Posten',
      'Einmalposten',
      'Detailansicht',
      'Sparhebel',
    ]);
    await expect(tab(page, 'Regelmäßige Posten')).toHaveAttribute('aria-selected', 'true');
    expect((await box(tabs(page).first())).y).toBeGreaterThan(rule.y + rule.height);
  });

  test('jeder Tab zeigt nur seinen Inhalt (AP-7 bis AP-9, AP-11)', async ({ page }) => {
    await registerWithPlan(page);

    await expect(page.getByRole('button', { name: 'Posten erfassen', exact: true })).toBeVisible();
    await expect(page.locator('app-budget-entries')).toBeVisible();

    await showTab(page, 'Einmalposten');
    await expect(page.getByRole('button', { name: 'Einmalposten erfassen' })).toBeVisible();
    await expect(page.locator('app-budget-entries')).toHaveCount(0);

    await showTab(page, 'Detailansicht');
    await expect(page.locator('app-category-pie')).toBeVisible();
    await expect(page.locator('app-one-time-items')).toHaveCount(0);

    await showTab(page, 'Sparhebel');
    await expect(page.locator('app-saving-potential-summary')).toBeVisible();
    await expect(page.locator('app-top-items')).toHaveCount(2);
    await expect(page.locator('app-category-pie')).toHaveCount(0);
  });

  test('„Einmalposten erfassen" steht oben im Tab wie „Posten erfassen", unten gibt es keinen Knopf mehr (AP-18)', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1280, height: 900 });
    await registerWithPlan(page);
    const addItem = page.getByRole('button', { name: 'Posten erfassen', exact: true });
    const itemsButton = await box(addItem);
    const itemsClass = await addItem.getAttribute('class');

    await showTab(page, 'Einmalposten');
    const addOneTime = page.getByRole('button', { name: 'Einmalposten erfassen' });
    await expect(addOneTime).toHaveCount(1);
    const oneTimeButton = await box(addOneTime);
    expect(Math.abs(oneTimeButton.x - itemsButton.x)).toBeLessThan(2);
    expect(Math.abs(oneTimeButton.y - itemsButton.y)).toBeLessThan(2);
    expect(await addOneTime.getAttribute('class')).toBe(itemsClass);
    await expect(page.locator('app-one-time-items button', { hasText: 'erfassen' })).toHaveCount(0);

    await addOneTime.click();
    await expect(page.getByRole('dialog')).toContainText('Einmalposten');
  });

  test('ohne Defizit steht unter der Bilanz eine Meldung mit Haken, bei Defizit die Warnung (AP-17, BI-6)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '4455', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '2515', 'Lebensunterhalt', undefined, 'Monatlich');

    const surplus = page.locator('app-overview-summary [role="status"]');
    await expect(surplus).toHaveText(
      /Deine Einnahmen decken deine Ausgaben\. Im Monat bleiben dir 1\.940,00\s€ übrig\./,
    );
    await expect(surplus.locator('mat-icon[svgicon="check_circle"]')).toBeVisible();
    await expect(page.getByRole('alert')).toHaveCount(0);

    await addExpense(page, 'Auto', '3000', 'Lebensunterhalt', undefined, 'Monatlich');
    await expect(page.getByRole('alert')).toContainText(
      'Deine Ausgaben übersteigen deine Einnahmen.',
    );
    await expect(surplus).toHaveCount(0);
  });

  test('in der Detailansicht steht die Legende neben der großen Torte (AP-9)', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 900 });
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await showTab(page, 'Detailansicht');

    const pie = await box(page.locator('app-category-pie .pie'));
    const legend = await box(page.locator('app-category-pie .legend'));
    expect(pie.width).toBeGreaterThan(300);
    expect(legend.x).toBeGreaterThan(pie.x + pie.width);
  });

  test('ein Jahr ohne Plan hat nur „Regelmäßige Posten" mit dem Hinweis und „Einmalposten" (AP-13)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    const year = new Date().getFullYear();
    await showTab(page, 'Sparhebel');

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await expect(
      page.getByText(`Für ${year + 1} gibt es noch keinen Haushaltsplan.`),
    ).toBeVisible();
    await expect(page.locator('app-overview-summary')).toHaveCount(0);
    await expect(tabs(page)).toHaveText(['Regelmäßige Posten', 'Einmalposten']);
    await expect(tab(page, 'Regelmäßige Posten')).toHaveAttribute('aria-selected', 'true');

    await showTab(page, 'Einmalposten');
    await expect(page.locator('app-one-time-items')).toContainText('Keine Einmalposten.');

    await openYear(page, year + 1);
    await expect(tab(page, 'Regelmäßige Posten')).toHaveAttribute('aria-selected', 'true');
  });

  test('auf dem Handy stehen Bilanz, 50/30/20-Regel und Tabs untereinander, ohne waagerechtes Scrollen (AP-4, AP-6)', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await registerWithPlan(page);

    const summary = await box(page.locator('app-overview-summary'));
    const rule = await box(page.locator('app-category-breakdown mat-card'));
    const firstTab = await box(tabs(page).first());
    expect(rule.y).toBeGreaterThanOrEqual(summary.y + summary.height);
    expect(firstTab.y).toBeGreaterThanOrEqual(rule.y + rule.height);

    await showTab(page, 'Sparhebel');
    await expect.poll(() => scrollsSideways(page)).toBe(false);
  });
});
