import { expect, Page, test } from "@playwright/test";
import {
  addExpense,
  addIncome,
  register,
  registerWithPlan,
  showAmounts,
  yearRow,
} from "./support";

function periodSwitch(page: Page) {
  return page.getByRole("radiogroup", {
    name: "Beträge monatlich oder jährlich",
  });
}

/** Cells of a row in the yearly overview: income, expenses, balance. */
function cells(page: Page, year: number) {
  const row = yearRow(page, year);
  return {
    income: row.locator("td").nth(0),
    expense: row.locator("td").nth(1),
    balance: row.locator("td").nth(2),
  };
}

async function showAllYears(page: Page): Promise<void> {
  await page.getByRole("link", { name: "Alle Jahre" }).click();
  await expect(page).toHaveURL("/");
}

// docs/specs/jahresuebersicht-monatsbetraege.md
test.describe("Monatsbeträge in der Jahresübersicht", () => {
  const year = new Date().getFullYear();

  test("ohne Plan gibt es keinen Umschalter (JM-1)", async ({ page }) => {
    await register(page);

    await expect(page.getByText("Noch keine Haushaltspläne.")).toBeVisible();
    await expect(periodSwitch(page)).toBeHidden();
  });

  test('zeigt zuerst Monatsbeträge mit derselben Veränderung zum Vorjahr, nach „Jährlich" die Jahresbeträge (JM-1, JM-2, JM-3, JM-5, JM-7)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "30900");
    await addExpense(page, "Miete", "24250", "Lebensunterhalt");
    await page.getByRole("button", { name: "Vorheriges Jahr" }).click();
    await page.getByRole("button", { name: "Haushaltsplan anlegen" }).click();
    await expect(
      page.getByText("Noch keine Einträge in diesem Haushaltsplan."),
    ).toBeVisible();
    await addIncome(page, "Gehalt", "30000");
    await addExpense(page, "Miete", "25000", "Lebensunterhalt");

    await showAllYears(page);

    await expect(
      periodSwitch(page).getByRole("radio", { name: "Monatlich" }),
    ).toBeChecked();
    const current = cells(page, year);
    await expect(current.income).toContainText("2.575,00 €");
    await expect(current.income).toContainText("+3,00 %");
    await expect(current.expense).toContainText("2.020,83 €");
    await expect(current.expense).toContainText("−3,00 %");
    await expect(current.balance).toContainText("554,17 €");
    await expect(current.balance).toContainText("+33,00 %");

    await showAmounts(page, "Jährlich");

    await expect(current.income).toContainText("30.900,00 €");
    await expect(current.income).toContainText("+3,00 %");
    await expect(current.expense).toContainText("24.250,00 €");
    await expect(current.balance).toContainText("6.650,00 €");
    await expect(current.balance).toContainText("+33,00 %");
  });

  test("ein Defizit bleibt auch in Monatsbeträgen gekennzeichnet (JM-3, JM-4)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "1000");
    await addExpense(page, "Miete", "1100", "Lebensunterhalt");

    await showAllYears(page);

    const balance = cells(page, year).balance;
    await expect(balance).toContainText("-8,33 €");
    await expect(balance).toContainText("Defizit");
  });

  test("die gewählte Darstellung bleibt nach dem Öffnen eines Plans und nach dem Neuladen gewählt (JM-7)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, "Gehalt", "1200");
    await showAllYears(page);
    await expect(cells(page, year).income).toContainText("100,00 €");

    await showAmounts(page, "Jährlich");
    await yearRow(page, year).click();
    await expect(page).toHaveURL(`/jahre/${year}`);
    await showAllYears(page);

    await expect(
      periodSwitch(page).getByRole("radio", { name: "Jährlich" }),
    ).toBeChecked();
    await expect(cells(page, year).income).toContainText("1.200,00 €");

    await page.reload();
    await expect(
      periodSwitch(page).getByRole("radio", { name: "Jährlich" }),
    ).toBeChecked();
    await expect(cells(page, year).income).toContainText("1.200,00 €");
  });
});
