import { expect, test } from "@playwright/test";
import {
  addNetWorthEntry,
  netWorthCard,
  netWorthEntry,
  netWorthList,
  netWorthRow,
  netWorthSums,
  openNetWorth,
  recordNetWorth,
  register,
  registerWithPlan,
  showAmounts,
} from "./support";

/** The example of the spec: net worth 2.000 € to 13.900 €, with a loss in 2024. */
const HISTORY = {
  2021: [
    ["ASSET", "Girokonto", "5000.00"],
    ["LIABILITY", "Studienkredit", "3000.00"],
  ],
  2022: [
    ["ASSET", "Girokonto", "7000.00"],
    ["LIABILITY", "Studienkredit", "2500.00"],
  ],
  2023: [
    ["ASSET", "Girokonto", "9000.00"],
    ["LIABILITY", "Studienkredit", "3000.00"],
  ],
  2024: [
    ["ASSET", "Girokonto", "15200.00"],
    ["LIABILITY", "Autokredit", "11400.00"],
  ],
  2025: [
    ["ASSET", "Depot", "20100.00"],
    ["LIABILITY", "Autokredit", "9600.00"],
  ],
  2026: [
    ["ASSET", "Girokonto", "3200.00"],
    ["LIABILITY", "Autokredit", "6300.00"],
    ["ASSET", "Tagesgeld", "6000.00"],
    ["ASSET", "ETF-Depot", "12500.00"],
    ["LIABILITY", "Ratenkauf Küche", "1500.00"],
  ],
} as const;

