import { expect, Page, test } from "@playwright/test";
import {
  addIncome,
  addOneTimeItem,
  deletedPlanRows,
  deletePlanInOverview,
  itemIn,
  openDeletedPlans,
  oneTimeItemRow,
  openYear,
  showOneTimeItems,
  registerWithPlan,
  yearRow,
} from "./support";

async function showAllYears(page: Page): Promise<void> {
  await page.getByRole("link", { name: "Alle Jahre" }).click();
  await expect(page).toHaveURL("/");
}

function overviewYears(page: Page) {
  return page.locator("app-yearly-overview-page tbody tr[data-year] th");
}

/** Creates an empty plan for the year shown, which has none (HP-5). */
async function createPlanHere(page: Page): Promise<void> {
  await page.getByRole("button", { name: "Haushaltsplan anlegen" }).click();
  await expect(page.getByText("Noch keine Einträge in diesem Haushaltsplan.")).toBeVisible();
}

// docs/specs/geloeschte-plaene.md
test.describe("Gelöschte Pläne", () => {
  const year = new Date().getFullYear();

  test("ein gelöschter Plan verschwindet aus der Jahresübersicht, steht unter „Gelöschte Pläne“ und lässt sich mit „Rückgängig“ zurückholen (GP-7 bis GP-9, GP-21)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await showAllYears(page);
    await expect(page.getByRole("link", { name: /Gelöschte Pläne/ })).toBeHidden();

    await yearRow(page, year).getByRole("button", { name: `Plan ${year} löschen` }).click();
    await page.getByRole("dialog").getByRole("button", { name: "Löschen" }).click();

    await expect(page.getByText(`Plan ${year} gelöscht.`)).toBeVisible();
    await expect(page.getByText("Noch keine Haushaltspläne.")).toBeVisible();
    await expect(page.getByRole("link", { name: "Gelöschte Pläne (1)" })).toBeVisible();

    await page.getByRole("button", { name: "Rückgängig" }).click();

    await expect(page.getByText(`Plan ${year} wiederhergestellt.`)).toBeVisible();
    await expect(overviewYears(page)).toHaveText([String(year)]);
    await expect(page.getByRole("link", { name: /Gelöschte Pläne/ })).toBeHidden();
  });

  test("„Rückgängig“ gibt es auch nach dem Löschen aus der Planansicht (GP-9)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");

    await page.getByRole("button", { name: "Plan löschen" }).click();
    await page.getByRole("dialog").getByRole("button", { name: "Löschen" }).click();
    await expect(page).toHaveURL("/");
    await page.getByRole("button", { name: "Rückgängig" }).click();

    await expect(overviewYears(page)).toHaveText([String(year)]);
    await openYear(page, year);
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toBeVisible();
  });

  test("ohne Plan im Jahr stellt „Wiederherstellen“ ohne Rückfrage mit allen Posten wieder her (GP-11, GP-12, GP-22, GP-25)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await addIncome(page, "Bonus", "500");
    await showAllYears(page);
    await deletePlanInOverview(page, year);
    await openDeletedPlans(page, 1);

    await expect(deletedPlanRows(page)).toHaveCount(1);
    const row = deletedPlanRows(page).first();
    await expect(row.locator("th")).toHaveText(String(year));
    await expect(row.locator("td").nth(1)).toHaveText(/\b2\s*$/);
    await expect(row).toContainText(/\d\d\.\d\d\.\d{4}, \d\d:\d\d/);

    await row.getByRole("button", { name: "Wiederherstellen" }).click();

    await expect(page.getByRole("dialog")).toBeHidden();
    await expect(page.getByText(`Plan ${year} wiederhergestellt.`)).toBeVisible();
    await expect(page.getByText("Keine gelöschten Pläne.")).toBeVisible();
    await openYear(page, year);
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toBeVisible();
    await expect(itemIn(page, "Einnahmen", "Bonus")).toBeVisible();
  });

  test("gibt es im Jahr schon einen Plan, wird er erst nach Rückfrage ersetzt und selbst gelöscht (GP-13, GP-14, GP-24)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await addIncome(page, "Bonus", "500");
    await showAllYears(page);
    await deletePlanInOverview(page, year);
    await openYear(page, year);
    await createPlanHere(page);
    await addIncome(page, "Neu", "100");
    await showAllYears(page);
    await openDeletedPlans(page, 1);

    await deletedPlanRows(page).first().getByRole("button", { name: "Wiederherstellen" }).click();
    let dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading")).toHaveText(`Plan ${year} wiederherstellen`);
    await expect(dialog).toContainText(
      `Für ${year} gibt es bereits einen Haushaltsplan mit 1 Posten. Er wird durch den gelöschten Plan mit 2 Posten ersetzt und unter „Gelöschte Pläne" abgelegt.`,
    );
    await dialog.getByRole("button", { name: "Abbrechen" }).click();
    await expect(dialog).toBeHidden();
    await expect(deletedPlanRows(page)).toHaveCount(1);

    await deletedPlanRows(page).first().getByRole("button", { name: "Wiederherstellen" }).click();
    dialog = page.getByRole("dialog");
    await dialog.getByRole("button", { name: "Überschreiben" }).click();

    await expect(page.getByText(`Plan ${year} wiederhergestellt.`)).toBeVisible();
    await expect(deletedPlanRows(page)).toHaveCount(1);
    await expect(deletedPlanRows(page).first().locator("td").nth(1)).toHaveText(/\b1\s*$/);
    await openYear(page, year);
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toBeVisible();
    await expect(itemIn(page, "Einnahmen", "Neu")).toBeHidden();
  });

  test("mehrere gelöschte Pläne stehen nach Jahr, darin der zuletzt gelöschte oben (GP-24)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Erster", "100");
    await showAllYears(page);
    await deletePlanInOverview(page, year);
    await openYear(page, year - 1);
    await createPlanHere(page);
    await openYear(page, year);
    await createPlanHere(page);
    await addIncome(page, "Zweiter", "100");
    await addIncome(page, "Dritter", "100");
    await showAllYears(page);
    await deletePlanInOverview(page, year);
    await deletePlanInOverview(page, year - 1);

    await openDeletedPlans(page, 3);

    await expect(deletedPlanRows(page).locator("th")).toHaveText([
      String(year),
      String(year),
      String(year - 1),
    ]);
    await expect(deletedPlanRows(page).nth(0).locator("td").nth(1)).toHaveText(/\b2\s*$/);
    await expect(deletedPlanRows(page).nth(1).locator("td").nth(1)).toHaveText(/\b1\s*$/);
  });

  test("„Endgültig löschen“ entfernt den Plan nach Rückfrage für immer (GP-16, GP-17)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await showAllYears(page);
    await deletePlanInOverview(page, year);
    await openDeletedPlans(page, 1);

    await deletedPlanRows(page).first().getByRole("button", { name: "Endgültig löschen" }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading")).toHaveText(`Plan ${year} endgültig löschen`);
    await expect(dialog).toContainText(
      `Der gelöschte Haushaltsplan ${year} mit 1 Posten wird endgültig gelöscht. Das lässt sich nicht rückgängig machen.`,
    );
    await dialog.getByRole("button", { name: "Abbrechen" }).click();
    await expect(deletedPlanRows(page)).toHaveCount(1);

    await deletedPlanRows(page).first().getByRole("button", { name: "Endgültig löschen" }).click();
    await page.getByRole("dialog").getByRole("button", { name: "Endgültig löschen" }).click();

    await expect(page.getByText(`Plan ${year} endgültig gelöscht.`)).toBeVisible();
    await expect(page.getByText("Keine gelöschten Pläne.")).toBeVisible();
    await page.getByRole("link", { name: "Alle Jahre" }).click();
    await expect(page.getByRole("link", { name: /Gelöschte Pläne/ })).toBeHidden();
  });

  test("ein beim Übernehmen überschriebener Plan steht unter „Gelöschte Pläne“ (GP-19, GP-20)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await openYear(page, year + 1);
    await createPlanHere(page);
    await addIncome(page, "Bonus", "500");
    await openYear(page, year);

    await page.getByRole("button", { name: "Als Vorlage übernehmen" }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("alert")).toContainText("unter „Gelöschte Pläne\" abgelegt");
    await dialog.getByRole("button", { name: "Überschreiben" }).click();
    await expect(page).toHaveURL(`/jahre/${year + 1}`);

    await showAllYears(page);
    await openDeletedPlans(page, 1);
    await expect(deletedPlanRows(page).first().locator("th")).toHaveText(String(year + 1));

    await deletedPlanRows(page).first().getByRole("button", { name: "Wiederherstellen" }).click();
    await page.getByRole("dialog").getByRole("button", { name: "Überschreiben" }).click();
    await openYear(page, year + 1);
    await expect(itemIn(page, "Einnahmen", "Bonus")).toBeVisible();
  });

  test("Löschen, Wiederherstellen und endgültiges Löschen lassen die Einmalposten, wie sie sind (GP-6)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addOneTimeItem(page, {
      type: "Ausgabe",
      description: "Nebenkostennachzahlung",
      amount: "480",
      date: `${year}-03-15`,
    });
    await showAllYears(page);
    await deletePlanInOverview(page, year);
    await openDeletedPlans(page, 1);
    await deletedPlanRows(page).first().getByRole("button", { name: "Wiederherstellen" }).click();
    await expect(page.getByText(`Plan ${year} wiederhergestellt.`)).toBeVisible();
    await openYear(page, year);
    await showOneTimeItems(page);
    await expect(oneTimeItemRow(page, "Nebenkostennachzahlung")).toBeVisible();

    await page.getByRole("button", { name: "Plan löschen" }).click();
    await page.getByRole("dialog").getByRole("button", { name: "Löschen" }).click();
    await expect(page).toHaveURL("/");
    await openDeletedPlans(page, 1);
    await deletedPlanRows(page).first().getByRole("button", { name: "Endgültig löschen" }).click();
    await page.getByRole("dialog").getByRole("button", { name: "Endgültig löschen" }).click();
    await expect(page.getByText("Keine gelöschten Pläne.")).toBeVisible();

    await openYear(page, year);
    await expect(page.getByText(`Für ${year} gibt es noch keinen Haushaltsplan.`)).toBeVisible();
    await showOneTimeItems(page);
    await expect(oneTimeItemRow(page, "Nebenkostennachzahlung")).toBeVisible();
  });

  test("auf einem 360 px breiten Bildschirm steht jeder gelöschte Plan ohne waagerechtes Scrollen (GP-26)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await showAllYears(page);
    await deletePlanInOverview(page, year);
    await page.setViewportSize({ width: 360, height: 800 });
    await openDeletedPlans(page, 1);

    await expect(deletedPlanRows(page).first().getByRole("button", { name: "Endgültig löschen" })).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
      ),
    ).toBe(false);
  });
});
