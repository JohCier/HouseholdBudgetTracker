import { expect, test } from '@playwright/test';
import {
  addExpense,
  addIncome,
  categoryRow,
  pieLegendItem,
  pieSlices,
  registerWithPlan,
  showPie,
  showTab,
} from './support';

// docs/specs/tortendiagramm.md
test.describe('Tortendiagramm der 50/30/20-Regel', () => {
  test.beforeEach(async ({ page }) => {
    await registerWithPlan(page);
  });

  test('die Balken stehen oben ohne Umschalter, die Torte im Tab „Detailansicht" (AP-2, AP-9)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '2000');

    await expect(page.getByRole('radiogroup', { name: 'Darstellung' })).toHaveCount(0);
    await expect(categoryRow(page, 'Lebensunterhalt')).toBeVisible();
    await expect(page.locator('app-category-pie')).toHaveCount(0);

    await showPie(page);
    await expect(pieLegendItem(page, 'Lebensunterhalt')).toBeVisible();
    await expect(categoryRow(page, 'Lebensunterhalt')).toBeVisible();

    await showTab(page, 'Regelmäßige Posten');
    await expect(page.locator('app-category-pie')).toHaveCount(0);
  });

  test('ein ausgeglichener Plan hat drei Stücke und eine Legende ohne Zielhinweis (TD-2, TD-3, TD-5, TD-9)', async ({
    page,
  }) => {
    await addIncome(page, 'Gehalt', '3000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '1500', 'Lebensunterhalt', undefined, 'Monatlich');
    await addExpense(page, 'Urlaub', '600', 'Freizeit & Spaß', undefined, 'Monatlich');
    await addExpense(page, 'ETF-Sparplan', '900', 'Sparen & Investieren', undefined, 'Monatlich');

    await showPie(page);

    await expect(pieSlices(page)).toHaveCount(3);
    await expect(
      page.getByRole('button', {
        name: 'Lebensunterhalt: 1.500,00 € im Monat, 50 % des Einkommens; Lebensunterhalt anzeigen',
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
    await addIncome(page, 'Gehalt', '2000', undefined, 'Monatlich');
    await addExpense(page, 'Miete', '1200', 'Lebensunterhalt', undefined, 'Monatlich');

    await showPie(page);

    await expect(pieLegendItem(page, 'Lebensunterhalt')).toContainText(
      '60 % von max. 50 % · Ziel überschritten',
    );
    const freizeit = pieLegendItem(page, 'Freizeit & Spaß');
    await expect(freizeit).toContainText('davon nicht verplant: 800,00 €');
    await expect(freizeit).toContainText('40 % von max. 30 % · Ziel überschritten');
    await expect(pieLegendItem(page, 'Sparen & Investieren')).toContainText('0,00 €');
    await expect(pieSlices(page)).toHaveCount(2);
    await expect(page.getByRole('button', { name: /^Sparen & Investieren:/ })).toHaveCount(0);
  });

  test('die Torte zeigt einen neu erfassten Eintrag (TD-3)', async ({ page }) => {
    await addIncome(page, 'Gehalt', '2000', undefined, 'Monatlich');
    await showPie(page);

    await addExpense(page, 'Miete', '1000', 'Lebensunterhalt', undefined, 'Monatlich');
    await showPie(page);

    await expect(pieLegendItem(page, 'Lebensunterhalt')).toContainText('1.000,00 €');
    await expect(pieLegendItem(page, 'Lebensunterhalt')).toContainText('50 % von max. 50 %');
  });

  test('bei einem Defizit zeigt die Torte die Aufteilung der Ausgaben (TD-4)', async ({ page }) => {
    await addExpense(page, 'Miete', '100', 'Lebensunterhalt', undefined, 'Monatlich');

    await showPie(page);

    await expect(pieSlices(page)).toHaveCount(1);
    await expect(
      page.getByRole('button', {
        name: 'Lebensunterhalt: 100,00 € im Monat, 0 % des Einkommens; Lebensunterhalt anzeigen',
      }),
    ).toBeVisible();
  });

  test('ein leerer Plan zeigt einen leeren Kreis und alle Kategorien mit 0,00 € (TD-7)', async ({
    page,
  }) => {
    await showPie(page);

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
