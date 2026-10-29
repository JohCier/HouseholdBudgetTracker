import { expect, Locator, Page } from '@playwright/test';

export const PASSWORD = 'geheim-1234';

/** A fresh address per test, so tests stay independent of each other and of earlier runs. */
export function uniqueEmail(): string {
  return `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.org`;
}

export async function register(page: Page, email = uniqueEmail()): Promise<string> {
  await page.goto('/register');
  await page.getByLabel('E-Mail-Adresse').fill(email);
  await page.getByLabel('Passwort', { exact: true }).fill(PASSWORD);
  await page.getByLabel('Passwort wiederholen').fill(PASSWORD);
  await page.getByRole('button', { name: 'Konto anlegen' }).click();
  await expect(page.getByText(email)).toBeVisible();
  return email;
}

/** Waits until the session has ended, so the next page load starts signed out. */
export async function logout(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Abmelden' }).click();
  await expect(page).toHaveURL('/login');
}

export async function login(page: Page, email: string, password = PASSWORD): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-Mail-Adresse').fill(email);
  await page.getByLabel('Passwort').fill(password);
  await page.getByRole('button', { name: 'Anmelden' }).click();
}

/** Opens the plan of a year, or the offer to create one (JU-11). */
export async function openYear(page: Page, year: number): Promise<void> {
  await page.goto(`/jahre/${year}`);
  await expect(page.getByRole('heading', { name: String(year), exact: true })).toBeVisible();
}

/** Row of a year in the yearly overview (JU-1). */
export function yearRow(page: Page, year: number) {
  return page.locator(`app-yearly-overview-page tbody tr[data-year="${year}"]`);
}

/** Switches the yearly overview to amounts per month or per year; the browser remembers it (JM-1, JM-7). */
export async function showAmounts(page: Page, period: 'Monatlich' | 'Jährlich'): Promise<void> {
  const toggle = page
    .getByRole('radiogroup', { name: 'Beträge monatlich oder jährlich' })
    .getByRole('radio', { name: period });
  await toggle.click();
  await expect(toggle).toBeChecked();
}

/**
 * Registers a new user and creates the plan for the current year from the yearly overview, which
 * then opens it (JU-12).
 */
export async function registerWithPlan(page: Page): Promise<string> {
  const email = await register(page);
  await page.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();
  await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
  return email;
}

export type Category = 'Lebensunterhalt' | 'Freizeit & Spaß' | 'Sparen & Investieren';

/** Whether an amount is entered per month or per year (MJ-1). */
export type Period = 'Monatlich' | 'Jährlich';

/** Chooses the period of the amount; the tests take amounts as yearly unless they say otherwise. */
async function choosePeriod(page: Page, period: Period): Promise<void> {
  await page
    .getByRole('radiogroup', { name: 'Angabe monatlich oder jährlich' })
    .getByRole('radio', { name: period })
    .click();
}

export async function addIncome(
  page: Page,
  description: string,
  amount: string,
  subcategory?: string,
  period: Period = 'Jährlich',
): Promise<void> {
  await page.getByRole('radio', { name: 'Einnahme' }).click();
  await page.getByLabel('Beschreibung').fill(description);
  await page.getByLabel('Betrag').fill(amount);
  await choosePeriod(page, period);
  await fillSubcategory(page, subcategory);
  await page.getByRole('button', { name: 'Hinzufügen', exact: true }).click();
  await expect(itemRow(page, description)).toBeVisible();
}

export async function addExpense(
  page: Page,
  description: string,
  amount: string,
  category: Category,
  subcategory?: string,
  period: Period = 'Jährlich',
): Promise<void> {
  await page.getByRole('radio', { name: 'Ausgabe', exact: true }).click();
  await page.getByLabel('Beschreibung').fill(description);
  await page.getByLabel('Betrag').fill(amount);
  await choosePeriod(page, period);
  await page.getByRole('combobox', { name: 'Kategorie', exact: true }).click();
  await page.getByRole('option', { name: category }).click();
  await fillSubcategory(page, subcategory);
  await page.getByRole('button', { name: 'Hinzufügen', exact: true }).click();
  await expect(itemRow(page, description)).toBeVisible();
}

