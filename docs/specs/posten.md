# Posten (Einnahmen und Ausgaben)

Status: umgesetzt

## Zweck

Posten sind die einzelnen geplanten Einnahmen und Ausgaben eines Haushaltsplans, z. B. „Gehalt" oder „Miete". Aus
ihnen ergibt sich die Auswertung nach der 50/30/20-Regel.

## Fachliche Regeln

- **PO-1**: Ein Posten ist entweder eine Einnahme oder eine Ausgabe.
- **PO-2**: Ein Posten hat kein eigenes Datum. Er gilt für das Jahr seines Plans. Sein Betrag wird monatlich oder
  jährlich angegeben, siehe [monats-jahresbetrag.md](monats-jahresbetrag.md).
- **PO-3**: Jeder Posten hat eine Beschreibung mit 1 bis 255 Zeichen, die nicht nur aus Leerzeichen besteht.
- **PO-4**: Der Betrag ist positiv, in Euro, und hat höchstens zwei Nachkommastellen. Mehr Nachkommastellen werden
  abgelehnt, nicht gerundet.
- **PO-5**: Eine Ausgabe hat genau eine Kategorie (Lebensunterhalt, Freizeit & Spaß, Sparen & Investieren). Eine
  Einnahme hat nie eine Kategorie.
- **PO-6**: Posten werden nur über ihren Plan hinzugefügt, geändert oder gelöscht und nur vom Besitzer des Plans
  (siehe [HP-4](haushaltsplan.md)).
- **PO-7**: Entfallen. Posten lassen sich bearbeiten, siehe [posten-bearbeiten.md](posten-bearbeiten.md).
- **PO-8**: Das Löschen eines Postens, der nicht (mehr) zum Plan gehört, wird abgelehnt.
- **PO-9**: Nach dem Löschen kann der Benutzer das Löschen kurz rückgängig machen. Der Posten wird dann mit gleicher
  Beschreibung, gleichem Betrag und gleicher Kategorie, aber neuer ID wieder angelegt, auch wenn inzwischen ein anderes
  Jahr angezeigt wird.
- **PO-10**: Ein neu erfasster Posten steht unten in seiner Unterkategorie-Gruppe, deren Reihenfolge der Benutzer frei
  ändern kann ([EG-1](eintraege-gruppen.md), UK-14). Bis zu [eintraege-gruppen.md](eintraege-gruppen.md) stand er oben.

## Akzeptanzkriterien

- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Miete", 850,00 €, Lebensunterhalt erfasst, **dann**
  antwortet die API mit 201, der ID des Postens und der Übersicht, die den Posten enthält (AU-1, AU-3).
- **Gegeben** ein Plan, **wenn** der Benutzer einen Posten erfolgreich erfasst hat, **dann** ist das Formular wieder
  leer und zeigt keine Fehlermeldungen; die gewählte Art bleibt (PO-1).
- **Gegeben** ein Plan, **wenn** eine Ausgabe ohne Kategorie erfasst wird, **dann** antwortet die API mit 400.
- **Gegeben** ein Plan, **wenn** ein Betrag von 0, ein negativer Betrag oder 10,005 erfasst wird, **dann** antwortet die
  API mit 400, und die Oberfläche zeigt „Bitte einen positiven Betrag angeben." bzw. „Höchstens zwei
  Nachkommastellen.", ohne die Anfrage zu senden.
- **Gegeben** ein Posten im Plan, **wenn** der Benutzer ihn löscht, **dann** antwortet die API mit 200 und der Übersicht
  ohne den Posten (AU-4), und die Oberfläche bietet für einige Sekunden „Rückgängig" an.
- **Gegeben** eine Posten-ID, die nicht zum Plan gehört, **wenn** sie gelöscht werden soll, **dann** antwortet die API
  mit 404.

## Nicht im Umfang

- Posten bearbeiten (eigene Spec: [posten-bearbeiten.md](posten-bearbeiten.md))
- Posten mit Datum, wiederkehrende Posten (Monatsbeträge: [monats-jahresbetrag.md](monats-jahresbetrag.md))
- Eigene Kategorien der 50/30/20-Regel (Unterkategorien: [unterkategorie.md](unterkategorie.md))
- Andere Währungen als Euro
- Eine fachliche Obergrenze für Beträge. Technisch begrenzt bleibt der Betrag auf zehn Vorkommastellen (Spalte
  `amount`); das ist keine fachliche Regel.

## Umsetzung

- Domäne: `HouseholdPlan.addIncome`, `addExpense`, `removeItem`; `BudgetItem`, `BudgetItemDescription`, `Money`,
  `ItemType`, `ExpenseCategory`
- Commands: `AddIncomeCommand`, `AddExpenseCommand`, `RemoveBudgetItemCommand`
- Events: `IncomeAdded`, `ExpenseAdded`, `BudgetItemRemoved`
- API: `POST /api/plans/{planId}/incomes`, `POST /api/plans/{planId}/expenses`,
  `DELETE /api/plans/{planId}/items/{itemId}`; Fehler als RFC 9457 Problem Details
- Frontend: `budget/ui/add-item-form.ts`, `budget/ui/budget-item-table.ts`, Rückgängig in `budget-dashboard.ts`
  und `BudgetStore.restoreItem`
- Tests: `BudgetItemTest`, `BudgetItemDescriptionTest`, `MoneyTest`, `HouseholdPlanTest`,
  `BudgetItemControllerTest`, `add-item-form.spec.ts`, `budget-item-table.spec.ts`, `budget-store.spec.ts`
- E2E: `e2e/tests/posten.spec.ts`

## Offene Fragen
