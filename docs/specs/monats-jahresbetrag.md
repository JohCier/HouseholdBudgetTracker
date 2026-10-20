# Monats- und Jahresbetrag

Status: geplant (Änderung von MJ-11 und MJ-14; MJ-1 bis MJ-13 sonst umgesetzt)

## Zweck

Heute ist jeder Posten ein Jahresbetrag (PO-2). Viele Einnahmen und Ausgaben kennt man aber als Monatsbetrag: Gehalt,
Miete, Abonnements, Sparrate. Wer sie erfasst, muss selbst mal 12 rechnen, und in der Übersicht fehlt der Blick auf
den Monat, also darauf, was jeden Monat ein- und ausgeht.

Mit dieser Spec gibt der Benutzer beim Erfassen und Bearbeiten an, ob ein Betrag monatlich oder jährlich gilt. Jeder
Posten hat dann einen Monats- und einen Jahresbetrag, und die Übersicht zeigt beide, für die einzelnen Posten wie für
die Summen.

Diese Spec ändert PO-2 in [posten.md](posten.md) und das Nicht-Ziel „Monatsbeträge" dort sowie das Nicht-Ziel „keine
Monatsansicht" in [produkt.md](../produkt.md): Ein Posten hat weiterhin kein Datum und gilt für das ganze Jahr seines
Plans, lässt sich aber als Monatsbetrag angeben und so anzeigen.

## Fachliche Regeln

### Angabe am Posten

- **MJ-1**: Jeder Posten, Einnahme wie Ausgabe, hat eine Angabe: monatlich oder jährlich. Sie sagt, ob der erfasste
  Betrag ein Monats- oder ein Jahresbetrag ist.
- **MJ-2**: Gespeichert werden der Betrag so, wie der Benutzer ihn eingegeben hat, und seine Angabe. Für den
  eingegebenen Betrag gilt PO-4 unverändert (positiv, höchstens zwei Nachkommastellen).
- **MJ-3**: Alle bestehenden Posten haben die Angabe „jährlich"; ihr Betrag bleibt unverändert.
- **MJ-4**: Die Angabe wird beim Erfassen gewählt und beim Bearbeiten geändert (PB-1). Ändert sich die Angabe oder der
  Betrag, ist das eine Änderung des Postens im Sinne von PB-5 und wird als `BudgetItemChanged` protokolliert. Wird das
  Löschen rückgängig gemacht (PO-9), wird auch die Angabe wiederhergestellt.
- **MJ-5**: Ein Wechsel der Angabe beim Bearbeiten rechnet den Betrag nicht um: Wer „850" von monatlich auf jährlich
  stellt, hat danach einen Jahresbetrag von 850,00 €.

### Monats- und Jahresbetrag

- **MJ-6**: Bei der Angabe „monatlich" ist der Jahresbetrag der Monatsbetrag × 12.
- **MJ-7**: Bei der Angabe „jährlich" ist der Monatsbetrag der Jahresbetrag ÷ 12, kaufmännisch auf ganze Cent gerundet
  (1.000,00 € jährlich ergibt 83,33 € monatlich).
- **MJ-8**: Gerechnet wird immer mit dem Jahresbetrag: Summen, Saldo und die Auswertung nach der 50/30/20-Regel
  (AW-1 bis AW-10) gehen vom Jahresbetrag aus. Der Monatsbetrag ist eine Anzeige und fließt in keine Rechnung ein.
- **MJ-9**: Jede Summe, die die Übersicht zeigt (Einnahmen, Ausgaben, Saldo, Bereiche, Kategorien, Unterkategorie-
  Gruppen nach UK-12), gibt es auch je Monat: die Jahressumme ÷ 12, kaufmännisch auf ganze Cent gerundet. Dadurch
  kann eine Monatssumme um wenige Cent von der Summe der angezeigten Monatsbeträge abweichen; das ist so gewollt.
- **MJ-10**: Monatsbeträge und Monatssummen rechnet das Backend (wie AW-9, UK-13); die Oberfläche rechnet nichts selbst.

### Anzeige

- **MJ-11**: Im Formular „Neuer Eintrag" und im Dialog „Bearbeiten" wählt der Benutzer die Angabe neben dem Betrag mit
  einem Umschalter „Monatlich | Jährlich". Beim Erfassen ist „Monatlich" vorausgewählt; nach dem Erfassen bleibt die
  gewählte Angabe wie die Art (PO-1). Auch der Dialog „Bearbeiten" öffnet immer mit „Monatlich" und zeigt den
  Monatsbetrag des Postens: bei einem monatlichen Posten den eingegebenen Betrag, bei einem jährlichen den Monatsbetrag
  nach MJ-7 (1.000,00 € jährlich erscheint als 83,33 € monatlich). Wer einen jährlichen Posten so speichert, macht ihn
  zum monatlichen Posten; sein Jahresbetrag kann dabei durch die Rundung um wenige Cent abweichen (83,33 € × 12 =
  999,96 €). Wer im Dialog auf „Jährlich" umstellt, rechnet nach MJ-5 nichts um.
