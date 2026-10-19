import { expect, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  categoryRow,
  pieLegendItem,
  pieSlices,
  registerWithPlan,
  showBreakdownAs,
} from './support';

// docs/specs/tortendiagramm.md
test.describe('Tortendiagramm der 50/30/20-Regel', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('die Übersicht zeigt zuerst die Balken, umschalten führt zur Torte und zurück (TD-1)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000');

    const view = page.getByRole('radiogroup', { name: 'Darstellung' });
    await expect(view.getByRole('radio', { name: 'Balken' })).toBeChecked();
    await expect(categoryRow(page, 'Lebensunterhalt')).toBeVisible();
    await expect(page.locator('app-category-pie')).toBeHidden();

    await showBreakdownAs(page, 'Torte');
    await expect(view.getByRole('radio', { name: 'Torte' })).toBeChecked();
    await expect(pieLegendItem(page, 'Lebensunterhalt')).toBeVisible();
    await expect(categoryRow(page, 'Lebensunterhalt')).toBeHidden();

    await showBreakdownAs(page, 'Balken');
    await expect(categoryRow(page, 'Lebensunterhalt')).toBeVisible();
    await expect(page.locator('app-category-pie')).toBeHidden();
  });

  test('ein ausgeglichener Plan hat drei Stücke und eine Legende ohne Zielhinweis (TD-2, TD-3, TD-5, TD-9)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000');
    await addExpense(page, 'Miete', '1500', 'Lebensunterhalt');
    await addExpense(page, 'Urlaub', '600', 'Freizeit & Spaß');
    await addExpense(page, 'ETF-Sparplan', '900', 'Sparen & Investieren');

    await showBreakdownAs(page, 'Torte');

    await expect(pieSlices(page)).toHaveCount(3);
    await expect(
      page.getByRole('img', {
        name: 'Lebensunterhalt: 1.500,00 €, 50 % des Einkommens',
      }),
    ).toBeVisible();
    await expect(pieLegendItem(page, 'Lebensunterhalt')).toContainText('50 % von max. 50 %');
    await expect(pieLegendItem(page, 'Freizeit & Spaß')).toContainText('20 % von max. 30 %');
    await expect(pieLegendItem(page, 'Sparen & Investieren')).toContainText('30 % von mind. 20 %');
    await expect(page.getByText(/Ziel (überschritten|nicht erreicht)/)).toHaveCount(0);
  });

  test('zu hohe Kosten zeigen den Zielhinweis, leere Kategorien haben kein Stück (TD-3, TD-5, TD-6)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000');
    await addExpense(page, 'Miete', '1200', 'Lebensunterhalt');

    await showBreakdownAs(page, 'Torte');

    await expect(pieLegendItem(page, 'Lebensunterhalt')).toContainText(
      '60 % von max. 50 % · Ziel überschritten',
    );
    const sparen = pieLegendItem(page, 'Sparen & Investieren');
    await expect(sparen).toContainText('davon nicht verplant: 800,00 €');
    await expect(sparen).toContainText('40 % von mind. 20 %');
    await expect(pieLegendItem(page, 'Freizeit & Spaß')).toContainText('0,00 €');
    await expect(pieSlices(page)).toHaveCount(2);
    await expect(page.getByRole('img', { name: /^Freizeit & Spaß/ })).toHaveCount(0);
  });

  test('die Torte bleibt gewählt, wenn ein Eintrag hinzukommt (TD-1, TD-3)', async ({ page }) => {
    await addIncome(page, 'Gehalt', '2000');
    await showBreakdownAs(page, 'Torte');

    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt');

    await expect(
      page.getByRole('radiogroup', { name: 'Darstellung' }).getByRole('radio', { name: 'Torte' }),
    ).toBeChecked();
    await expect(pieLegendItem(page, 'Lebensunterhalt')).toContainText('1.000,00 €');
    await expect(pieLegendItem(page, 'Lebensunterhalt')).toContainText('50 % von max. 50 %');
  });

  test('bei einem Defizit zeigt die Torte die Aufteilung der Ausgaben (TD-4)', async ({ page }) => {
    await addExpense(page, 'Miete', '100', 'Lebensunterhalt');

    await showBreakdownAs(page, 'Torte');

    await expect(pieSlices(page)).toHaveCount(1);
    await expect(
      page.getByRole('img', {
        name: 'Lebensunterhalt: 100,00 €, 0 % des Einkommens',
      }),
    ).toBeVisible();
  });

  test('ein leerer Plan zeigt einen leeren Kreis und alle Kategorien mit 0,00 € (TD-7)', async ({
    page,
  }) => {
    await showBreakdownAs(page, 'Torte');

    await expect(page.getByText('Noch keine Ausgaben oder Einnahmen')).toBeVisible();
    await expect(pieSlices(page)).toHaveCount(0);
    for (const category of [
      'Lebensunterhalt',
      'Freizeit & Spaß',
      'Sparen & Investieren',
    ] as const) {
      await expect(pieLegendItem(page, category)).toContainText('0,00 €');
    }
  });
});
