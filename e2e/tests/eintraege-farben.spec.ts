import { expect, Locator, Page, test } from '@playwright/test';
import { addExpense, addIncome, itemGroup, itemRow, registerWithPlan, sumRow } from './support';

type Rgb = { r: number; g: number; b: number };

/** Background color of an element as computed by the browser, also for `color-mix()` (EF-1). */
async function background(locator: Locator): Promise<Rgb> {
  const value = await locator.evaluate((element) => getComputedStyle(element).backgroundColor);
  const srgb = value.match(/color\(srgb ([\d.]+) ([\d.]+) ([\d.]+)/);
  if (srgb) {
    return { r: +srgb[1] * 255, g: +srgb[2] * 255, b: +srgb[3] * 255 };
  }
  const [r, g, b] = value.match(/[\d.]+/g)!.map(Number);
  return { r, g, b };
}

function differs(a: Rgb, b: Rgb): boolean {
  return Math.abs(a.r - b.r) + Math.abs(a.g - b.g) + Math.abs(a.b - b.b) > 3;
}

function fontWeight(locator: Locator): Promise<number> {
  return locator.evaluate((element) => Number(getComputedStyle(element).fontWeight));
}

async function rightEdge(locator: Locator): Promise<number> {
  const box = (await locator.boundingBox())!;
  return box.x + box.width;
}

/** Background of the card "Einträge", against which groups and sums are tinted. */
function card(page: Page): Locator {
  return page.locator('app-budget-entries mat-card');
}

/** Groups tinted in the color of their category, the sum of the category on a stronger tint (EF-1, EF-2). */
async function expectTintsAndSums(page: Page): Promise<void> {
  const cardBackground = await background(card(page));
  const auto = itemGroup(page, 'Lebensunterhalt', 'Auto');
  const haus = itemGroup(page, 'Lebensunterhalt', 'Haus');
  const autoBackground = await background(auto);
  expect(differs(autoBackground, cardBackground)).toBe(true);
  // Brownish: more red than blue, as the color of Lebensunterhalt
  expect(autoBackground.r).toBeGreaterThan(autoBackground.b);
  // A gap with the background of the card between two groups
  const autoBox = (await auto.boundingBox())!;
  const hausBox = (await haus.boundingBox())!;
  expect(hausBox.y).toBeGreaterThan(autoBox.y + autoBox.height);

  const categorySum = sumRow(page, 'Lebensunterhalt');
  expect(differs(await background(categorySum), autoBackground)).toBe(true);
  expect(differs(await background(categorySum), cardBackground)).toBe(true);
  expect(await fontWeight(categorySum)).toBeGreaterThanOrEqual(700);
  expect(await fontWeight(sumRow(page, 'Lebensunterhalt', 'Auto'))).toBeLessThan(700);
}

// docs/specs/eintraege-farben.md
test.describe('Einträge: Unterkategorien farblich absetzen', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '4850', 'Arbeit', 'Monatlich');
    await addExpense(page, 'Sprit', '120', 'Lebensunterhalt', 'Auto', 'Monatlich');
    await addExpense(page, 'Darlehen', '1830', 'Lebensunterhalt', 'Haus', 'Monatlich');
    await addExpense(
      page,
      'Grundsteuer und Gebäudeversicherung',
      '1200',
      'Lebensunterhalt',
      'Haus',
    );
  });

  test('Unterkategorien sind in der Farbe ihrer Kategorie getönt, die Kategoriesumme hebt sich ab (EF-1, EF-2)', async ({
    page,
  }) => {
    await expectTintsAndSums(page);
  });

  test('bei den Einnahmen ist die Gruppe grünlich getönt und „Summe Einnahmen" fett (EF-1, EF-2, EF-3)', async ({
    page,
  }) => {
    const arbeit = await background(itemGroup(page, 'Einnahmen', 'Arbeit'));
    expect(differs(arbeit, await background(card(page)))).toBe(true);
    expect(arbeit.g).toBeGreaterThan(arbeit.r);
    expect(await fontWeight(sumRow(page, 'Einnahmen'))).toBeGreaterThanOrEqual(700);
    expect(await fontWeight(sumRow(page, 'Ausgaben'))).toBeGreaterThanOrEqual(700);
  });

  test('im dunklen Farbschema gelten dieselben Unterschiede (EF-5)', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await expectTintsAndSums(page);
  });

  test('auf dem Handy mit großer Schrift stehen alle Beträge in ihren Spalten (EF-4)', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 412, height: 915 });
    // As with the font size of the phone set to 120 %
    await page.addStyleTag({ content: 'html { font-size: 120% }' });
    await expect
      .poll(() =>
        page.evaluate(
          () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
        ),
      )
      .toBe(false);
    const columns = page.locator('app-budget-entries .columns');
    const month = await rightEdge(columns.getByText('Monat'));
    const year = await rightEdge(columns.getByText('Jahr'));
    const rows = [
      itemRow(page, 'Darlehen').locator('.amount.monthly'),
      sumRow(page, 'Lebensunterhalt', 'Haus').locator('.total.monthly'),
      sumRow(page, 'Lebensunterhalt').locator('.total.monthly'),
      sumRow(page, 'Ausgaben').locator('.total.monthly'),
    ];
    for (const amount of rows) {
      expect(Math.abs((await rightEdge(amount)) - month)).toBeLessThan(2);
    }
    expect(
      Math.abs((await rightEdge(sumRow(page, 'Lebensunterhalt').locator('.total.yearly'))) - year),
    ).toBeLessThan(2);
    // Some room between the amounts and the edge of the tint
    expect(await rightEdge(itemGroup(page, 'Lebensunterhalt', 'Haus'))).toBeGreaterThan(year + 4);
  });
});
