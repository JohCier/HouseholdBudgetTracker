import { expect, Page } from '@playwright/test';

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
  await page.getByRole('button', { name: 'Hinzufügen' }).click();
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
  await page.getByRole('radio', { name: 'Ausgabe' }).click();
  await page.getByLabel('Beschreibung').fill(description);
  await page.getByLabel('Betrag').fill(amount);
  await choosePeriod(page, period);
  await page.getByRole('combobox', { name: 'Kategorie', exact: true }).click();
  await page.getByRole('option', { name: category }).click();
  await fillSubcategory(page, subcategory);
  await page.getByRole('button', { name: 'Hinzufügen' }).click();
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
  return entrySection(page, section).getByRole('group', { name: subcategory, exact: true });
}

/** Names of the subcategory groups of the incomes or a category, top first (UK-14). */
export function groupNames(page: Page, section: 'Einnahmen' | Category) {
  return entrySection(page, section).locator('.group-name');
}

/** Descriptions of the items of a group, top first (UK-14). */
export function itemDescriptions(
  page: Page,
  section: 'Einnahmen' | Category,
  subcategory: string,
) {
  return itemGroup(page, section, subcategory).locator('.item .description');
}

/** Yearly or monthly value of one of the tiles "Einnahmen", "Ausgaben", "Saldo" (MJ-13). */
export function tile(
  page: Page,
  label: 'Einnahmen' | 'Ausgaben' | 'Saldo',
  per: 'Jahr' | 'Monat' = 'Jahr',
) {
  return page
    .locator('app-overview-summary .tile')
    .filter({ hasText: label })
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

/** Slices of the pie chart, each labelled with category, total and share of income. */
export function pieSlices(page: Page) {
  return page.locator('app-category-pie').getByRole('img');
}
