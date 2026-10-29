import { expect, Page, test } from "@playwright/test";
import {
  addExpense,
  addIncome,
  groupNames,
  itemIn,
  openEditDialog,
  openYear,
  register,
  registerWithPlan,
  balanceSheet,
  showAmounts,
  yearRow,
} from "./support";

/** Opens the dialog to take over the plan shown and waits for its target year to be checked. */
async function openCopyDialog(page: Page) {
  await page.getByRole("button", { name: "Als Vorlage übernehmen" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByText(/Posten werden übernommen\./)).toBeVisible();
  return dialog;
}

async function showAllYears(page: Page): Promise<void> {
  await page.getByRole("link", { name: "Alle Jahre" }).click();
  await expect(page).toHaveURL("/");
}

// docs/specs/plan-uebernehmen.md
test.describe("Plan als Vorlage übernehmen", () => {
  const year = new Date().getFullYear();

  test("übernimmt den Plan mit allen Posten, Unterkategorien, Notizen und der Reihenfolge ins vorgeschlagene Folgejahr und öffnet es (PU-1 bis PU-4, PU-8, PU-13, PU-15)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await addExpense(page, "Miete", "900", "Lebensunterhalt", "Wohnen");
    await page.getByLabel("Notiz (optional)").fill("Inspektion im Mai");
    await addExpense(page, "Wartung", "400", "Lebensunterhalt", "Auto");
    await page
      .getByRole("button", { name: "Unterkategorie Auto verschieben" })
      .focus();
    const saved = page.waitForResponse((response) =>
      response.url().endsWith("/subcategory-order"),
    );
    await page.keyboard.press("ArrowDown");
    await saved;
    await expect(groupNames(page, "Lebensunterhalt")).toHaveText([
      "Wohnen",
      "Auto",
    ]);

    const dialog = await openCopyDialog(page);
    await expect(dialog.getByRole("heading")).toHaveText(
      `Plan ${year} als Vorlage übernehmen`,
    );
    await expect(dialog.getByLabel("Zieljahr")).toHaveValue(String(year + 1));
    await expect(dialog.getByText("3 Posten werden übernommen.")).toBeVisible();
    await dialog.getByRole("button", { name: "Übernehmen" }).click();

    await expect(page).toHaveURL(`/jahre/${year + 1}`);
    await expect(
      page.getByText(`Plan ${year} nach ${year + 1} übernommen.`),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: String(year + 1), exact: true }),
    ).toBeVisible();
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toBeVisible();
    await expect(groupNames(page, "Lebensunterhalt")).toHaveText([
      "Wohnen",
      "Auto",
    ]);
    await expect(itemIn(page, "Lebensunterhalt", "Wartung")).toContainText(
      "Inspektion im Mai",
    );
    await expect(balanceSheet(page, "Saldo")).toContainText("1.700,00");
  });

  test("Änderungen im übernommenen Plan lassen die Vorlage unverändert (PU-5)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await addExpense(page, "Miete", "900", "Lebensunterhalt");
    await (
      await openCopyDialog(page)
    )
      .getByRole("button", { name: "Übernehmen" })
      .click();
    await expect(page).toHaveURL(`/jahre/${year + 1}`);

    const edit = await openEditDialog(page, "Gehalt");
    await edit.getByLabel("Betrag").fill("3200");
    await edit.getByRole("button", { name: "Speichern" }).click();
    await page.getByRole("button", { name: "Eintrag Miete löschen" }).click();
    await expect(itemIn(page, "Lebensunterhalt", "Miete")).toBeHidden();

    await openYear(page, year);
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toContainText("3.000,00");
    await expect(itemIn(page, "Lebensunterhalt", "Miete")).toBeVisible();
  });

  test("übernimmt einen Plan aus der Jahresübersicht, ohne ihn zu öffnen, und zeigt das Zieljahr danach als neue Zeile (PU-12, PU-15)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await showAllYears(page);

    await yearRow(page, year)
      .getByRole("button", { name: `Plan ${year} als Vorlage übernehmen` })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByText("1 Posten werden übernommen.")).toBeVisible();
    await dialog.getByRole("button", { name: "Übernehmen" }).click();

    await expect(page).toHaveURL(`/jahre/${year + 1}`);
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toBeVisible();
    await showAllYears(page);
    await expect(page.locator("app-yearly-overview-page tbody tr[data-year] th")).toHaveText([
      String(year + 1),
      String(year),
    ]);
    await showAmounts(page, "Jährlich");
    await expect(
      yearRow(page, year + 1)
        .locator("td")
        .nth(0),
    ).toContainText("3.000,00");
  });

  test("ein bestehender Plan im Zieljahr wird nur nach Rückfrage überschrieben (PU-9, PU-13, PU-14)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await addExpense(page, "Miete", "900", "Lebensunterhalt");
    await openYear(page, year + 1);
    await page.getByRole("button", { name: "Haushaltsplan anlegen" }).click();
    await addIncome(page, "Bonus", "500");
    await openYear(page, year);

    let dialog = await openCopyDialog(page);
    await expect(dialog.getByRole("alert")).toContainText(
      `Für ${year + 1} gibt es bereits einen Haushaltsplan mit 1 Posten. Er wird überschrieben und unter „Gelöschte Pläne" abgelegt.`,
    );
    await dialog.getByRole("button", { name: "Abbrechen" }).click();
    await openYear(page, year + 1);
    await expect(itemIn(page, "Einnahmen", "Bonus")).toBeVisible();

    await openYear(page, year);
    dialog = await openCopyDialog(page);
    await expect(
      dialog.getByRole("button", { name: "Übernehmen" }),
    ).toBeHidden();
    await dialog.getByRole("button", { name: "Überschreiben" }).click();

    await expect(page).toHaveURL(`/jahre/${year + 1}`);
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toBeVisible();
    await expect(itemIn(page, "Lebensunterhalt", "Miete")).toBeVisible();
    await expect(itemIn(page, "Einnahmen", "Bonus")).toBeHidden();
  });

  test("das Zieljahr ist frei wählbar, aber nicht das Jahr der Vorlage (PU-2)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");

    const dialog = await openCopyDialog(page);
    await dialog.getByLabel("Zieljahr").fill(String(year));
    await dialog.getByLabel("Zieljahr").blur();
    await expect(
      dialog.getByText(`Bitte ein anderes Jahr als ${year} wählen.`),
    ).toBeVisible();
    await expect(
      dialog.getByRole("button", { name: "Übernehmen" }),
    ).toBeDisabled();

    await dialog.getByLabel("Zieljahr").fill(String(year - 3));
    await dialog.getByRole("button", { name: "Übernehmen" }).click();

    await expect(page).toHaveURL(`/jahre/${year - 3}`);
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toBeVisible();
  });

  test("ein Jahr ohne Plan übernimmt einen anderen Plan, vorbelegt mit dem neuesten davor (PU-12, PU-13)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "3000");
    await openYear(page, year + 2);

    await page
      .getByRole("button", { name: "Aus anderem Jahr übernehmen" })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading")).toHaveText(
      `Plan für ${year + 2} übernehmen`,
    );
    await expect(
      dialog.getByRole("combobox", { name: "Vorlage" }),
    ).toContainText(`Plan ${year}`);
    await expect(dialog.getByText("1 Posten werden übernommen.")).toBeVisible();
    await dialog.getByRole("button", { name: "Übernehmen" }).click();

    await expect(
      page.getByText(`Plan ${year} nach ${year + 2} übernommen.`),
    ).toBeVisible();
    await expect(page).toHaveURL(`/jahre/${year + 2}`);
    await expect(itemIn(page, "Einnahmen", "Gehalt")).toBeVisible();
  });

  test('ohne Plan bietet ein Jahr nur „Haushaltsplan anlegen" an (PU-12)', async ({
    page,
  }) => {
    await register(page);
    await openYear(page, year + 2);

    await expect(
      page.getByRole("button", { name: "Haushaltsplan anlegen" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Aus anderem Jahr übernehmen" }),
    ).toBeHidden();
    await expect(
      page.getByRole("button", { name: "Als Vorlage übernehmen" }),
    ).toBeHidden();
  });
});
