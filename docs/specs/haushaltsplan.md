# Haushaltsplan

Status: umgesetzt

## Zweck

Ein Haushaltsplan bündelt alles, was ein Benutzer für ein Kalenderjahr an Einnahmen und Ausgaben plant. Er ist die
Einheit, die ausgewertet wird, und gehört genau einem Benutzer.

## Fachliche Regeln

- **HP-1**: Ein Plan gilt für genau ein Kalenderjahr von 1900 bis einschließlich 2200.
  Pläne für andere Jahre werden weder angelegt noch abgerufen. Bereits gespeicherte Pläne außerhalb dieser Jahre
  bleiben in der Datenbank, sind aber nicht mehr erreichbar.
- **HP-2**: Ein Benutzer hat höchstens einen aktiven Plan pro Jahr; gelöschte Pläne zählen nicht
  ([GP-1](geloeschte-plaene.md)). Ein zweiter Plan für dasselbe Jahr wird abgelehnt; wird ein Plan in ein Jahr
  übernommen, das schon einen hat, wird dieser nur nach Bestätigung überschrieben (PU-9 in
  [plan-uebernehmen.md](plan-uebernehmen.md)).
- **HP-3**: Ein Plan gehört dem Benutzer, der ihn angelegt hat (Besitzer). Der Besitzer ändert sich nie.
- **HP-4**: Nur der Besitzer sieht und ändert seinen Plan. Pläne anderer Benutzer werden behandelt, als gäbe es sie
  nicht, damit ihre Existenz nicht verraten wird.
- **HP-5**: Ein neuer Plan ist leer. Posten kommen erst danach hinzu (siehe [posten.md](posten.md)), außer der Plan
  entsteht durch Übernahme eines anderen Plans (PU-8 in [plan-uebernehmen.md](plan-uebernehmen.md)).
- **HP-6**: Gleichzeitige Änderungen am selben Plan werden nacheinander ausgeführt, keine geht verloren.
- **HP-7**: Jede Änderung wird als Domain Event protokolliert, aber nur, wenn sie erfolgreich gespeichert wurde.

## Akzeptanzkriterien

- **Gegeben** ein angemeldeter Benutzer ohne Plan für 2026, **wenn** er einen Plan für 2026 anlegt, **dann** antwortet
  die API mit 201, der ID des Plans und der Übersicht für 2026, die einen leeren Plan zeigt (AU-1, AU-3).
- **Gegeben** ein Benutzer mit Plan für 2026, **wenn** er erneut einen Plan für 2026 anlegt, **dann** antwortet die API
  mit 409.
- **Gegeben** ein Benutzer ohne Plan für 2027, **wenn** er die Übersicht für 2027 abruft, **dann** antwortet die API mit
  404, und die Oberfläche bietet „Haushaltsplan anlegen" an.
- **Gegeben** Benutzer A mit einem Plan, **wenn** Benutzer B diesen Plan über seine ID ändern will, **dann** antwortet
  die API mit 404 (nicht 403).
- **Gegeben** die Planansicht eines Jahres (`/jahre/<Jahr>`, siehe JU-11), **dann** zeigt sie dieses Jahr; mit den
  Pfeilen wechselt der Benutzer zum vorherigen bzw. nächsten Jahr. Nach der Anmeldung zeigt die Oberfläche die
  Jahresübersicht ([jahresuebersicht.md](jahresuebersicht.md)).
- **Gegeben** die Oberfläche zeigt einen Plan, **wenn** der Benutzer das Jahr wechselt, **dann** zeigt sie bis zum Laden
  des neuen Jahres und auch bei einem Ladefehler nichts mehr vom vorherigen Jahr, und es lässt sich nichts darin
  ändern (HP-1).
- **Gegeben** ein Plan für 1899 oder 2201, **wenn** er angelegt oder abgerufen werden soll, **dann** antwortet die API mit
  400 (HP-1).
- **Gegeben** die Oberfläche zeigt 1900 bzw. 2200, **dann** ist der Pfeil zum vorherigen bzw. nächsten Jahr deaktiviert
  (HP-1).

## Nicht im Umfang

- Plan auf ein anderes Jahr verschieben; löschen siehe [plan-loeschen.md](plan-loeschen.md)
- Plan mit anderen Benutzern teilen

## Umsetzung

- Domäne: `HouseholdPlan` (`create`, `isOwnedBy`), `OwnerId`, `HouseholdPlanId`
- HP-1: `HouseholdPlan.create` lehnt Jahre außerhalb von `FIRST_YEAR`/`LAST_YEAR` mit `DomainValidationException` ab
  (400), `reconstitute` nicht; `CreateHouseholdPlanRequest` und `GET /api/plans?year=` prüfen dieselben Grenzen.
  Frontend: `FIRST_YEAR`/`LAST_YEAR` in `budget/year.ts`, `BudgetStore.hasPreviousYear`/`hasNextYear` deaktivieren
  die Pfeile; `BudgetStore.selectYear` verwirft die Übersicht des vorherigen Jahres sofort
- HP-2: `HouseholdPlanApplicationService.createPlan` prüft vorab, die Unique-Constraint in
  `V4__household_plan_owner.sql` sichert gegen gleichzeitige Anfragen
- HP-4: `HouseholdPlanApplicationService.load` filtert über `isOwnedBy` und wirft `HouseholdPlanNotFoundException`
- HP-6: `JdbcHouseholdPlanRepository.findById` mit `SELECT … FOR UPDATE`
- HP-7: `AuditLogListener` (`@TransactionalEventListener(AFTER_COMMIT)`)
- API: `POST /api/plans`, `GET /api/plans?year=`
- Frontend: `budget/budget-dashboard.ts`, `budget/data-access/budget-store.ts`, `budget/year.ts`
- Tests: `HouseholdPlanTest`, `HouseholdPlanApplicationServiceTest`, `HouseholdPlanControllerTest`, `HouseholdPlanOverviewControllerTest`,
  `HouseholdPlanIntegrationTest`, `JdbcHouseholdPlanRepositoryTest`, `budget-dashboard.spec.ts`,
  `budget-store.spec.ts`, `year.spec.ts`
- E2E: `e2e/tests/haushaltsplan.spec.ts`

## Offene Fragen