- **MJ-12**: Die Karte „Einträge" zeigt bei jedem Posten und bei jeder Summe (Bereich, Kategorie, Gruppe) zwei
  Spalten: „Monat" und „Jahr". Der vom Benutzer eingegebene Betrag ist hervorgehoben, der abgeleitete schwächer
  dargestellt.
- **MJ-13**: Die Kacheln oben in der Übersicht zeigen Einnahmen, Ausgaben und Saldo jeweils je Monat und je Jahr.
- **MJ-14**: Die Karte „50/30/20-Regel" zeigt in beiden Darstellungen, Balken und Torte (TD-1), die Summen der
  Kategorien und „davon nicht verplant" je Monat: die Jahreswerte der Auswertung ÷ 12, kaufmännisch auf ganze Cent
  gerundet (MJ-9). Die Anteile am Einkommen, die Zielerreichung und die Größe der Tortenstücke bleiben unverändert,
  weil sie für Monat und Jahr gleich sind; gerechnet wird weiter mit Jahresbeträgen (MJ-8).

## Akzeptanzkriterien

- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Miete", 850,00 €, „Monatlich", Lebensunterhalt erfasst,
  **dann** zeigt die Karte „Einträge" bei „Miete" 850,00 € je Monat und 10.200,00 € je Jahr, und Lebensunterhalt in
  der Auswertung steigt um 10.200,00 € (MJ-6, MJ-8, MJ-12).
- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Versicherung", 1.000,00 €, „Jährlich" erfasst, **dann**
  zeigt die Karte „Einträge" 83,33 € je Monat und 1.000,00 € je Jahr (MJ-7).
- **Gegeben** Einnahmen „Gehalt" 3.000,00 € monatlich und Ausgaben „Miete" 850,00 € monatlich, **dann** zeigen die
  Kacheln Einnahmen 3.000,00 € / 36.000,00 €, Ausgaben 850,00 € / 10.200,00 € und Saldo 2.150,00 € / 25.800,00 €
  (je Monat / je Jahr, MJ-9, MJ-13).
- **Gegeben** drei Ausgaben von je 100,00 € jährlich in derselben Gruppe, **dann** zeigt jeder Posten 8,33 € je Monat,
  die Gruppe aber 25,00 € je Monat und 300,00 € je Jahr (MJ-9).
- **Gegeben** ein Posten, der vor dieser Änderung erfasst wurde, **dann** hat er die Angabe „jährlich", und sein
  Jahresbetrag ist der bisherige Betrag (MJ-3).
- **Gegeben** eine Ausgabe 850,00 € monatlich, **wenn** der Benutzer „Bearbeiten" wählt, **dann** zeigt der Dialog
  850 und „Monatlich"; nach dem Umstellen auf „Jährlich" und „Speichern" ist der Jahresbetrag 850,00 € und ein Event
  `BudgetItemChanged` wird protokolliert (MJ-4, MJ-5, MJ-11).
- **Gegeben** eine Ausgabe „Versicherung" 1.000,00 € jährlich, **wenn** der Benutzer „Bearbeiten" wählt, **dann** ist
  „Monatlich" gewählt und der Betrag 83,33; nach „Speichern" zeigt die Karte „Einträge" 83,33 € je Monat
  hervorgehoben und 999,96 € je Jahr (MJ-11).
- **Gegeben** eine Ausgabe „Miete" 850,00 € monatlich, **wenn** der Benutzer „Bearbeiten" wählt, **dann** ist
  „Monatlich" gewählt und der Betrag 850 (MJ-11).
- **Gegeben** Einnahmen 3.000,00 € monatlich und Ausgaben Lebensunterhalt 1.500,00 € monatlich, **dann** zeigt die
  Karte „50/30/20-Regel" für Lebensunterhalt 1.500,00 € und 50 %, in den Balken wie in der Legende der Torte, und für
  Sparen & Investieren „davon nicht verplant: 1.500,00 €" (MJ-14).
- **Gegeben** ein gelöschter Posten mit Angabe „monatlich", **wenn** der Benutzer „Rückgängig" wählt, **dann** hat der
  Posten wieder dieselbe Angabe und denselben Betrag (MJ-4).
- **Gegeben** das Formular „Neuer Eintrag", **wenn** es geöffnet wird, **dann** ist „Monatlich" gewählt; nach dem
  Erfassen eines Postens mit „Jährlich" bleibt „Jährlich" gewählt (MJ-11).
- **Gegeben** ein Plan, **wenn** ein Posten mit unbekannter Angabe erfasst oder geändert werden soll, **dann**
  antwortet die API mit 400 (MJ-1).

## Nicht im Umfang

