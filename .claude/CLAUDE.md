# Household Budget Tracker

Haushaltsbuch nach der 50/30/20-Regel: Benutzer planen pro Jahr Einnahmen und Ausgaben, die Anwendung bewertet, ob
Lebensunterhalt (höchstens 50 %), Freizeit & Spaß (höchstens 30 %) und Sparen & Investieren (mindestens 20 %) ihren
Anteil am Einkommen einhalten. Jeder Benutzer sieht nur seine eigenen Pläne.

## Fachliche Dokumentation

- Zweck, Funktionsumfang und Nicht-Ziele stehen in `docs/produkt.md`, Fachbegriffe und ihre Namen im Code in
  `docs/glossar.md`.
- Vor jeder fachlichen Änderung die betroffene Spec in `docs/specs/` lesen. Widerspricht der Auftrag einer Spec oder
  fehlt eine passende Spec, nachfragen. Ändert sich fachliches Verhalten, wird die Spec im selben PR angepasst.
- Neue Specs entstehen aus `docs/specs/_vorlage.md`; neue Fachbegriffe kommen ins Glossar.
- Tests, Commits und PRs verweisen auf die Regel-IDs der Specs (z. B. `HP-2`).

### Ablauf für neue Features

1. Wird ein neues Feature oder eine fachliche Änderung beschrieben, zuerst die Spec schreiben bzw. anpassen
   (Status „geplant"), noch ohne Code. Offene Fragen in der Spec festhalten.
2. Die Spec zur Freigabe vorlegen und dabei die offenen Fragen stellen. Erst nach Freigabe implementieren.
3. Eine Spec pro Auftrag und PR umsetzen; die Akzeptanzkriterien werden zu Tests.
4. Nach der Umsetzung den Status auf „umgesetzt" setzen und den Abschnitt „Umsetzung" ergänzen.

„Setze alle geplanten Specs um" heißt: alle Specs mit Status „geplant" nacheinander, je Spec ein eigener PR.

## Landkarte

| Ordner | Inhalt |
|---|---|
| `apps/backend/` | Spring Boot, Haushaltspläne, `/api/plans`; Pakete `domain`, `application`, `infrastructure` unter `de.jcss.hbt` |
| `apps/users/` | Spring Boot, Registrierung und Anmeldung, `/api/auth`; gleiche Schichten unter `de.jcss.hbt.users` |
| `apps/frontend/` | Angular + Material; je Feature `data-access/` (API, Signal-Store) und `ui/` (Komponenten) |
| `packages/shared-types/` | OpenAPI-Verträge (`openapi/*.json`, generiert, nie von Hand ändern) und daraus erzeugte TypeScript-Typen |

Architektur, Sitzungs-Cookie, Docker und CI beschreibt das `README.md` ausführlich.

## Befehle

- Schnelle Tests ohne Docker: `./mvnw test` in `apps/backend` bzw. `apps/users`
- Alle Tests inkl. Testcontainers: `./mvnw verify` (braucht Docker); neue Tests mit Datenbank bekommen `@IntegrationTest`
- Frontend: `npm test --workspace=frontend`, `npm run build --workspace=frontend` (erzeugen vorher die API-Typen)
- API-Vertrag nach Änderung an Controller oder DTO neu schreiben:
  `./mvnw verify -Dit.test=OpenApiContractTest -Dopenapi.update=true`; JSON, Java und Frontend gemeinsam committen

## Konventionen

- Code, Kommentare und Javadoc auf Englisch; Dokumentation, Oberfläche, Commit-Nachrichten und PRs auf Deutsch.
- Die Kategorien der 50/30/20-Regel heißen auch im Code deutsch (`LEBENSUNTERHALT` …).
- Fertig heißt: schnelle Tests grün, bei Java-Änderungen zusätzlich `./mvnw verify`, bei API-Änderungen Vertrag und
  Frontend im selben Commit.

## Weitere Regeln

Diese Regeln lädt Claude Code automatisch, sobald Dateien im jeweiligen Bereich bearbeitet werden:

- `.claude/rules/ddd.md`: Aggregates, Entities, Value Objects, Repositorys, Commands, Services, Events, Read Models
  (Java-Code in `apps/backend` und `apps/users`)
- `.claude/rules/ci-cd.md`: Regeln für die Pipelines (`.github/workflows/`)