/** Types the subcategory, if any, and closes the suggestions, so they cover nothing (UK-6). */
async function fillSubcategory(page: Page, subcategory: string | undefined): Promise<void> {
  if (subcategory !== undefined) {
    await page.getByRole('combobox', { name: 'Unterkategorie (optional)' }).fill(subcategory);
    await page.keyboard.press('Tab');
  }
}

/**
 * Opens the edit dialog of an item and waits until the dialog has moved the focus to its first
 * field. Typing before that can land in the wrong field, because the dialog takes the focus only
 * once it has opened.
 */
export async function openEditDialog(page: Page, description: string) {
  await page.getByRole('button', { name: `Eintrag ${description} bearbeiten` }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByLabel('Beschreibung')).toBeFocused();
  return dialog;
}

/** Entry of an item in the card "Einträge". */
export function itemRow(page: Page, description: string) {
  return page.locator('app-budget-entries .item').filter({ hasText: description });
}

/** The incomes or one category of the expenses in the card "Einträge" (UK-9, UK-10). */
export function entrySection(page: Page, section: 'Einnahmen' | Category) {
  return page.locator(`app-budget-entries section[aria-label="${section}"]`);
}

/** Entry of an item within the incomes or one category of the expenses (UK-9, UK-10). */
export function itemIn(page: Page, section: 'Einnahmen' | Category, description: string) {
  return entrySection(page, section).locator('.item').filter({ hasText: description });
}

/** Subcategory group within the incomes or a category, with its sum and its items (UK-11). */
export function itemGroup(page: Page, section: 'Einnahmen' | Category, subcategory: string) {
  return entrySection(page, section).getByRole('group', {
    name: subcategory,
    exact: true,
  });
}

/**
 * Row with the sum below a subcategory group, or below a category or the incomes or expenses as a
 * whole (EG-5, EG-6).
 */
export function sumRow(
  page: Page,
  of: 'Einnahmen' | 'Ausgaben' | Category,
  subcategory?: string,
): Locator {
  if (subcategory !== undefined) {
    return itemGroup(page, of as 'Einnahmen' | Category, subcategory).locator('.group-summary');
  }
  return page
    .locator('app-budget-entries .summary')
    .filter({ has: page.getByText(`Summe ${of}`, { exact: true }) });
}

/** Names of the subcategory groups of the incomes or a category, top first (UK-14). */
export function groupNames(page: Page, section: 'Einnahmen' | Category) {
  return entrySection(page, section).locator('.group-name');
}

/** Descriptions of the items of a group, top first (UK-14). */
export function itemDescriptions(page: Page, section: 'Einnahmen' | Category, subcategory: string) {
  return itemGroup(page, section, subcategory).locator('.item .description');
}

