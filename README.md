# Household Budget Tracker

Haushaltsbuch nach der 50/30/20-Regel: höchstens 50 % des Jahreseinkommens für den Lebensunterhalt,
höchstens 30 % für Freizeit & Spaß, mindestens 20 % für Sparen & Investieren. Jeder Benutzer sieht und bearbeitet nur
seine eigenen Haushaltspläne.

| Ordner                   | Inhalt                                                                                      |
|--------------------------|---------------------------------------------------------------------------------------------|
| `apps/backend/`          | Spring Boot (Java 25, Maven) – Haushaltspläne, DDD mit getrennter Schreib-/Leseseite, REST-API unter `/api/plans` |
| `apps/users/`            | Spring Boot (Java 25, Maven) – Registrierung und Anmeldung, REST-API unter `/api/auth`        |
| `apps/frontend/`         | Angular 22 + Angular Material; im Container nginx als einziger Einstiegspunkt                 |
| `packages/shared-types/` | REST-Verträge beider Dienste (`openapi/*.json`) und die daraus generierten TypeScript-Typen   |

Das Repository ist ein npm-Workspace (`package.json` im Wurzelverzeichnis) aus `apps/frontend` und
`packages/shared-types`; die Java-Apps sind eigenständige Maven-Projekte.

## Architektur

```
Browser ──► nginx (apps/frontend) ──┬─ /api/auth/**  ──► users    ──┐
                                    └─ /api/**       ──► backend  ──┴─► PostgreSQL (Schemas "users" und "public")
```

### Anmeldung

Die Users-App speichert Benutzer (E-Mail-Adresse, BCrypt-Hash des Passworts) im Schema `users`. Registrierung und
Anmeldung setzen ein **HttpOnly-Cookie `HBT_SESSION`** (`SameSite=Strict`) mit einem signierten JWT (HS256, Subject =
Benutzer-ID, 8 Stunden gültig). Der Browser schickt es bei jeder Anfrage mit; das Backend prüft nur die Signatur mit
demselben Secret (`JWT_SECRET`) und kennt Benutzer ausschließlich über ihre ID. Sitzungen werden nicht gespeichert:
Abmelden löscht das Cookie.

Ein Haushaltsplan gehört dem Benutzer, der ihn angelegt hat (`OwnerId` im Aggregate `HouseholdPlan`). Jeder Benutzer hat
höchstens einen Plan pro Jahr. Pläne anderer Benutzer behandelt das Backend wie nicht vorhandene (404), damit ihre
Existenz nicht verraten wird.

### Backend (`apps/backend`)

```
de.jcss.hbt
├── domain                  reines Java, ohne Frameworks
│   ├── shared              @QueryHandler (die übrigen DDD-Bausteine kommen aus jMolecules, s. u.)
│   └── householdplan       HouseholdPlan (Aggregate Root, ein Plan pro Besitzer und Jahr) mit BudgetItems (Entities),
│                           Value Objects (Money, OwnerId, BudgetSummary, ExpenseCategory, …), HouseholdPlanRepository,
│                           Commands in .command, Events in .event
│                           Leseseite (.overview): Read Model HouseholdPlanOverview und HouseholdPlanOverviewQueryHandler
├── application/householdplan  HouseholdPlanApplicationService
└── infrastructure
    ├── web                 REST-Controller, DTOs, Fehler als RFC 9457 Problem Details
    ├── security            Prüfung des Sitzungs-Cookies (Spring Security Resource Server)
    ├── persistence         HouseholdPlanRepository und HouseholdPlanOverviewQueryHandler über JdbcClient,
    │                       Schema per Flyway
    └── audit               Beispiel-Listener für Domain Events
```

