# Haushaltsplan

Status: geplant

HP-2 bis HP-7 sind umgesetzt; geplant ist die neue Jahresgrenze in HP-1 (1900 bis 2200 statt 1 bis 9999).

## Zweck

Ein Haushaltsplan bündelt alles, was ein Benutzer für ein Kalenderjahr an Einnahmen und Ausgaben plant. Er ist die
Einheit, die ausgewertet wird, und gehört genau einem Benutzer.

## Fachliche Regeln

- **HP-1**: Ein Plan gilt für genau ein Kalenderjahr von 1900 bis einschließlich 2200 (geplant; heute 1 bis 9999).
  Pläne für andere Jahre werden weder angelegt noch abgerufen. Bereits gespeicherte Pläne außerhalb dieser Jahre
  bleiben in der Datenbank, sind aber nicht mehr erreichbar.
- **HP-2**: Ein Benutzer hat höchstens einen Plan pro Jahr. Ein zweiter Plan für dasselbe Jahr wird abgelehnt.
- **HP-3**: Ein Plan gehört dem Benutzer, der ihn angelegt hat (Besitzer). Der Besitzer ändert sich nie.
- **HP-4**: Nur der Besitzer sieht und ändert seinen Plan. Pläne anderer Benutzer werden behandelt, als gäbe es sie
  nicht, damit ihre Existenz nicht verraten wird.
- **HP-5**: Ein neuer Plan ist leer. Posten kommen erst danach hinzu (siehe [posten.md](posten.md)).
- **HP-6**: Gleichzeitige Änderungen am selben Plan werden nacheinander ausgeführt, keine geht verloren.
- **HP-7**: Jede Änderung wird als Domain Event protokolliert, aber nur, wenn sie erfolgreich gespeichert wurde.

## Akzeptanzkriterien

- **Gegeben** ein angemeldeter Benutzer ohne Plan für 2026, **wenn** er einen Plan für 2026 anlegt, **dann** antwortet
  die API mit 201 und der ID des Plans, und die Übersicht für 2026 zeigt einen leeren Plan.
- **Gegeben** ein Benutzer mit Plan für 2026, **wenn** er erneut einen Plan für 2026 anlegt, **dann** antwortet die API
  mit 409.
- **Gegeben** ein Benutzer ohne Plan für 2027, **wenn** er die Übersicht für 2027 abruft, **dann** antwortet die API mit
  404, und die Oberfläche bietet „Haushaltsplan anlegen" an.
- **Gegeben** Benutzer A mit einem Plan, **wenn** Benutzer B diesen Plan über seine ID ändern will, **dann** antwortet
  die API mit 404 (nicht 403).
- **Gegeben** die Oberfläche, **wenn** sie geöffnet wird, **dann** zeigt sie das aktuelle Jahr (Zeitzone des Browsers);
  mit den Pfeilen wechselt der Benutzer zum vorherigen bzw. nächsten Jahr.
- **Gegeben** ein Plan für 1899 oder 2201, **wenn** er angelegt oder abgerufen werden soll, **dann** antwortet die API mit
  400 (HP-1).
- **Gegeben** die Oberfläche zeigt 1900 bzw. 2200, **dann** ist der Pfeil zum vorherigen bzw. nächsten Jahr deaktiviert
  (HP-1).

## Nicht im Umfang

- Plan löschen oder auf ein anderes Jahr verschieben
- Plan mit anderen Benutzern teilen
- Plan aus dem Vorjahr kopieren

## Umsetzung

- Domäne: `HouseholdPlan` (`create`, `isOwnedBy`), `OwnerId`, `HouseholdPlanId`
- HP-2: `HouseholdPlanApplicationService.createPlan` prüft vorab, die Unique-Constraint in
  `V4__household_plan_owner.sql` sichert gegen gleichzeitige Anfragen
- HP-4: `HouseholdPlanApplicationService.load` filtert über `isOwnedBy` und wirft `HouseholdPlanNotFoundException`
- HP-6: `JdbcHouseholdPlanRepository.findById` mit `SELECT … FOR UPDATE`
- HP-7: `AuditLogListener` (`@TransactionalEventListener(AFTER_COMMIT)`)
- API: `POST /api/plans`, `GET /api/plans?year=`
- Frontend: `budget/budget-dashboard.ts`, `budget/data-access/budget-store.ts`, `budget/year.ts`
- Tests: `HouseholdPlanTest`, `HouseholdPlanApplicationServiceTest`, `HouseholdPlanControllerTest`,
  `HouseholdPlanIntegrationTest`, `JdbcHouseholdPlanRepositoryTest`, `budget-dashboard.spec.ts`,
  `budget-store.spec.ts`
- E2E: `e2e/tests/haushaltsplan.spec.ts`

## Offene Fragen

