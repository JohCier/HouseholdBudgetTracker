# Household Budget Tracker

Haushaltsbuch nach der 50/30/20-Regel: höchstens 50 % des Jahreseinkommens für den Lebensunterhalt,
höchstens 30 % für Freizeit & Spaß, mindestens 20 % für Sparen & Investieren. Jeder Benutzer sieht und bearbeitet nur
seine eigenen Haushaltspläne.

Funktionsumfang, Nicht-Ziele und fachliche Regeln beschreiben [docs/produkt.md](docs/produkt.md),
[docs/glossar.md](docs/glossar.md) und die Specs in [docs/specs/](docs/specs/).

| Ordner                   | Inhalt                                                                                      |
|--------------------------|---------------------------------------------------------------------------------------------|
| `pom.xml`                | Maven-Parent und Aggregator der Java-Apps: Versionen, gemeinsame Abhängigkeiten, Test-Konfiguration |
| `apps/backend/`          | Spring Boot (Java 25, Maven) – Haushaltspläne, DDD mit getrennter Schreib-/Leseseite, REST-API unter `/api/plans` |
| `apps/users/`            | Spring Boot (Java 25, Maven) – Registrierung und Anmeldung, REST-API unter `/api/auth`        |
| `apps/frontend/`         | Angular 22 + Angular Material; im Container nginx als einziger Einstiegspunkt                 |
| `packages/shared-types/` | REST-Verträge beider Dienste (`openapi/*.json`) und die daraus generierten TypeScript-Typen   |

Das Repository ist ein npm-Workspace (`package.json` im Wurzelverzeichnis) aus `apps/frontend` und
`packages/shared-types`. Die Java-Apps sind Module eines Maven-Projekts: Die `pom.xml` im Wurzelverzeichnis erbt von
`spring-boot-starter-parent` und legt Versionen (inkl. der CVE-Overrides), die Abhängigkeiten beider Apps und die
Konfiguration von Surefire und Failsafe fest; die `pom.xml` einer App enthält nur, was nur sie braucht. Maven läuft
im Wurzelverzeichnis: ohne `-pl` für beide Apps, mit `-pl apps/backend` bzw. `-pl apps/users` für eine.

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
│                           HouseholdPlanRepository; Value Objects (Money, OwnerId, BudgetSummary, ExpenseCategory, …)
│                           in .valueobject, Commands in .command, Events in .event
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
(`HouseholdPlanCreated`, `IncomeAdded`, `ExpenseAdded`, `BudgetItemChanged`, `BudgetItemRemoved`), die der Service nach dem Speichern über
Springs `ApplicationEventPublisher` veröffentlicht. Listener wie `AuditLogListener` reagieren per
`@TransactionalEventListener` erst nach dem Commit und in einer eigenen Transaktion. Jeder Command antwortet mit der
Übersicht des Plans nach der Änderung, die der Service noch in derselben Transaktion über die Leseseite liest; die
Oberfläche zeigt sie an, ohne neu zu laden (siehe [antwort-mit-uebersicht.md](docs/specs/antwort-mit-uebersicht.md)).

**Leseseite:** Es gibt ein Read Model, die `HouseholdPlanOverview` eines Jahres mit Einträgen, Summen und
50/30/20-Aufteilung. Summen, Saldo und die Bewertung nach 50/30/20 rechnet das Value Object `BudgetSummary` aus der
Domäne; das Read Model befüllt es nur und gibt die Werte weiter. Der ApplicationService liefert sie über
`getOverview(owner, year)` und lädt sie dafür über den `HouseholdPlanOverviewQueryHandler`. Dessen JDBC-Implementierung
liest direkt per SQL, ohne Aggregat oder Repository.

### Users (`apps/users`)

```
de.jcss.hbt.users
├── domain
│   └── user                User (Aggregate Root), PasswordHasher (Port), UserRepository; Value Objects (UserId,
│                           EmailAddress, Password, PasswordHash) in .valueobject, Commands in .command, Events in .event
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
Value Objects als Records/Enums im Paket `valueobject`, Entities im Paket ihrer Aggregate Root, Events als Records mit
`timestamp`, Gleichheit von Entities über ihre ID).

## Lokale Entwicklung

Voraussetzung: Docker (für PostgreSQL und die Integrationstests).

```bash
# Backend auf :8080 – startet PostgreSQL aus compose.yaml automatisch (spring-boot-docker-compose)
./mvnw spring-boot:run -pl apps/backend

# Users auf :8081 – nutzt dieselbe PostgreSQL
./mvnw spring-boot:run -pl apps/users

