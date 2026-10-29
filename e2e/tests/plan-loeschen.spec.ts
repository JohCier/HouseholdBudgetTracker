import { expect, Page, test } from "@playwright/test";
import { addExpense, addIncome, registerWithPlan, showAmounts, yearRow } from "./support";

async function showAllYears(page: Page): Promise<void> {
  await page.getByRole("link", { name: "Alle Jahre" }).click();
  await expect(page).toHaveURL("/");
}

/** Takes over the plan shown into the following year, which then shows. */
async function copyToNextYear(page: Page, year: number): Promise<void> {
  await page.getByRole("button", { name: "Als Vorlage übernehmen" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByText(/Posten werden übernommen\./)).toBeVisible();
  await dialog.getByRole("button", { name: "Übernehmen" }).click();
  await expect(page).toHaveURL(`/jahre/${year + 1}`);
}

function overviewYears(page: Page) {
  return page.locator("app-yearly-overview-page tbody tr[data-year] th");
}

// docs/specs/plan-loeschen.md
test.describe("Haushaltsplan löschen", () => {
  const year = new Date().getFullYear();

  test("löscht einen Plan aus der Jahresübersicht nach Rückfrage mit Jahr und Anzahl der Posten, ohne ihn zu öffnen (PL-1, PL-7 bis PL-9, GP-8)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await addExpense(page, "Miete", "900", "Lebensunterhalt");
    await copyToNextYear(page, year);
    await showAllYears(page);
    await expect(overviewYears(page)).toHaveText([String(year + 1), String(year)]);

    await yearRow(page, year + 1)
      .getByRole("button", { name: `Plan ${year + 1} löschen` })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading")).toHaveText(`Plan ${year + 1} löschen`);
    await expect(
      dialog.getByText(
        `Der Haushaltsplan ${year + 1} mit 2 Posten wird unter „Gelöschte Pläne" abgelegt und lässt sich dort wiederherstellen.`,
      ),
    ).toBeVisible();
    await dialog.getByRole("button", { name: "Löschen" }).click();

    await expect(page.getByText(`Plan ${year + 1} gelöscht.`)).toBeVisible();
    await expect(page).toHaveURL("/");
    await expect(overviewYears(page)).toHaveText([String(year)]);
  });

  test("Abbrechen lässt den Plan unverändert (PL-8)", async ({ page }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await showAllYears(page);

    await yearRow(page, year)
      .getByRole("button", { name: `Plan ${year} löschen` })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByText(/mit 1 Posten wird unter/)).toBeVisible();
    await dialog.getByRole("button", { name: "Abbrechen" }).click();

    await expect(dialog).toBeHidden();
    await expect(overviewYears(page)).toHaveText([String(year)]);
    await showAmounts(page, "Jährlich");
    await expect(yearRow(page, year)).toContainText("3.000,00");
  });

  test("löscht den angezeigten Plan aus der Planansicht und zeigt danach die Jahresübersicht; das Jahr lässt sich neu und leer anlegen (PL-2, PL-7, PL-9)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");

    await page.getByRole("button", { name: "Plan löschen" }).click();
    const dialog = page.getByRole("dialog");
    await expect(
      dialog.getByText(`Der Haushaltsplan ${year} mit 1 Posten wird unter`, {
        exact: false,
      }),
    ).toBeVisible();
    await dialog.getByRole("button", { name: "Löschen" }).click();

    await expect(page).toHaveURL("/");
    await expect(page.getByText(`Plan ${year} gelöscht.`)).toBeVisible();
    await expect(page.getByText("Noch keine Haushaltspläne.")).toBeVisible();

    await page.getByRole("button", { name: "Haushaltsplan anlegen" }).click();
    await expect(page).toHaveURL(`/jahre/${year}`);
    await expect(
      page.getByText("Noch keine Einträge in diesem Haushaltsplan."),
    ).toBeVisible();
  });

  test("ein leerer Plan lässt sich löschen, der Dialog nennt 0 Posten (PL-1, PL-8)", async ({
    page,
  }) => {
    await registerWithPlan(page);

    await page.getByRole("button", { name: "Plan löschen" }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByText(/mit 0 Posten wird unter/)).toBeVisible();
    await dialog.getByRole("button", { name: "Löschen" }).click();

    await expect(page.getByText("Noch keine Haushaltspläne.")).toBeVisible();
  });

  test("andere Pläne bleiben erhalten, das Folgejahr zeigt keine Veränderung zum Vorjahr mehr (PL-3, JU-4)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await copyToNextYear(page, year);
    await showAllYears(page);
    await expect(yearRow(page, year + 1)).toContainText("±0,00 %");

    await yearRow(page, year)
      .getByRole("button", { name: `Plan ${year} löschen` })
      .click();
    await page.getByRole("dialog").getByRole("button", { name: "Löschen" }).click();

    await expect(overviewYears(page)).toHaveText([String(year + 1)]);
    await showAmounts(page, "Jährlich");
    await expect(yearRow(page, year + 1)).toContainText("3.000,00");
    await expect(yearRow(page, year + 1)).not.toContainText("%");
  });
});
