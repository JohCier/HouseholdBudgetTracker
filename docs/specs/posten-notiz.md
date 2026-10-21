# Notiz zum Posten

Status: umgesetzt

## Zweck

Die Beschreibung eines Postens sagt, wofür er steht („Miete"), aber nicht, was dahintersteckt: wie sich ein Betrag
zusammensetzt, wann ein Vertrag ausläuft oder warum ein Betrag geschätzt ist. Mit dieser Spec kann der Besitzer eines
Plans zu jedem Posten eine optionale Notiz festhalten und später wiederfinden.

## Fachliche Regeln

- **PN-1**: Jeder Posten, Einnahme wie Ausgabe, kann eine Notiz haben. Die Notiz ist optional.
- **PN-2**: Eine Notiz ist Freitext mit höchstens 1000 Zeichen; Zeilenumbrüche sind erlaubt. Eine längere Notiz wird
  abgelehnt, nicht gekürzt.
- **PN-3**: Eine Notiz, die leer ist oder nur aus Leerzeichen besteht, gilt als „keine Notiz". Führende und
  abschließende Leerzeichen werden entfernt.
- **PN-4**: Die Notiz wird beim Erfassen eines Postens angegeben und beim Bearbeiten geändert oder entfernt (siehe
  [PB-1](posten-bearbeiten.md)). Wie die übrigen Werte eines Postens ändert sie nur der Besitzer des Plans (PO-6).
- **PN-5**: Eine neue, geänderte oder entfernte Notiz ist eine Änderung des Postens im Sinne von PB-5 und wird als
  `BudgetItemChanged` protokolliert. Der Text der Notiz steht nicht im Protokoll.
- **PN-6**: Die Notiz hat keinen Einfluss auf die Auswertung nach der 50/30/20-Regel.
- **PN-7**: Die Tabelle der Posten zeigt bei Posten mit Notiz die Notiz unter der Beschreibung an.
- **PN-8**: Wird das Löschen eines Postens rückgängig gemacht (PO-9), wird auch seine Notiz wiederhergestellt.

## Akzeptanzkriterien

- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Miete", 850,00 €, Lebensunterhalt mit der Notiz
  „Warmmiete inkl. Nebenkosten" erfasst, **dann** zeigt die Tabelle die Notiz unter „Miete" an.
- **Gegeben** ein Plan, **wenn** ein Posten ohne Notiz erfasst wird, **dann** antwortet die API mit 201, und die Tabelle
  zeigt beim Posten keine Notiz.
- **Gegeben** ein Posten, **wenn** der Benutzer „Bearbeiten" wählt, **dann** zeigt der Dialog die bisherige Notiz; nach
  dem Ändern und „Speichern" zeigt die Tabelle die neue Notiz, nach dem Leeren des Feldes keine Notiz mehr.
- **Gegeben** ein Posten, **wenn** eine Notiz mit mehr als 1000 Zeichen erfasst oder gespeichert werden soll, **dann**
  antwortet die API mit 400, und die Oberfläche zeigt „Höchstens 1000 Zeichen.", ohne die Anfrage zu senden.
- **Gegeben** ein Posten, **wenn** nur die Notiz geändert wird, **dann** wird ein Event `BudgetItemChanged`
  protokolliert, ohne den Text der Notiz.
- **Gegeben** ein gelöschter Posten mit Notiz, **wenn** der Benutzer „Rückgängig" wählt, **dann** erscheint der Posten
  wieder mit seiner Notiz.

## Nicht im Umfang

- Mehrere Notizen pro Posten, Notizen zum ganzen Plan
- Formatierung (Fettschrift, Links, Listen), Anhänge
- Suche oder Filter nach Notizen
- Verlauf früherer Notizen

## Umsetzung

- Domäne: Value Object `BudgetItemNote` (PN-2, PN-3; `ofNullable` macht aus leerem Text „keine Notiz"), optionales
  Feld `note` in `BudgetItem`; `HouseholdPlan.addIncome`, `addExpense` und `changeItem` nehmen die Notiz entgegen
  (PN-1, PN-4), `BudgetItem.hasSameValuesAs` vergleicht sie mit (PN-5). Die Events tragen weiterhin nur IDs, der Text
  der Notiz landet nicht im Protokoll (PN-5). `BudgetSummary` kennt die Notiz nicht (PN-6)
- Commands: `AddIncomeCommand`, `AddExpenseCommand`, `ChangeBudgetItemCommand` mit optionaler Notiz
- Persistenz: Spalte `budget_item.note VARCHAR(1000)` (Migration `V6__budget_item_note.sql`), geschrieben von
  `JdbcHouseholdPlanRepository`, gelesen auch von `JdbcHouseholdPlanOverviewQueryHandler` (Read Model `OverviewItem`)
- API: optionales Feld `note` (höchstens 1000 Zeichen, sonst 400) in `AddIncomeRequest`, `AddExpenseRequest` und
  `ChangeBudgetItemRequest`; ein fehlendes oder leeres `note` beim Ändern entfernt die Notiz. `BudgetItemResponse`
  liefert `note` oder `null`
- Frontend: Feld „Notiz (optional)" mit Zeichenzähler in `budget/ui/add-item-form.html` und
  `budget/ui/edit-item-dialog.ts`, Prüfung und Aufbereitung in `budget/ui/item-validators.ts` (`NOTE_MAX_LENGTH`,
  `noteOf`); `budget/ui/budget-item-table.ts` zeigt die Notiz vollständig in kleiner grauer Schrift unter der
  Beschreibung (PN-7); `BudgetStore.restoreItem` stellt sie wieder her (PN-8)
- Tests: `BudgetItemNoteTest`, `HouseholdPlanTest`, `BudgetItemControllerTest`, `JdbcHouseholdPlanRepositoryTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `add-item-form.spec.ts`, `edit-item-dialog.spec.ts`,
  `budget-item-table.spec.ts`, `budget-store.spec.ts`
- E2E: `e2e/tests/posten-notiz.spec.ts` (PN-1 bis PN-4, PN-7, PN-8; PN-5 und PN-6 decken die Backend-Tests ab)

## Offene Fragen