# Frontend auf :4200, /api/auth wird per Proxy an :8081, /api an :8080 weitergeleitet
npm ci && npm start --workspace=frontend
```

`./mvnw spring-boot:run` und die Tests setzen ein Entwicklungs-Secret für `JWT_SECRET` (siehe `pom.xml` im Wurzelverzeichnis).
Wer die Apps anders startet, z. B. aus der IDE, muss `JWT_SECRET` selbst setzen – in beiden Apps auf denselben Wert,
mindestens 32 Bytes; ohne startet keine der beiden.

Tests (im Wurzelverzeichnis; für nur eine App `-pl apps/backend` bzw. `-pl apps/users` anhängen):

- `./mvnw test` – schnelle Tests ohne Docker (Domäne, Application Services, Controller per `@WebMvcTest`,
  `ArchitectureTest`), dauert Sekunden
- `./mvnw verify` – zusätzlich alle mit `@IntegrationTest` markierten Tests: Sie starten die ganze App gegen eine
  PostgreSQL per Testcontainers und brauchen Docker; Maven führt sie nach den schnellen Tests über das
  Failsafe-Plugin aus. Neue Tests mit Datenbank bekommen deshalb `@IntegrationTest` statt `@SpringBootTest`.

Frontend: `npm test --workspace=frontend`. API-Dokumentation des Backends: http://localhost:8080/api/docs

E2E-Tests (`e2e/`, Playwright): Sie bedienen die Oberfläche in Chromium gegen den kompletten Stack und decken die
Kernabläufe der Specs ab (Registrierung und Anmeldung, Haushaltsplan, Posten erfassen und bearbeiten, 50/30/20-Auswertung). Jeder Test
registriert einen eigenen Benutzer, die Tests laufen deshalb parallel und auch gegen eine Datenbank mit Altdaten.

```bash
docker compose --profile app up --build --wait   # Stack auf http://localhost:8080
cd e2e && npm ci && npx playwright install chromium
npx playwright test                              # andere Adresse: E2E_BASE_URL=http://localhost:4200 npx playwright test
```

In der CI laufen sie im Workflow `.github/workflows/e2e.yml`, weil sie mehrere Minuten dauern, nur bei Bedarf:

- **Pull Request:** nur mit dem Label `e2e`. Das Setzen des Labels startet den Lauf, danach läuft jeder weitere Push
  auf den PR ebenfalls mit E2E-Tests. Ohne Label wird der Job übersprungen.
- **Manuell:** „Run workflow“ beim Workflow „E2E“ im Actions-Tab, für einen beliebigen Branch.
- **Push auf master und Tags:** `ci.yml` ruft den Workflow vor dem Image-Build auf, wenn sich das Frontend (siehe
  Pfadfilter unten) oder die E2E-Tests geändert haben; reine Backend-Änderungen laufen ohne E2E-Tests. Schlägt einer
  fehl, werden keine Images gebaut und nichts deployt.

Die Images des Stacks nutzen den Layer-Cache, den der Image-Build in `ci.yml` schreibt, Chromium kommt aus dem
Actions-Cache, und Playwright läuft in der CI mit 4 Workern. Schlägt ein Test fehl, hängt der Playwright-Bericht mit
Traces und Screenshots als Artefakt `playwright-report` am Lauf.

## API-Verträge ändern

Die Java-Controller und ihre DTOs sind die einzige Quelle des Vertrags (Code-first). Alles Weitere wird daraus
erzeugt, die Richtung ist immer Java → JSON → TypeScript:

```
Controller + DTOs (Java)
   │  springdoc erzeugt zur Laufzeit die Spezifikation unter /v3/api-docs
   │  OpenApiContractTest -Dopenapi.update=true schreibt sie in die Datei
   ▼
packages/shared-types/openapi/plans.json bzw. users.json   (eingecheckt, nie von Hand bearbeiten)
   │  npm run generate:api – läuft automatisch vor start, build und test des Frontends
   ▼
