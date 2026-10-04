# Household Budget Tracker

Haushaltsbuch nach der 50/30/20-Regel: höchstens 50 % des Jahreseinkommens für den Lebensunterhalt,
höchstens 30 % für Freizeit & Spaß, mindestens 20 % für Sparen & Investieren.

| Ordner      | Inhalt                                                                                   |
|-------------|------------------------------------------------------------------------------------------|
| `backend/`  | Spring Boot (Java 25, Maven) – DDD / hexagonale Architektur, REST-API unter `/api/budget` |
| `frontend/` | Angular 22 + Angular Material                                                            |
| `api/`      | `openapi.json` – REST-Vertrag zwischen Backend und Frontend                               |

## Architektur

```
de.jcss.hbt
├── domain        BudgetItem (Aggregate Root), Money, BudgetOverview, ExpenseCategory – reines Java
├── application   BudgetService, Ports: port.in.BudgetUseCases, port.out.BudgetItemRepository
└── adapter
    ├── in.web    REST-Controller, DTOs, Fehler als RFC 9457 Problem Details
    └── out.persistence   PostgreSQL über JdbcClient, Schema per Flyway
```

Die Abhängigkeitsregeln (Adapter → Application → Domain, Domain ohne Frameworks) prüft `ArchitectureTest` (ArchUnit).

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
`ghcr.io/johcier/householdbudgettracker-ai`:

| Auslöser         | Tags                              |
|------------------|-----------------------------------|
| Push auf master  | `latest`, `sha-<commit>`          |
| Git-Tag `v1.2.3` | `1.2.3`, `1.2`, `sha-<commit>`    |
| Pull Request     | nur Build, kein Push              |

Die Node-Version steht ausschließlich in `frontend/.nvmrc`.
