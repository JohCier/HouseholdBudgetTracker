# Household Budget Tracker

Haushaltsbuch nach der 50/30/20-Regel: höchstens 50 % des Jahreseinkommens für den Lebensunterhalt,
höchstens 30 % für Freizeit & Spaß, mindestens 20 % für Sparen & Investieren.

| Ordner      | Inhalt                                                                                   |
|-------------|------------------------------------------------------------------------------------------|
| `backend/`  | Spring Boot (Java 25, Maven) – DDD mit getrennter Schreib-/Leseseite, REST-API unter `/api/plans` |
| `frontend/` | Angular 22 + Angular Material                                                            |
| `api/`      | `openapi.json` – REST-Vertrag zwischen Backend und Frontend                               |

## Architektur

```
de.jcss.hbt
├── domain                  reines Java, ohne Frameworks
│   ├── shared              DDD-Bausteine: @AggregateRoot, @DomainEntity, @ValueObject, @ReadModel, @QueryHandler,
│                           DomainEvent, DomainCommand
│   └── householdplan       HouseholdPlan (Aggregate Root, ein Plan pro Jahr) mit BudgetItems (Entities),
│                           Money, ExpenseCategory, HouseholdPlanRepository, Events in .event
│                           Leseseite (.overview): Read Model HouseholdPlanOverview und HouseholdPlanOverviewQueryHandler
├── application
│   ├── shared              @ApplicationService
│   └── householdplan       HouseholdPlanApplicationService + Commands in .command
└── infrastructure
    ├── web                 REST-Controller, DTOs, Fehler als RFC 9457 Problem Details
    ├── persistence         HouseholdPlanRepository und HouseholdPlanOverviewQueryHandler über JdbcClient,
    │                       Schema per Flyway
    └── audit               Beispiel-Listener für Domain Events
```

**Schreibseite:** Der `HouseholdPlanApplicationService` lädt pro Command den `HouseholdPlan` über das Repository,
ruft dessen Verhalten auf und speichert ihn. Der Plan sammelt dabei Domain Events (`HouseholdPlanCreated`,
`IncomeAdded`, `ExpenseAdded`, `BudgetItemRemoved`), die der Service nach dem Speichern über Springs
`ApplicationEventPublisher` veröffentlicht. Listener wie `AuditLogListener` reagieren per
`@TransactionalEventListener` erst nach dem Commit und in einer eigenen Transaktion.

**Leseseite:** Es gibt ein Read Model, die `HouseholdPlanOverview` eines Jahres mit Einträgen, Summen und
50/30/20-Aufteilung. Der ApplicationService liefert sie über `getOverview(year)` und lädt sie dafür über den
`HouseholdPlanOverviewQueryHandler`. Dessen JDBC-Implementierung liest direkt per SQL, ohne Aggregat oder Repository.

Abhängigkeitsregeln (Infrastructure → Application → Domain, Domain ohne Frameworks), die Lage der Bausteine und die
Trennung von Schreib- und Leseseite prüft `ArchitectureTest` (ArchUnit).

## Lokale Entwicklung

Voraussetzung: Docker (für PostgreSQL und die Integrationstests).

```bash
# Backend auf :8080 – startet PostgreSQL aus compose.yaml automatisch (spring-boot-docker-compose)
cd backend && ./mvnw spring-boot:run

# Frontend auf :4200, /api wird per Proxy an :8080 weitergeleitet
cd frontend && npm ci && npm start
```

Tests: `./mvnw verify` (backend, nutzt Testcontainers) bzw. `npm test` (frontend).
API-Dokumentation: http://localhost:8080/api/docs

## API-Vertrag ändern

Das Frontend generiert seine TypeScript-Typen aus `api/openapi.json` (`npm run generate:api`, läuft automatisch
vor `start`, `build` und `test`). `OpenApiContractTest` im Backend schlägt fehl, sobald die vom Server erzeugte
Spezifikation davon abweicht. Nach einer gewollten API-Änderung:

```bash
cd backend && ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true
```

## Docker

```bash
# Kompletter Stack (App + PostgreSQL) auf :8080
docker compose --profile app up --build
```

Das Image allein erwartet eine Datenbank über `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` und
`SPRING_DATASOURCE_PASSWORD`. Der Container meldet seinen Zustand über `/actuator/health/readiness`
(Docker-`HEALTHCHECK`).

Die GitHub-Action `.github/workflows/ci.yml` baut und testet beide Teile und pusht anschließend ein Image nach
`ghcr.io/johcier/householdbudgettracker`:

| Auslöser         | Tags                              |
|------------------|-----------------------------------|
| Push auf master  | `latest`, `sha-<commit>`          |
| Git-Tag `v1.2.3` | `1.2.3`, `1.2`, `sha-<commit>`    |
| Pull Request     | nur Build, kein Push              |

Die Node-Version steht ausschließlich in `frontend/.nvmrc`.
