import { expect, test } from '@playwright/test';
import { addIncome, itemRow, register } from './support';

// docs/specs/fehlermeldungen.md
test.describe('Fehlermeldungen', () => {
  test('ein doppelter Plan wird auf Deutsch gemeldet (FM-1, FM-2)', async ({ page, context }) => {
    await register(page);
    // Second tab of the same session, still offering to create the plan
    const otherTab = await context.newPage();
    await otherTab.goto('/');
    await expect(otherTab.getByRole('button', { name: 'Haushaltsplan anlegen' })).toBeVisible();

    await page.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();
    await expect(page.getByText('Noch keine Einträge in diesem Haushaltsplan.')).toBeVisible();
    await otherTab.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();

    await expect(
      otherTab.getByText('Für dieses Jahr gibt es bereits einen Haushaltsplan.'),
    ).toBeVisible();
    await expect(otherTab.getByText('already exists')).toBeHidden();
  });

  test('ein bereits gelöschter Posten wird auf Deutsch gemeldet (FM-1, FM-2)', async ({
    page,
    context,
  }) => {
    await register(page);
    await page.getByRole('button', { name: 'Haushaltsplan anlegen' }).click();
    await addIncome(page, 'Gehalt', '3000');
    const otherTab = await context.newPage();
    await otherTab.goto('/');
    await expect(itemRow(otherTab, 'Gehalt')).toBeVisible();

    await page.getByRole('button', { name: 'Eintrag Gehalt löschen' }).click();
    await expect(itemRow(page, 'Gehalt')).toBeHidden();
    await otherTab.getByRole('button', { name: 'Eintrag Gehalt löschen' }).click();

    await expect(otherTab.getByText('Diesen Eintrag gibt es nicht (mehr).')).toBeVisible();
    await expect(otherTab.getByText('not found')).toBeHidden();
  });
});
