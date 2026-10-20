import { expect, Locator, Page, test } from "@playwright/test";
import {
  addExpense,
  addIncome,
  itemRow,
  registerWithPlan,
  sumRow,
  yearRow,
} from "./support";

const PHONE = { width: 390, height: 844 };

/** Whether the page is wider than the screen, so it scrolls sideways (MA-1). */
async function scrollsSideways(page: Page): Promise<boolean> {
  return page.evaluate(
    () =>
      document.documentElement.scrollWidth >
      document.documentElement.clientWidth,
  );
}

async function box(locator: Locator) {
  const result = await locator.boundingBox();
  expect(result).not.toBeNull();
  return result!;
}

/** Whether an element lies within the width of the screen, wherever it is when scrolled down (MA-1). */
async function expectWithinScreenWidth(locator: Locator): Promise<void> {
  await expect(locator).toBeVisible();
  expect(await rightEdge(locator)).toBeLessThanOrEqual(PHONE.width);
}

/** Right edge of an element, to check that amounts line up below their column header. */
async function rightEdge(locator: Locator): Promise<number> {
  const { x, width } = await box(locator);
  return x + width;
}

// docs/specs/mobile-ansicht.md
test.describe("Ansicht auf schmalen Bildschirmen", () => {
  test.beforeEach(async ({ page }) => {
    // Filled in on a wide screen, as the form is the same on every width
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000", undefined, "Monatlich");
    await addExpense(
      page,
      "Darlehen",
      "830",
      "Lebensunterhalt",
      "Haus",
      "Monatlich",
    );
    await addExpense(
      page,
      "Grundsteuer und Gebäudeversicherung",
      "1200",
      "Lebensunterhalt",
      "Haus",
    );
    await addExpense(
      page,
      "Streaming",
      "18",
      "Freizeit & Spaß",
      undefined,
      "Monatlich",
    );
  });

  test("der Plan ist auf dem Handy nicht breiter als der Bildschirm (MA-1)", async ({
    page,
  }) => {
    await page.setViewportSize(PHONE);
    await expect.poll(() => scrollsSideways(page)).toBe(false);
  });

  test("ein Posten steht auf dem Handy in zwei Zeilen, die Beträge unter ihren Spalten (MA-2, MA-4)", async ({
    page,
  }) => {
    await page.setViewportSize(PHONE);
    const item = itemRow(page, "Grundsteuer und Gebäudeversicherung");
    const description = item.locator(".description");
    const monthly = item.locator(".amount.monthly");
    const yearly = item.locator(".amount.yearly");

    // At most three lines, instead of one letter per line
    const lineHeight = await description.evaluate((element) =>
      parseFloat(getComputedStyle(element).lineHeight),
    );
    expect((await box(description)).height).toBeLessThanOrEqual(
      3 * lineHeight + 16,
    );
    // The amounts below the description, lined up below "Monat" and "Jahr"
    expect((await box(monthly)).y).toBeGreaterThanOrEqual(
      (await box(description)).y + (await box(description)).height - 1,
    );
    const columns = page.locator("app-budget-entries .columns");
    const monthColumn = columns.getByText("Monat", { exact: true });
    const yearColumn = columns.getByText("Jahr", { exact: true });
    expect(
      Math.abs((await rightEdge(monthly)) - (await rightEdge(monthColumn))),
    ).toBeLessThan(2);
    expect(
      Math.abs((await rightEdge(yearly)) - (await rightEdge(yearColumn))),
    ).toBeLessThan(2);
    await expectWithinScreenWidth(
      page.getByRole("button", {
        name: "Eintrag Grundsteuer und Gebäudeversicherung löschen",
      }),
    );
  });

  test("ein Posten ohne Griff steht bündig mit einem Posten mit Griff (MA-4)", async ({
    page,
  }) => {
    await page.setViewportSize(PHONE);
    const withHandle = await box(
      itemRow(page, "Darlehen").locator(".description"),
    );
    const withoutHandle = await box(
      itemRow(page, "Streaming").locator(".description"),
    );
    expect(Math.abs(withHandle.x - withoutHandle.x)).toBeLessThan(2);
  });

  test("in einer Summenzeile stehen auf dem Handy die Beträge unter der Bezeichnung (MA-3)", async ({
    page,
  }) => {
    await page.setViewportSize(PHONE);
    const sum = sumRow(page, "Lebensunterhalt", "Haus");
    const label = await box(sum.getByText("Summe Haus"));
    const monthly = sum.locator(".total.monthly");
    await expect(monthly).toHaveText("930,00 €");
    await expect(sum.locator(".total.yearly")).toHaveText("11.160,00 €");
    expect((await box(monthly)).y).toBeGreaterThanOrEqual(
      label.y + label.height - 1,
    );
    const monthColumn = page
      .locator("app-budget-entries .columns")
      .getByText("Monat");
    expect(
      Math.abs((await rightEdge(monthly)) - (await rightEdge(monthColumn))),
    ).toBeLessThan(2);
    await expectWithinScreenWidth(
      page.getByRole("button", { name: "Posten in Haus hinzufügen" }),
    );
  });

  test("auf einem breiten Bildschirm steht ein Posten weiterhin in einer Zeile (MA-2)", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1280, height: 900 });
    const item = itemRow(page, "Darlehen");
    const description = await box(item.locator(".description"));
    const monthly = await box(item.locator(".amount.monthly"));
    expect(monthly.y).toBeLessThan(description.y + description.height);
    expect(monthly.x).toBeGreaterThan(description.x + description.width - 1);
  });

  test("die Jahresübersicht passt auf den Bildschirm des Handys (MA-1, MA-5)", async ({
    page,
  }) => {
    await page.setViewportSize(PHONE);
    await page.goto("/");
    const year = new Date().getFullYear();
    await expect(yearRow(page, year)).toBeVisible();
    await expect.poll(() => scrollsSideways(page)).toBe(false);
    await expect(
      page.getByRole("button", { name: `Plan ${year} löschen` }),
    ).toBeInViewport({
      ratio: 1,
    });
  });

  test('im Dialog „Neuer Eintrag in Haus" ist die Auswahl der Art vollständig zu sehen (MA-6)', async ({
    page,
  }) => {
    await page.setViewportSize(PHONE);
    await page
      .getByRole("button", { name: "Posten in Haus hinzufügen" })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByLabel("Beschreibung")).toBeFocused();
    const type = dialog.getByRole("radiogroup", { name: "Art des Eintrags" });
    expect((await box(type)).height).toBeGreaterThanOrEqual(36);
  });
});