/** Opens the dialog of the saving potential of an expense (PM-4b). */
export async function openSavingPotential(page: Page, description: string) {
  await page.getByRole('button', { name: `Sparpotential bei ${description}`, exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading')).toHaveText(`Sparpotential bei ${description}`);
  return dialog;
}

/** Saves the saving potential of an expense, or the prefilled monthly amount (PM-4b). */
export async function setSavingPotential(page: Page, description: string, monthly?: string) {
  const dialog = await openSavingPotential(page, description);
  if (monthly !== undefined) {
    await dialog.getByLabel('Betrag pro Monat').fill(monthly);
  }
  await dialog.getByRole('button', { name: 'Speichern' }).click();
  await expect(dialog).toBeHidden();
}

/** Yearly or monthly value of the row "Gesamteinnahmen", "Gesamtausgaben" or "Saldo" in the box "Bilanz" (BI-1). */
export function balanceSheet(
  page: Page,
  label: 'Gesamteinnahmen' | 'Gesamtausgaben' | 'Saldo' | 'Sparpotential',
  per: 'Jahr' | 'Monat' = 'Jahr',
) {
  return page
    .locator('app-overview-summary .balance-sheet tbody tr')
    .filter({ has: page.getByRole('rowheader', { name: label, exact: true }) })
    .locator(per === 'Jahr' ? '.value.yearly' : '.value.monthly');
}

/** Monthly or yearly saving potential of a subcategory in the box beside the balance sheet (SU-6). */
export function savingPotentialOfSubcategory(
  page: Page,
  subcategory: string,
  per: 'Jahr' | 'Monat' = 'Monat',
) {
  return page
    .locator('app-saving-potential-summary tbody tr')
    .filter({ has: page.getByRole('rowheader', { name: subcategory, exact: true }) })
    .locator(per === 'Jahr' ? '.value.yearly' : '.value.monthly');
}

/** Row of a category in the 50/30/20 card, with its total and ratio. */
export function categoryRow(page: Page, category: Category) {
  return page.locator('app-category-breakdown .row').filter({ hasText: category });
}

/** Switches the 50/30/20 card between "Balken" and "Torte" (TD-1). */
export async function showBreakdownAs(page: Page, view: 'Balken' | 'Torte'): Promise<void> {
  await page
    .getByRole('radiogroup', { name: 'Darstellung' })
    .getByRole('radio', { name: view })
    .click();
}

/** Legend entry of a category in the pie chart of the 50/30/20 card. */
export function pieLegendItem(page: Page, category: Category) {
  return page.locator('app-category-pie .legend-item').filter({ hasText: category });
}

/** Slices of the pie chart, each labelled with name, amount and share (TD-9, TK-12). */
export function pieSlices(page: Page) {
  return page.locator('app-category-pie .slice');
}

/** Selects the part of the plan the pie shows: the rule, a category or one of its groups (TK-1). */
export async function showInPie(page: Page, category?: Category, subcategory?: string) {
  const name = !category
    ? '50/30/20-Regel'
    : subcategory
      ? `${category} › ${subcategory}`
      : category;
  const selection = page.getByRole('combobox', { name: 'Ausschnitt' });
  await selection.click();
  await page.getByRole('option', { name, exact: true }).click();
  await expect(selection).toContainText(name);
  // Until the list has closed, a click on the field does not open it again
  await expect(page.getByRole('listbox')).toBeHidden();
}

/** Line of a group, an item or "nicht verplant" in the legend of a category or group (TK-8). */
export function pieEntry(page: Page, name: string) {
  return page.locator('app-category-pie .legend-item').filter({
    has: page.locator('.name', { hasText: new RegExp(`^\\s*${name}\\s*$`) }),
  });
}

/** The selected category or group with its total above the legend (TK-7). */
export function pieSummary(page: Page) {
  return page.locator('app-category-pie .selection-summary');
}

export type NetWorthType = 'Vermögenswert' | 'Schuld';

/** Records an asset or a liability on the page "Vermögen" shown, from the list of its type (VS-11). */
export async function addNetWorthEntry(
  page: Page,
  type: NetWorthType,
  name: string,
  amount: string,
): Promise<void> {
  const list = netWorthList(page, type === 'Schuld' ? 'Schulden' : 'Vermögen');
  const empty = page.getByText(/ist noch kein Vermögen erfasst\./);
  if (await empty.isVisible()) {
    await page.getByRole('button', { name: 'Wert erfassen' }).click();
  } else {
    await list.getByRole('button', { name: 'Wert erfassen' }).click();
  }
  const dialog = page.getByRole('dialog');
  await dialog.getByRole('radio', { name: type }).click();
  await dialog.getByLabel('Bezeichnung').fill(name);
  await dialog.getByLabel('Betrag am Jahresende').fill(amount);
  await dialog.getByRole('button', { name: 'Speichern' }).click();
  await expect(dialog).toBeHidden();
  await expect(netWorthEntry(page, name)).toBeVisible();
}

/** Records assets and liabilities of a year straight through the API, signed in as the page is (VS-1). */
export async function recordNetWorth(
  page: Page,
  year: number,
  entries: readonly (readonly [type: 'ASSET' | 'LIABILITY', name: string, amount: string])[],
): Promise<void> {
  for (const [type, name, amount] of entries) {
    const response = await page.request.post(`/api/net-worth/${year}/entries`, { data: { type, name, amount } });
    expect(response.status()).toBe(201);
  }
}

/** Opens the page "Vermögen" of a year (VS-8). */
export async function openNetWorth(page: Page, year: number): Promise<void> {
  await page.goto(`/vermoegen/${year}`);
  await expect(page.getByRole('heading', { name: `Vermögen ${year}`, exact: true })).toBeVisible();
}

/** The list "Vermögen" or "Schulden" of the page "Vermögen" (VS-8). */
export function netWorthList(page: Page, list: 'Vermögen' | 'Schulden') {
  return page.locator(`app-net-worth-page .list[data-type="${list === 'Schulden' ? 'LIABILITY' : 'ASSET'}"]`);
}

/** The line of an asset or a liability on the page "Vermögen". */
export function netWorthEntry(page: Page, name: string) {
  return page.locator('app-net-worth-page li.entry').filter({ has: page.getByText(name, { exact: true }) });
}

/** The box with the sums of the page "Vermögen" (VS-8). */
export function netWorthSums(page: Page) {
  return page.locator('app-net-worth-page .sums');
}

/** The card "Vermögen" of the yearly overview (VS-18). */
export function netWorthCard(page: Page) {
  return page.locator('app-net-worth-card');
}

/** The row of a year in the card "Vermögen" (VS-18). */
export function netWorthRow(page: Page, year: number) {
  return page.locator(`app-net-worth-card tbody tr[data-year="${year}"]`);
}

/** Sorts the slices of a category or group in the pie by entries, amount or name (TS-1). */
export async function sortPie(page: Page, by: 'Einträge' | 'Betrag' | 'Name') {
  const sort = page.getByRole('combobox', { name: 'Sortierung' });
  await sort.click();
  await page.getByRole('option', { name: by, exact: true }).click();
  await expect(sort).toContainText(by);
  // Until the list has closed, a click on the field does not open it again
  await expect(page.getByRole('listbox')).toBeHidden();
}

/** Names in the legend of a category or group, top to bottom (TK-8). */
export function pieNames(page: Page) {
  return page.locator('app-category-pie .legend-item .name');
}

/** A one-time item as entered in its dialog (EP-1). */
export interface OneTimeItemInput {
  type: 'Einnahme' | 'Ausgabe';
  description: string;
  amount: string;
  /** As the API and an input of type date take it, e.g. "2026-03-15". */
  date: string;
  note?: string;
}

/** Fills the dialog of a one-time item and saves it (EP-7). */
export async function fillOneTimeItemDialog(page: Page, item: OneTimeItemInput): Promise<void> {
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByLabel('Beschreibung')).toBeVisible();
  await dialog.getByRole('radio', { name: item.type }).click();
  await dialog.getByLabel('Beschreibung').fill(item.description);
  await dialog.getByLabel('Betrag').fill(item.amount);
  await dialog.getByLabel('Datum').fill(item.date);
  await dialog.getByLabel('Notiz (optional)').fill(item.note ?? '');
  await dialog.getByRole('button', { name: /^(Erfassen|Speichern)$/ }).click();
  await expect(dialog).toBeHidden();
}

/** Enters a one-time item in the section "Einmalposten" of the year shown (EP-7). */
export async function addOneTimeItem(page: Page, item: OneTimeItemInput): Promise<void> {
  await page.getByRole('button', { name: 'Einmalposten erfassen' }).click();
  await fillOneTimeItemDialog(page, item);
  await expect(oneTimeItemRow(page, item.description)).toBeVisible();
}

/** Entry of a one-time item in the section "Einmalposten" (EP-8). */
export function oneTimeItemRow(page: Page, description: string) {
  return page.locator('app-one-time-items [role="listitem"]').filter({ hasText: description });
}

/** One of the totals below the one-time items of the year shown (EP-9). */
export function oneTimeTotal(page: Page, label: 'Einnahmen' | 'Ausgaben' | 'Saldo') {
  return page
    .locator('app-one-time-items .total-row')
    .filter({ has: page.getByText(label, { exact: true }) })
    .locator('.amount');
}

/** The one-time income, expenses and balance of a year in the yearly overview (EP-13). */
export function oneTimeCells(page: Page, year: number) {
  const cells = yearRow(page, year).locator('td.one-time');
  return { income: cells.nth(0), expense: cells.nth(1), balance: cells.nth(2) };
}

/** Switches the card "Top 5" between "Sparpotential" and "Ausgaben" (TP-7). */
export async function showTopItems(page: Page, list: 'Sparpotential' | 'Ausgaben'): Promise<void> {
  const toggle = page.getByRole('radiogroup', { name: 'Liste' }).getByRole('radio', { name: list });
  await toggle.click();
  await expect(toggle).toBeChecked();
}

/** The rows of the list the card "Top 5" shows (TP-8). */
export function topItemRows(page: Page) {
  return page.getByRole('table', { name: 'Top 5' }).locator('tbody tr');
}