- Weitere Angaben wie wöchentlich, vierteljährlich oder halbjährlich
- Beträge für einzelne Monate, Posten, die nur in bestimmten Monaten anfallen, eine Ansicht je Kalendermonat
- Jahresbeträge in der Karte „50/30/20-Regel" neben den Monatsbeträgen (MJ-14); die Jahreswerte stehen in den Kacheln
  und in der Karte „Einträge"
- Ein Schalter, der die ganze Übersicht nur monatlich oder nur jährlich zeigt

## Umsetzung

- Domäne: Value Object `ItemAmount` (eingegebener Betrag `value` und `AmountPeriod` `MONTHLY`/`YEARLY`, MJ-1, MJ-2)
  mit `annual()` (MJ-6) und `monthly()` (MJ-7); `Money.timesMonthsPerYear` und `Money.perMonth` rechnen mal 12 bzw.
  durch 12, kaufmännisch auf den Cent gerundet. `BudgetItem` hält statt `Money` ein `ItemAmount`; `addTo` gibt der
  `BudgetSummary` den Jahresbetrag (MJ-8). `hasSameValuesAs` vergleicht auch die Angabe, sodass ein Wechsel als
  `BudgetItemChanged` protokolliert wird (MJ-4); `changedTo` übernimmt den Betrag unverändert (MJ-5); `copy` übernimmt ihn samt Angabe in einen anderen Plan
  ([PU-3](plan-uebernehmen.md))
- Commands: `AddIncomeCommand`, `AddExpenseCommand`, `ChangeBudgetItemCommand` mit `ItemAmount`
- Persistenz: Migration `V8__budget_item_amount_period.sql` mit der Spalte `budget_item.amount_period`, Vorgabe
  `YEARLY` für bestehende Posten (MJ-3); `amount` bleibt der eingegebene Betrag
- Read Model: `OverviewItem` mit `ItemAmount`; `HouseholdPlanOverview` summiert Jahresbeträge und liefert
  `monthlyTotalIncome`, `monthlyTotalExpense`, `monthlyBalance`, `OverviewItemGroup` und `OverviewCategoryItems`
  `monthlyTotal`, jeweils die Jahressumme durch 12 (MJ-9, MJ-10). `CategoryBreakdown` bleibt bei Jahreswerten (MJ-14),
  ebenso die Jahresübersicht ([JU-2](jahresuebersicht.md)): `JdbcHouseholdPlanOverviewQueryHandler` summiert dort je
  Angabe und rechnet monatliche Summen mal 12
- API: Pflichtfeld `period` (`MONTHLY`, `YEARLY`) in `AddIncomeRequest`, `AddExpenseRequest`,
  `ChangeBudgetItemRequest`, sonst 400 (MJ-1); `BudgetItemResponse` mit `amount` (wie eingegeben), `period`,
  `monthlyAmount`, `annualAmount`; Monatssummen in `HouseholdPlanOverviewResponse`, `ItemGroupResponse`,
  `CategoryItemsResponse`
- Frontend: Umschalter „Monatlich | Jährlich" (`AMOUNT_PERIODS` in `budget.model.ts`) neben dem Betrag in
  `add-item-form` (Vorauswahl „Monatlich", bleibt nach dem Erfassen) und `edit-item-dialog` (MJ-11); Spalten „Monat"
  und „Jahr" in `budget-entries`, der abgeleitete Betrag mit der Klasse `derived` schwächer (MJ-12); Kacheln in
  `overview-summary` je Monat und Jahr (MJ-13); `BudgetStore.restoreItem` stellt die Angabe wieder her (MJ-4)
- Tests: `ItemAmountTest`, `MoneyTest`, `BudgetItemTest`, `HouseholdPlanTest`, `HouseholdPlanCopyTest`, `HouseholdPlanOverviewTest`,
  `HouseholdPlanOverviewGroupsTest`, `HouseholdPlanControllerTest`, `JdbcHouseholdPlanRepositoryTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `add-item-form.spec.ts`, `edit-item-dialog.spec.ts`,
  `budget-entries.spec.ts`, `overview-summary.spec.ts`
- E2E: `e2e/tests/monats-jahresbetrag.spec.ts` (MJ-1, MJ-4 bis MJ-9, MJ-11 bis MJ-13; MJ-3 und die 400-Fälle decken die
  Backend-Tests ab). Die Helfer `addIncome`/`addExpense` erfassen ohne Angabe jährlich, damit die übrigen E2E-Tests
  ihre Jahresbeträge behalten; `tile` liest den Jahreswert

## Offene Fragen

- Gilt MJ-14 für Balken und Torte? Vorschlag: ja, beide Darstellungen derselben Karte zeigen Monatswerte, damit beim
  Umschalten nicht andere Beträge erscheinen. Gewünscht war ausdrücklich die Torte.
- Zeigt die Karte „50/30/20-Regel" nur Monatswerte oder Monat und Jahr? Vorschlag: nur Monatswerte, mit dem Hinweis
  „je Monat" in der Überschrift der Legende bzw. der Balken.