**Schreibseite:** Jeder Command enthält den angemeldeten Benutzer als Besitzer. Der `HouseholdPlanApplicationService`
lädt pro Command den `HouseholdPlan` über das Repository, lässt den Plan prüfen, ob er diesem Besitzer gehört
(`isOwnedBy`), ruft dessen Verhalten auf und speichert ihn. Neue Pläne kommen per `add` ins Repository, geänderte per
`save`. Beim Laden sperrt das Repository den Plan bis zum Ende der Transaktion (`SELECT … FOR UPDATE`), sodass
gleichzeitige Änderungen am selben Plan nacheinander statt gegeneinander laufen. Der Plan sammelt dabei Domain Events
(`HouseholdPlanCreated`, `IncomeAdded`, `ExpenseAdded`, `BudgetItemRemoved`), die der Service nach dem Speichern über
Springs `ApplicationEventPublisher` veröffentlicht. Listener wie `AuditLogListener` reagieren per
`@TransactionalEventListener` erst nach dem Commit und in einer eigenen Transaktion.

**Leseseite:** Es gibt ein Read Model, die `HouseholdPlanOverview` eines Jahres mit Einträgen, Summen und
50/30/20-Aufteilung. Summen, Saldo und die Bewertung nach 50/30/20 rechnet das Value Object `BudgetSummary` aus der
Domäne; das Read Model befüllt es nur und gibt die Werte weiter. Der ApplicationService liefert sie über
`getOverview(owner, year)` und lädt sie dafür über den `HouseholdPlanOverviewQueryHandler`. Dessen JDBC-Implementierung
liest direkt per SQL, ohne Aggregat oder Repository.

### Users (`apps/users`)

```
de.jcss.hbt.users
├── domain
│   └── user                User (Aggregate Root), Value Objects (UserId, EmailAddress, Password, PasswordHash),
│                           PasswordHasher (Port), UserRepository, RegisterUserCommand, Event UserRegistered
├── application/user        UserApplicationService (register, authenticate)
└── infrastructure
    ├── web                 AuthController (/api/auth/register, /login, /logout, /me)
    ├── security            Ausstellen und Prüfen der Sitzungs-Cookies, BCryptPasswordHasher
    └── persistence         JdbcUserRepository, Schema "users" per Flyway
```

Der `User` speichert nie das Passwort selbst, nur dessen Hash, und entscheidet selbst, ob ein Passwort stimmt
(`authenticate`). E-Mail-Adressen werden kleingeschrieben gespeichert und sind eindeutig.

