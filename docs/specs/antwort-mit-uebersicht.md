# Übersicht als Antwort auf Änderungen

Status: umgesetzt

## Zweck

Nach jeder Änderung an einem Plan (Plan anlegen, Posten erfassen, ändern oder löschen) lädt die Oberfläche heute die
komplette Übersicht mit einer zweiten Anfrage neu. Jeder Klick kostet so zwei Anfragen und die doppelte Wartezeit.
Mit dieser Spec liefert die API die Übersicht nach der Änderung direkt in ihrer Antwort mit, und die Oberfläche zeigt
sie ohne weitere Anfrage an. Für den Benutzer ändert sich nur, dass die Anzeige schneller aktualisiert wird.

## Fachliche Regeln

- **AU-1**: Jede erfolgreiche Änderung an einem Plan beantwortet die API mit der Übersicht dieses Plans nach der
  Änderung. Die Übersicht hat dieselbe Form wie bei `GET /api/plans?year=` (Posten, Summen, Aufteilung je Kategorie).
  Das gilt für: Plan anlegen (HP-5), Einnahme oder Ausgabe erfassen (PO-1), Posten ändern (PB-1) und Posten löschen
  (PO-6).
- **AU-2**: Die mitgelieferte Übersicht enthält genau den Stand nach dieser Änderung, einschließlich der Änderung
  selbst. Sie ist mit dem identisch, was ein anschließender Abruf der Übersicht liefern würde, solange niemand sonst
  den Plan ändert.
- **AU-3**: Antworten auf das Erfassen eines Postens und das Anlegen eines Plans nennen weiterhin die ID des neuen
  Postens bzw. Plans, neben der Übersicht.
- **AU-4**: Statuscodes: Plan anlegen und Posten erfassen antworten weiter mit 201, Posten ändern und löschen mit 200
  statt bisher 204, weil die Antwort jetzt einen Inhalt hat. Fehlerantworten (400, 404, 409) bleiben unverändert.
- **AU-5**: Die Oberfläche zeigt nach einer Änderung die mitgelieferte Übersicht an und fragt sie nicht erneut ab.
  Gehört die Antwort zu einem anderen Jahr als dem gerade gewählten (z. B. Rückgängig nach einem Jahreswechsel, PO-9),
  zeigt sie die Übersicht nicht an; das gewählte Jahr bleibt wie es ist (HP-1).
- **AU-6**: Wird gerade die Übersicht des gewählten Jahres geladen, während eine Änderung antwortet, gewinnt die
  jüngere Antwort, damit keine veralteten Daten stehen bleiben.

## Akzeptanzkriterien

- **Gegeben** ein angemeldeter Benutzer ohne Plan für 2026, **wenn** er einen Plan für 2026 anlegt, **dann** antwortet
  die API mit 201, der ID des Plans und der leeren Übersicht für 2026 (AU-1, AU-3).
- **Gegeben** ein Plan, **wenn** der Benutzer die Ausgabe „Miete", 850,00 €, Lebensunterhalt erfasst, **dann**
  antwortet die API mit 201, der ID des Postens und einer Übersicht, die „Miete" enthält und Ausgaben von 850,00 € zeigt
  (AU-1, AU-2, AU-3).
- **Gegeben** ein Posten, **wenn** er geändert wird, **dann** antwortet die API mit 200 und der Übersicht mit den neuen
  Werten (AU-1, AU-4).
- **Gegeben** ein Posten, **wenn** er gelöscht wird, **dann** antwortet die API mit 200 und der Übersicht ohne diesen
  Posten (AU-1, AU-4).
- **Gegeben** die Oberfläche zeigt einen Plan, **wenn** der Benutzer einen Posten erfasst, ändert oder löscht, **dann**
  zeigt sie die neue Übersicht an, ohne `GET /api/plans` erneut aufzurufen (AU-5).
- **Gegeben** ein gelöschter Posten aus 2026 und die Oberfläche zeigt inzwischen 2027, **wenn** der Benutzer
  „Rückgängig" wählt, **dann** bleibt 2027 angezeigt, und der Posten ist in 2026 wieder vorhanden (AU-5, PO-9).

## Nicht im Umfang

- Optimistische Anzeige vor der Antwort des Servers
- Änderungen anderer Sitzungen oder Tabs live übernehmen (der Stand kommt weiter erst mit der nächsten eigenen Änderung
  oder dem nächsten Abruf)
- Änderungen an `GET /api/plans?year=` selbst

## Umsetzung

- Application: `HouseholdPlanApplicationService` liest nach jedem Command die Übersicht in derselben Transaktion über
  den `HouseholdPlanOverviewQueryHandler` (AU-1, AU-2); `createPlan` gibt `CreatedHouseholdPlan`, `addIncome` und
  `addExpense` geben `AddedBudgetItem` zurück (ID und Übersicht, AU-3), `changeItem` und `removeItem` die Übersicht
- API: `POST /api/plans` antwortet mit `CreatedHouseholdPlanResponse`, `POST …/incomes` und `…/expenses` mit
  `AddedBudgetItemResponse` (jeweils `{ id, overview }`, 201); `PUT` und `DELETE …/items/{itemId}` antworten mit 200
  und `HouseholdPlanOverviewResponse` (AU-4); API-Vertrag `packages/shared-types/openapi/plans.json`
- Frontend: `BudgetStore` zeigt die Übersicht aus der Antwort an, statt neu zu laden; gehört sie nicht zum gewählten
  Jahr, bleibt die Anzeige unverändert (AU-5); eine laufende Ladeanfrage wird verworfen (AU-6)
- Angepasste Akzeptanzkriterien: [haushaltsplan.md](haushaltsplan.md) (HP-5), [posten.md](posten.md) (PO-1, PO-6),
  [posten-bearbeiten.md](posten-bearbeiten.md) (PB-1)
- Tests: `HouseholdPlanApplicationServiceTest`, `HouseholdPlanControllerTest`, `HouseholdPlanIntegrationTest`,
  `budget-api.spec.ts`, `budget-store.spec.ts`
- E2E: `e2e/tests/antwort-mit-uebersicht.spec.ts` (AU-5 mit PO-9; AU-1 bis AU-4 decken die Backend-Tests ab)

## Offene Fragen
