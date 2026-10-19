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

/** Registers a new user and creates the plan for the current year. */
export async function registerWithPlan(page: Page): Promise<string> {
  const email = await register(page);
  await page.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();
  await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
  return email;
}

export type Category = 'Lebensunterhalt' | 'Freizeit & Spaß' | 'Sparen & Investieren';

export async function addIncome(page: Page, description: string, amount: string): Promise<void> {
  await page.getByRole('radio', { name: 'Einnahme' }).click();
  await page.getByLabel('Beschreibung').fill(description);
  await page.getByLabel('Betrag').fill(amount);
  await page.getByRole('button', { name: 'Hinzufügen' }).click();
  await expect(itemRow(page, description)).toBeVisible();
}

export async function addExpense(
  page: Page,
  description: string,
  amount: string,
  category: Category,
): Promise<void> {
  await page.getByRole('radio', { name: 'Ausgabe' }).click();
  await page.getByLabel('Beschreibung').fill(description);
  await page.getByLabel('Betrag').fill(amount);
  await page.getByRole('combobox', { name: 'Kategorie' }).click();
  await page.getByRole('option', { name: category }).click();
  await page.getByRole('button', { name: 'Hinzufügen' }).click();
  await expect(itemRow(page, description)).toBeVisible();
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

export function itemRow(page: Page, description: string) {
  return page.getByRole('row').filter({ hasText: description });
}

/** Value of one of the tiles "Einnahmen", "Ausgaben", "Saldo". */
export function tile(page: Page, label: 'Einnahmen' | 'Ausgaben' | 'Saldo') {
  return page.locator('app-overview-summary .tile').filter({ hasText: label }).locator('.value');
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