Die DDD-Bausteine beider Apps stammen aus [jMolecules](https://github.com/xmolecules/jmolecules): `@AggregateRoot`,
`@Entity` und `@ValueObject` (mit `@Identity` am ID-Feld), `DomainEvent`, `@Command` und `@QueryModel` (Read Models).
Domain Events tragen ihren Zeitpunkt im Feld `timestamp` (`Instant`). Die Schichten sind als Onion-Ringe je Paket
deklariert (`package-info.java` mit `@DomainRing`, `@ApplicationRing`, `@InfrastructureRing`).

In beiden Apps prüft `ArchitectureTest` (ArchUnit) die Abhängigkeitsregeln (Infrastructure → Application → Domain
über jMolecules' `ensureOnionSimple`, Domain ohne Frameworks außer jMolecules), die DDD-Regeln von jMolecules (u. a.
Referenzen auf andere Aggregates nur über deren ID) sowie Lage und Form der Bausteine (keine Setter, nur finale Felder,
Value Objects als Records/Enums, Events als Records mit `timestamp`, Gleichheit von Entities über ihre ID).

## Lokale Entwicklung

Voraussetzung: Docker (für PostgreSQL und die Integrationstests).

```bash
# Backend auf :8080 – startet PostgreSQL aus compose.yaml automatisch (spring-boot-docker-compose)
cd apps/backend && ./mvnw spring-boot:run

# Users auf :8081 – nutzt dieselbe PostgreSQL
cd apps/users && ./mvnw spring-boot:run

# Frontend auf :4200, /api/auth wird per Proxy an :8081, /api an :8080 weitergeleitet
npm ci && npm start --workspace=frontend
```

`./mvnw spring-boot:run` und die Tests setzen ein Entwicklungs-Secret für `JWT_SECRET` (siehe `pom.xml` beider Apps).
Wer die Apps anders startet, z. B. aus der IDE, muss `JWT_SECRET` selbst setzen – in beiden Apps auf denselben Wert,
mindestens 32 Bytes; ohne startet keine der beiden.

Tests: `./mvnw verify` (in `apps/backend` bzw. `apps/users`, nutzt Testcontainers) bzw.
`npm test --workspace=frontend`. API-Dokumentation des Backends: http://localhost:8080/api/docs

## API-Verträge ändern

Das Frontend importiert seine API-Typen aus `@hbt/shared-types`. Das Paket generiert sie aus
`packages/shared-types/openapi/plans.json` und `users.json` (`npm run generate:api`, läuft automatisch vor `start`,
`build` und `test` des Frontends). `OpenApiContractTest` der jeweiligen Java-App schlägt fehl, sobald die vom Server
erzeugte Spezifikation davon abweicht. Nach einer gewollten API-Änderung in der betroffenen App:

```bash
./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true
```

## Docker

```bash
# Kompletter Stack (nginx + backend + users + PostgreSQL) auf :8080
docker compose --profile app up --build
```

Jede App hat ihr eigenes Image (`apps/*/Dockerfile`). Das Frontend-Image (nginx, Build-Kontext ist das
Wurzelverzeichnis) liefert die Angular-App aus und leitet `/api/auth/**` an `users:8081`, `/api/**` an
`backend:8080` weiter; nur dieser Container ist nach außen erreichbar. Backend und Users erwarten die Datenbank über
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` und `SPRING_DATASOURCE_PASSWORD` sowie `JWT_SECRET`, und melden
ihren Zustand über `/actuator/health/readiness` (Docker-`HEALTHCHECK`). Das Sitzungs-Cookie ist standardmäßig
`Secure`; solange die App nur per HTTP (nicht localhost) erreichbar ist, muss `HBT_AUTH_SECURE_COOKIE=false` gesetzt
sein.

Jede App hat einen eigenen Workflow (`.github/workflows/{backend,users,frontend}.yml`), der sie baut, testet und
anschließend ein Image nach `ghcr.io/johcier/householdbudgettracker/<app>` pusht. Bei Push und Pull Request läuft ein
Workflow nur, wenn sich etwas unter `apps/<app>/` oder an gemeinsam genutzten Dateien geändert hat (`packages/`,
`package.json`, `package-lock.json`, `.dockerignore`, `reusable-*.yml`); die Liste steht im `paths`-Filter des
jeweiligen Workflows. Git-Tags und manuelle Starts (`workflow_dispatch`) bauen immer. Die gemeinsamen Schritte liegen in
`reusable-java-test.yml` und `reusable-docker-image.yml`.

| Auslöser         | Tags                              |
|------------------|-----------------------------------|
| Push auf master  | `latest`, `sha-<commit>`          |
| Git-Tag `v1.2.3` | `1.2.3`, `1.2`, `sha-<commit>`    |
| Pull Request     | nur Build, kein Push              |

Unveränderte Apps bekommen kein neues Image, `sha-<commit>` gibt es also nur für die gebauten Apps. Schlägt ein Lauf auf
master fehl, den fehlgeschlagenen Workflow neu starten: Ein späterer Push, der die App nicht berührt, baut sie nicht.
Alle Apps neu bauen: `for w in backend users frontend; do gh workflow run $w.yml; done`.

`deploy.yml` rollt bei jedem Push auf master sowie nach jedem erfolgreichen App-Workflow auf master
(`workflow_run`) `compose.prod.yaml` und die `latest`-Images aus; Deploys laufen nacheinander.

Für das Deployment (`compose.prod.yaml`) braucht der Server in `/opt/hbt/.env` neben `DB_PASSWORD` auch ein
`JWT_SECRET` (z. B. `openssl rand -base64 48`). Optional schaltet `SECURE_COOKIE=true` das `Secure`-Flag ein, sobald
der Server per HTTPS erreichbar ist.

Die Node-Version steht ausschließlich in `apps/frontend/.nvmrc`.