// docs/specs/vermoegen.md
test.describe("Vermögen und Schulden", () => {
  test("erfasst Vermögenswerte und Schulden und zeigt Vermögen, Schulden und Nettovermögen (VS-1, VS-6, VS-8, VS-11)", async ({
    page,
  }) => {
    await register(page);
    await openNetWorth(page, 2026);
    await expect(
      page.getByText("Für 2026 ist noch kein Vermögen erfasst."),
    ).toBeVisible();

    await addNetWorthEntry(page, "Vermögenswert", "Girokonto", "3200");
    await addNetWorthEntry(page, "Vermögenswert", "Tagesgeld", "6000");
    await addNetWorthEntry(page, "Vermögenswert", "ETF-Depot", "12500");
    await addNetWorthEntry(page, "Schuld", "Autokredit", "6300");
    await addNetWorthEntry(page, "Schuld", "Ratenkauf Küche", "1500");

    await expect(
      netWorthList(page, "Vermögen").locator(".entry .name"),
    ).toHaveText(["Girokonto", "Tagesgeld", "ETF-Depot"]);
    await expect(
      netWorthList(page, "Schulden").locator(".entry .name"),
    ).toHaveText(["Autokredit", "Ratenkauf Küche"]);
    await expect(netWorthList(page, "Vermögen").locator(".sum")).toContainText(
      "21.700,00 €",
    );
    await expect(netWorthList(page, "Schulden").locator(".sum")).toContainText(
      "7.800,00 €",
    );
    await expect(netWorthSums(page)).toContainText("Nettovermögen 13.900,00 €");
  });

  test('„Wert erfassen" in der Liste Schulden schlägt eine Schuld vor (VS-11)', async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2026, [["ASSET", "Girokonto", "100.00"]]);
    await openNetWorth(page, 2026);

    await netWorthList(page, "Schulden")
      .getByRole("button", { name: "Wert erfassen" })
      .click();

    await expect(
      page.getByRole("dialog").getByRole("radio", { name: "Schuld" }),
    ).toBeChecked();
  });

  test("bearbeitet einen Wert und löscht ihn erst nach Rückfrage (VS-5, VS-7)", async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2026, [["ASSET", "Girokonto", "3200.00"]]);
    await openNetWorth(page, 2026);

    await netWorthEntry(page, "Girokonto")
      .getByRole("button", { name: "Girokonto bearbeiten" })
      .click();
    const dialog = page.getByRole("dialog");
    await dialog.getByLabel("Betrag am Jahresende").fill("4000");
    await dialog.getByRole("button", { name: "Speichern" }).click();
    await expect(netWorthEntry(page, "Girokonto")).toContainText("4.000,00 €");

    await netWorthEntry(page, "Girokonto")
      .getByRole("button", { name: "Girokonto löschen" })
      .click();
    await expect(page.getByRole("dialog")).toContainText(
      "Wert „Girokonto“ löschen?",
    );
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Abbrechen" })
      .click();
    await expect(netWorthEntry(page, "Girokonto")).toBeVisible();

    await netWorthEntry(page, "Girokonto")
      .getByRole("button", { name: "Girokonto löschen" })
      .click();
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Löschen" })
      .click();
    await expect(
      page.getByText("Für 2026 ist noch kein Vermögen erfasst."),
    ).toBeVisible();
  });

  test("zeigt ein negatives Nettovermögen als überschuldet (VS-6)", async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2026, [
      ["ASSET", "Girokonto", "2000.00"],
      ["LIABILITY", "Kredit", "5000.00"],
    ]);
    await openNetWorth(page, 2026);

    await expect(netWorthSums(page)).toContainText("-3.000,00 €");
    await expect(netWorthSums(page)).toContainText("Überschuldet");
  });

  test("zeigt die Veränderung zum Vorjahr in Euro und Prozent, auch nach einem negativen Vorjahr (VS-12, VS-13)", async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2025, [
      ["ASSET", "Girokonto", "1000.00"],
      ["LIABILITY", "Kredit", "2000.00"],
    ]);
    await recordNetWorth(page, 2026, [
      ["ASSET", "Girokonto", "1500.00"],
      ["LIABILITY", "Kredit", "2000.00"],
    ]);
    await openNetWorth(page, 2026);

    await expect(netWorthSums(page).locator(".change")).toContainText(
      "+500,00 €",
    );
    await expect(netWorthSums(page).locator(".change")).toContainText(
      "+50,00 %",
    );
  });

  test('wechselt mit den Pfeilen das Jahr, „Alle Jahre" führt zur Übersicht (VS-9)', async ({
    page,
  }) => {
    await register(page);
    await openNetWorth(page, 2026);

    await page.getByRole("button", { name: "Nächstes Jahr" }).click();
    await expect(page).toHaveURL("/vermoegen/2027");
    await expect(
      page.getByRole("heading", { name: "Vermögen 2027" }),
    ).toBeVisible();

    await page.getByRole("link", { name: "Alle Jahre" }).click();
    await expect(page).toHaveURL("/");

    await page.goto("/vermoegen/2201");
    await expect(page).toHaveURL("/");
  });

  test("übernimmt das Vermögen ins vorgeschlagene Folgejahr und lässt die Quelle unverändert (VS-15, VS-17)", async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2026, HISTORY[2026]);
    await openNetWorth(page, 2026);

    await page.getByRole("button", { name: "Vermögen übernehmen" }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByLabel("Zieljahr")).toHaveValue("2027");
    await expect(dialog).toContainText(
      "5 Werte werden mit den Beträgen übernommen.",
    );
    await dialog.getByRole("button", { name: "Übernehmen" }).click();

    await expect(page).toHaveURL("/vermoegen/2027");
    await expect(
      page.getByText("Vermögen 2026 nach 2027 übernommen."),
    ).toBeVisible();
    await expect(netWorthSums(page)).toContainText("Nettovermögen 13.900,00 €");

    await netWorthEntry(page, "Girokonto")
      .getByRole("button", { name: "Girokonto bearbeiten" })
      .click();
    await page
      .getByRole("dialog")
      .getByLabel("Betrag am Jahresende")
      .fill("4000");
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Speichern" })
      .click();
    await expect(netWorthEntry(page, "Girokonto")).toContainText("4.000,00 €");

    await openNetWorth(page, 2026);
    await expect(netWorthEntry(page, "Girokonto")).toContainText("3.200,00 €");
  });

  test('warnt vor dem Überschreiben eines Jahres mit Werten; „Abbrechen" ändert nichts (VS-16)', async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2026, HISTORY[2026]);
    await recordNetWorth(page, 2027, [
      ["ASSET", "Sparbuch", "100.00"],
      ["ASSET", "Bargeld", "50.00"],
      ["ASSET", "Bausparvertrag", "900.00"],
      ["LIABILITY", "Dispo", "10.00"],
    ]);
    await openNetWorth(page, 2026);

    await page.getByRole("button", { name: "Vermögen übernehmen" }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toContainText(
      "Für 2027 ist bereits Vermögen mit 4 Werten erfasst. Es wird überschrieben, die Werte lassen sich nicht wiederherstellen.",
    );
    await expect(
      dialog.getByRole("button", { name: "Überschreiben" }),
    ).toBeVisible();
    await dialog.getByRole("button", { name: "Abbrechen" }).click();

    await openNetWorth(page, 2027);
    await expect(netWorthEntry(page, "Sparbuch")).toBeVisible();
  });

  test("übernimmt in einem Jahr ohne Werte aus einem anderen Jahr (VS-10, VS-17)", async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2026, [["ASSET", "Girokonto", "3200.00"]]);
    await openNetWorth(page, 2027);

    await page
      .getByRole("button", { name: "Aus anderem Jahr übernehmen" })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toContainText(
      "1 Wert wird mit den Beträgen übernommen.",
    );
    await dialog.getByRole("button", { name: "Übernehmen" }).click();

    await expect(netWorthEntry(page, "Girokonto")).toBeVisible();
    await expect(
      page.getByText("Vermögen 2026 nach 2027 übernommen."),
    ).toBeVisible();
  });

  test("zeigt in der Jahresübersicht alle Jahre mit Nettovermögen und Veränderung, das neueste oben (VS-12, VS-13, VS-18)", async ({
    page,
  }) => {
    await register(page);
    for (const [year, entries] of Object.entries(HISTORY)) {
      await recordNetWorth(page, Number(year), entries);
    }
    await page.goto("/");

    await expect(netWorthCard(page).locator("tbody th")).toHaveText([
      "2026",
      "2025",
      "2024",
      "2023",
      "2022",
      "2021",
    ]);
    await expect(netWorthCard(page).locator(".hero")).toContainText(
      "13.900,00 €",
    );
    await expect(netWorthCard(page).locator(".hero")).toContainText(
      "+3.400,00 € (+32,38 %) zum Vorjahr",
    );
    await expect(netWorthRow(page, 2026)).toContainText("21.700,00 €");
    await expect(netWorthRow(page, 2026)).toContainText("7.800,00 €");
    await expect(netWorthRow(page, 2026).locator(".change")).toHaveClass(
      /good/,
    );
    await expect(netWorthRow(page, 2024).locator(".change")).toContainText(
      "−2.200,00 €",
    );
    await expect(netWorthRow(page, 2024).locator(".change")).toContainText(
      "−36,67 %",
    );
    await expect(netWorthRow(page, 2024).locator(".change")).toHaveClass(/bad/);
    await expect(netWorthRow(page, 2021).locator(".change")).toHaveText("–");

    await netWorthRow(page, 2025).click();
    await expect(page).toHaveURL("/vermoegen/2025");
  });

  test("zeigt den Verlauf als Linie mit Balken für Wachstum und Verlust und die Zahlen beim Zeigen auf ein Jahr (VS-20, VS-22)", async ({
    page,
  }) => {
    await register(page);
    for (const [year, entries] of Object.entries(HISTORY)) {
      await recordNetWorth(page, Number(year), entries);
    }
    await page.goto("/");
    const chart = page.locator("app-net-worth-chart");

    await expect(chart.locator("circle.point")).toHaveCount(6);
    await expect(chart.locator("polyline.line")).toHaveCount(1);
    await expect(chart.locator("path.bar.gain")).toHaveCount(4);
    await expect(chart.locator("path.bar.loss")).toHaveCount(1);

    await chart.locator('rect.hit[data-year="2024"]').hover();
    await expect(chart.locator(".tooltip")).toContainText("2024");
    await expect(chart.locator(".tooltip")).toContainText(
      "Nettovermögen 3.800,00 €",
    );
    await expect(chart.locator(".tooltip")).toContainText(
      "−2.200,00 € (−36,67 %) zum Vorjahr",
    );
  });

  test("unterbricht die Linie bei einem fehlenden Jahr und vergleicht nicht über die Lücke (VS-12, VS-21)", async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2023, HISTORY[2023]);
    await recordNetWorth(page, 2025, HISTORY[2025]);
    await page.goto("/");
    const chart = page.locator("app-net-worth-chart");

    await expect(chart.locator("polyline.line")).toHaveCount(2);
    await expect(chart.locator("circle.point")).toHaveCount(2);
    await expect(chart.locator("text.axis", { hasText: "2024" })).toBeVisible();
    await expect(chart.locator("path.bar")).toHaveCount(0);
    await expect(netWorthRow(page, 2025).locator(".change")).toHaveText("–");
  });

  test("zeigt mit nur einem Jahr einen Punkt und einen Hinweis (VS-23)", async ({
    page,
  }) => {
    await register(page);
    await recordNetWorth(page, 2026, [["ASSET", "Girokonto", "3200.00"]]);
    await page.goto("/");

    await expect(page.locator("app-net-worth-chart circle.point")).toHaveCount(
      1,
    );
    await expect(
      page.getByText("Ab zwei Jahren zeigt das Diagramm den Verlauf."),
    ).toBeVisible();
  });

  test('zeigt Bestände unabhängig von „Monatlich" oder „Jährlich" (VS-19)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await recordNetWorth(page, 2026, HISTORY[2026]);
    await page.goto("/");

    await showAmounts(page, "Monatlich");
    await expect(netWorthRow(page, 2026)).toContainText("13.900,00 €");
    await showAmounts(page, "Jährlich");
    await expect(netWorthRow(page, 2026)).toContainText("13.900,00 €");
  });

  test('bietet einem neuen Benutzer „Vermögen erfassen" für das aktuelle Jahr an (VS-10, VS-19)', async ({
    page,
  }) => {
    await register(page);
    const year = new Date().getFullYear();

    await expect(netWorthCard(page)).toContainText(
      "Noch kein Vermögen erfasst.",
    );
    await netWorthCard(page)
      .getByRole("link", { name: "Vermögen erfassen" })
      .click();

    await expect(page).toHaveURL(`/vermoegen/${year}`);
    await expect(
      page.getByText(`Für ${year} ist noch kein Vermögen erfasst.`),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Aus anderem Jahr übernehmen" }),
    ).toBeHidden();
  });

  test("bleibt erhalten, wenn der Haushaltsplan des Jahres gelöscht wird (VS-4)", async ({
    page,
  }) => {
    await registerWithPlan(page);
    const year = new Date().getFullYear();
    await recordNetWorth(page, year, [["ASSET", "Girokonto", "3200.00"]]);

    await page.getByRole("button", { name: "Plan löschen" }).click();
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Löschen" })
      .click();
    await expect(page).toHaveURL("/");

    await expect(netWorthRow(page, year)).toContainText("3.200,00 €");
  });

  test("passt auf einem 390 px breiten Bildschirm ohne waagerechtes Scrollen der Seite (VS-24)", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await register(page);
    for (const [year, entries] of Object.entries(HISTORY)) {
      await recordNetWorth(page, Number(year), entries);
    }
    await page.goto("/");
    await expect(page.locator("app-net-worth-chart circle.point")).toHaveCount(
      6,
    );

    expect(
      await page.evaluate(() => document.documentElement.scrollWidth),
    ).toBeLessThanOrEqual(390);
    await expect(netWorthRow(page, 2026)).toContainText(
      "21.700,00 € − 7.800,00 €",
    );

    await openNetWorth(page, 2026);
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth),
    ).toBeLessThanOrEqual(390);
  });
});