TypeScript-Typen in @hbt/shared-types → Frontend
```

`OpenApiContractTest` der jeweiligen Java-App vergleicht die vom Server erzeugte Spezifikation mit der eingecheckten
JSON-Datei und schlägt fehl, sobald beide voneinander abweichen. So fällt jede API-Änderung auf und muss bewusst
übernommen werden.

Vorgehen bei einer Änderung, z. B. ein neues Feld in einem Request-DTO:

1. Controller bzw. DTO in der betroffenen App (`apps/backend` oder `apps/users`) ändern.
2. `./mvnw verify` – `OpenApiContractTest` schlägt mit „API changed …“ fehl; das ist erwartet.
3. Die JSON-Datei aus dem Java-Code neu erzeugen:

   ```bash
   ./mvnw verify -pl apps/backend -Dit.test=OpenApiContractTest -Dopenapi.update=true   # bzw. -pl apps/users
   ```

4. Frontend bauen oder testen – die TypeScript-Typen werden neu generiert, der Compiler zeigt die Stellen, die
   angepasst werden müssen.
5. Java-Änderung, JSON-Datei und Frontend-Anpassung gemeinsam committen.

## Docker

```bash
# Kompletter Stack (nginx + backend + users + PostgreSQL) auf :8080
docker compose --profile app up --build
```

Jede App hat ihr eigenes Image (`apps/*/Dockerfile`), Build-Kontext ist bei allen das Wurzelverzeichnis (das
Frontend braucht den npm-Workspace, die Java-Apps die Parent-POM), z. B. `docker build -f apps/backend/Dockerfile .`;
`.dockerignore` im Wurzelverzeichnis gilt für alle Images. Das Frontend-Image (nginx) liefert die Angular-App aus und leitet `/api/auth/**` an `users:8081`, `/api/**` an
`backend:8080` weiter; nur dieser Container ist nach außen erreichbar. Backend und Users erwarten die Datenbank über
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` und `SPRING_DATASOURCE_PASSWORD` sowie `JWT_SECRET`, und melden
ihren Zustand über `/actuator/health/readiness` (Docker-`HEALTHCHECK`). Metriken für Prometheus (JVM, HTTP-Anfragen,
Verbindungspool) liefern beide ohne Anmeldung unter `/actuator/prometheus`; wie die Health-Endpunkte ist er von außen
nicht erreichbar, weil nginx nur `/api` und die API-Dokumentation an die Apps weiterleitet. Das Sitzungs-Cookie ist standardmäßig
`Secure`; solange die App nur per HTTP (nicht localhost) erreichbar ist, muss `HBT_AUTH_SECURE_COOKIE=false` gesetzt
sein.

Die GitHub-Action `.github/workflows/ci.yml` baut und testet die geänderten Apps und pusht anschließend je ein Image
nach `ghcr.io/johcier/householdbudgettracker/{backend,users,frontend}`. Auch das Frontend wird nur gebaut, getestet
und deployt, wenn es sich geändert hat:

| Auslöser          | Gebaute Apps               | Tags                           |
|-------------------|----------------------------|--------------------------------|
| Push auf master   | geänderte Apps             | `latest`, `sha-<commit>`       |
| Git-Tag `v1.2.3`  | alle                       | `1.2.3`, `1.2`, `sha-<commit>` |
| Pull Request      | geänderte Apps             | nur Build, kein Push           |
| Manueller Start   | alle                       | wie Push auf master            |

Welche Dateien zu welcher App gehören, legen die Pfadfilter im Job `changes` fest: für Backend und Users
`apps/<app>/**` und der jeweilige OpenAPI-Vertrag in `packages/shared-types/openapi/`, für das Frontend
`apps/frontend/**`, `packages/shared-types/**`, `package.json`, `package-lock.json` und `.dockerignore`. Eine Änderung
an `ci.yml` baut alle Apps. Übersprungene Jobs gelten als erfolgreich, Pflicht-Checks in den Branch-Regeln blockieren
den Merge also nicht. Welche App gebaut wurde, zeigt der Job-Graph des Workflow-Laufs (unveränderte Apps haben keinen
oder einen übersprungenen Job), welche Dateien
geändert wurden, das Log des Filter-Schritts im Job `changes`. Unveränderte Apps bekommen kein neues
`sha-<commit>`-Tag, für `IMAGE_TAG` eignet sich daher nur `latest` oder eine Release-Version. Schlägt ein Build fehl,
holt ihn der nächste Push nicht automatisch nach (die App gilt dann als unverändert); dafür die Action manuell starten.

Bei jedem Lauf prüft Trivy die Abhängigkeiten (`pom.xml`, `package-lock.json`) auf bekannte Sicherheitslücken
(CVEs). Funde der Schwere CRITICAL oder HIGH, für die es bereits einen Fix gibt, lassen den Job `cve-scan` fehlschlagen;
Build, Push und Deploy laufen trotzdem weiter. Die Funde stehen in der Zusammenfassung des Workflow-Laufs. Bewusst
akzeptierte Funde kommen mit Begründung in eine `.trivyignore` im Repository-Root (eine CVE-ID pro Zeile). Die
Trivy-Version (`version` in `ci.yml`) aktualisiert Dependabot nicht, sie ist von Hand nachzuziehen.

Für das Deployment (`compose.prod.yaml`) braucht der Server in `/opt/hbt/.env` neben `DB_PASSWORD` auch ein
`JWT_SECRET` (z. B. `openssl rand -base64 48`). In Produktion steht vor dem Frontend ein [Caddy](https://caddyserver.com)
als einziger nach außen erreichbarer Container (Ports 80 und 443). Ohne weitere Angaben liefert er die App wie bisher
per HTTP auf Port 80 aus.

HTTPS einschalten:

1. Für die Domain einen DNS-A-Eintrag (bei IPv6 zusätzlich AAAA) auf die IP des Servers setzen.
2. In der Firewall des Servers die Ports 80 und 443 (TCP, für HTTP/3 auch UDP) öffnen; Let's Encrypt prüft die
   Domain über Port 80.
3. In `/opt/hbt/.env` `DOMAIN=budget.example.de` und `SECURE_COOKIE=true` eintragen und
   `docker compose -f compose.prod.yaml up -d` ausführen (oder den nächsten Deploy abwarten).

Caddy holt dann ein Zertifikat von Let's Encrypt, verlängert es selbst und leitet HTTP auf HTTPS um. Zertifikate liegen
im Volume `caddy-data`, damit sie Neustarts überdauern.

Die Node-Version steht ausschließlich in `apps/frontend/.nvmrc`.
