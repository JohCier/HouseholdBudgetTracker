---
name: spec-umsetzen
description: Eine freigegebene Spec aus docs/specs/ im Haushaltsbuch umsetzen, von der Domäne bis zum Playwright-Test. Verwenden, wenn eine Spec mit Status „geplant" implementiert werden soll oder „setze alle geplanten Specs um" verlangt ist.
---

# Spec umsetzen

Voraussetzung: Die Spec ist freigegeben (siehe „Ablauf für neue Features" in `CLAUDE.md`). Eine Spec, ein Branch, ein PR.

## 1. Lesen, nicht suchen

- Die Spec selbst, dazu nur die Specs, auf die sie verweist. Welche Spec ein Regel-Präfix nutzt, steht in der Tabelle in
  `docs/produkt.md`.
- Neue Begriffe gleich in `docs/glossar.md` eintragen (deutscher Begriff, Name im Code).
- Ähnliche Umsetzung als Vorlage: der Abschnitt „Umsetzung" einer verwandten Spec listet Klassen und Tests.

## 2. Backend (`apps/backend`, bei Anmeldung `apps/users`)

Reihenfolge von innen nach außen; die DDD-Regeln lädt Claude Code automatisch mit der ersten Java-Datei.

| Schicht | Typische Dateien |
|---|---|
| Domäne | Methode in `domain/householdplan/` (`HouseholdPlan`, `BudgetItem`), Value Object in `valueobject/`, Command in `command/`, Event in `event/`, Read Model in `overview/` |
| Tests Domäne | `HouseholdPlan<Thema>Test` unter `src/test/.../domain/householdplan/`, `<ValueObject>Test` unter `…/householdplan/valueobject/` |
| Application | `HouseholdPlanApplicationService` (laden, Fachmethode, `save`, Events) und `HouseholdPlanApplicationServiceTest` |
| Persistenz | Flyway-Migration `src/main/resources/db/migration/V<n+1>__<thema>.sql` (nächste freie Nummer), `JdbcHouseholdPlanRepository`, `JdbcHouseholdPlanOverviewQueryHandler` und ihre Tests (`@IntegrationTest`) |
| Web | `HouseholdPlanController`, DTOs in `infrastructure/web/dto/`; Test in der passenden Klasse unter `infrastructure/web/` (`HouseholdPlanControllerTest`, `HouseholdPlanOverviewControllerTest`, `BudgetItem*ControllerTest`) |
| Ende zu Ende | `HouseholdPlanIntegrationTest` (Pläne) bzw. `BudgetItemIntegrationTest` (Posten) |

Ändert sich Controller oder DTO: Skill `api-vertrag`.

## 3. Frontend (`apps/frontend/src/app/budget/`)

- `data-access/budget-api.ts` (HTTP), `data-access/budget-store.ts` (Signal-Store) mit `.spec.ts`
- Komponenten in `ui/`, Seiten `budget-dashboard.ts`, `yearly-overview-page.ts`; Texte auf Deutsch
- Typen nur aus `@hbt/shared-types`, nie von Hand nachbauen

## 4. Playwright (`e2e/tests/<spec>.spec.ts`)

- Jedes Akzeptanzkriterium mit sichtbarem Verhalten bekommt einen Test, Regel-ID im Namen: `test('… (PM-4)', …)`
- Hilfsfunktionen (`registerWithPlan`, `addIncome`, `addExpense`, `itemRow`, `balanceSheet` …) aus `e2e/tests/support.ts` verwenden und dort
  ergänzen, statt sie zu kopieren
- Lauf: Stack mit `docker compose --profile app up --build --wait`, dann `npx playwright test tests/<spec>.spec.ts` in
  `e2e`

## 5. Prüfen und abschließen

- Während der Arbeit nur betroffene Tests (Befehle in `CLAUDE.md`), vor dem Commit: `./mvnw -q verify` in jeder
  geänderten Java-App, `npm test -s --workspace=frontend`, `npm run build -s --workspace=frontend`, die E2E-Datei
- Spec: Status „umgesetzt", Abschnitt „Umsetzung" mit Klassen, Tests und E2E-Abdeckung (knapp, Dateiliste statt
  Fließtext); neue Zeile in der Tabelle in `docs/produkt.md` mit Regel-Präfix
- Commit und PR auf Deutsch, Titel mit Regel-IDs, z. B. `Posten markieren (PM-1 bis PM-14)`
