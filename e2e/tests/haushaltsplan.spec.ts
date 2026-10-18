import { expect, test } from '@playwright/test';
import { addIncome, itemRow, register, registerWithPlan } from './support';

// docs/specs/haushaltsplan.md
test.describe('Haushaltsplan', () => {
  const year = new Date().getFullYear();

  test('ohne Plan bietet die Oberfläche an, einen anzulegen; ein neuer Plan ist leer (HP-5)', async ({
    page,
  }) => {
    await register(page);

    await expect(page.getByRole('heading', { name: String(year) })).toBeVisible();
    await expect(page.getByText(`Für ${year} gibt es noch keinen Haushaltsplan.`)).toBeVisible();

    await page.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();

    await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Haushaltsplan anlegen' })).toBeHidden();
  });

  test('mit den Pfeilen wechselt der Benutzer das Jahr, jeder Plan gilt für ein Jahr (HP-1)', async ({
    page,
  }) => {
    await registerWithPlan(page);
    await addIncome(page, 'Gehalt', '3000');

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await expect(page.getByRole('heading', { name: String(year + 1) })).toBeVisible();
    await expect(
      page.getByText(`Für ${year + 1} gibt es noch keinen Haushaltsplan.`),
    ).toBeVisible();

    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();
    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click();
    await expect(page.getByRole('heading', { name: String(year - 1) })).toBeVisible();
    await expect(
      page.getByText(`Für ${year - 1} gibt es noch keinen Haushaltsplan.`),
    ).toBeVisible();

    await page.getByRole('button', { name: 'Nächstes Jahr' }).click();
    await expect(itemRow(page, 'Gehalt')).toBeVisible();
  });

  test('ein Benutzer sieht nur seine eigenen Pläne (HP-4)', async ({ browser }) => {
    const alice = await browser.newPage();
    await registerWithPlan(alice);
    await addIncome(alice, 'Gehalt von A', '3000');

    // Separate context, so B has its own session cookie
    const bob = await (await browser.newContext()).newPage();
    await register(bob);

    await expect(bob.getByText(`Für ${year} gibt es noch keinen Haushaltsplan.`)).toBeVisible();
    await expect(bob.getByText('Gehalt von A')).toBeHidden();
  });
});
