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
3. Eine Spec pro Auftrag und PR umsetzen; die Akzeptanzkriterien werden zu Tests. Jedes neue oder geänderte
   Akzeptanzkriterium mit sichtbarem Verhalten in der Oberfläche bekommt im selben PR einen Playwright-Test in
   `e2e/tests/<spec>.spec.ts`, mit der Regel-ID im Testnamen (z. B. `(PB-4)`). Reine API-Fälle (400, 404) decken die
   Backend-Tests ab.
4. Nach der Umsetzung den Status auf „umgesetzt" setzen und den Abschnitt „Umsetzung" ergänzen.

Die Schritte der Umsetzung je Schicht beschreibt der Skill `spec-umsetzen`, die Aktualisierung des API-Vertrags der
Skill `api-vertrag`. Welche Spec zu einem Regel-Präfix (z. B. `PM-`) gehört, zeigt die Tabelle in `docs/produkt.md`.

„Setze alle geplanten Specs um" heißt: alle Specs mit Status „geplant" nacheinander, je Spec ein eigener PR.

## Architektur

Architektur, Sitzungs-Cookie, Docker und CI beschreibt das `README.md` ausführlich.

## Befehle

- Maven-Befehle im Wurzelverzeichnis; ohne `-pl` für beide Services, mit `-pl apps/backend` bzw. `-pl apps/users` für einen.
  Versionen und gemeinsame Abhängigkeiten gehören in die Parent-POM `pom.xml`, in die Service-POM nur das Eigene
- Schnelle Tests ohne Docker: `./mvnw -q test` (grün: keine Ausgabe)
- Alle Tests inkl. Testcontainers: `./mvnw -q verify` (braucht Docker); neue Tests mit Datenbank bekommen `@IntegrationTest`
- Frontend: `npm test -s --workspace=frontend`, `npm run build -s --workspace=frontend` (erzeugen vorher die API-Typen)
- E2E-Tests: Stack mit `docker compose --profile app up --build --wait` starten, dann `npx playwright test` in `e2e`
- Während der Arbeit nur betroffene Tests: `./mvnw -q test -pl apps/backend -Dtest=HouseholdPlanTest`,
  `npm test -s --workspace=frontend -- --include=src/app/budget/data-access/budget-store.spec.ts`,
  `npx playwright test tests/<spec>.spec.ts`; die ganzen Suiten erst vor dem Commit
- Logs der Java-Tests stehen nicht in der Konsole, sondern in `target/surefire-reports/` bzw. `target/failsafe-reports/`
  (`<Testklasse>-output.txt`); nur bei Fehlschlägen gezielt lesen
- API-Vertrag nach Änderung an Controller oder DTO neu schreiben:
  `./mvnw -q verify -pl apps/backend -Dit.test=OpenApiContractTest -Dopenapi.update=true` (bzw. `-pl apps/users`); JSON, Java und Frontend gemeinsam committen.
  `packages/shared-types/openapi/*.json` nicht lesen, sondern mit `git diff --stat` prüfen; Typen stehen in
  `@hbt/shared-types`
- Cloud-Sitzung: Zeigt `echo $JAVA_HOME` nicht JDK 25, Maven-Befehle mit `. .claude/hooks/cloud-env.sh &&` beginnen

## Konventionen

- Code, Kommentare und Javadoc auf Englisch; Dokumentation, Oberfläche, Commit-Nachrichten und PRs auf Deutsch.
- Die Kategorien der 50/30/20-Regel heißen auch im Code deutsch (`LEBENSUNTERHALT` …).
- Maßgebliche Kenngröße ist der Monat: was man im Monat mehr oder weniger zur Verfügung hat. Neue Werte, Eingaben und
  Berechnungen gehen vom Monatsbetrag aus; Jahreswerte sind daraus abgeleitet (Zwölffaches).
- Fertig heißt: schnelle Tests grün, bei Java-Änderungen zusätzlich `./mvnw verify`, bei API-Änderungen Vertrag und
  Frontend im selben Commit.

## Weitere Regeln

Diese Regeln lädt Claude Code automatisch, sobald Dateien im jeweiligen Bereich bearbeitet werden:

- `.claude/rules/ddd.md`: Aggregates, Entities, Value Objects, Repositorys, Commands, Services, Events, Read Models
  (Java-Code in `apps/backend` und `apps/users`)
- `.claude/rules/ci-cd.md`: Regeln für die Pipelines (`.github/workflows/`)
- `.claude/rules/frontend.md`: Features, `shared/` und gemeinsame Dialoge (`apps/frontend/src`)
- `.claude/rules/struktur.md`: nichts zweimal pflegen (Code, Konfiguration, Versionen); gilt immer. Den ganzen Scan
  macht der Skill `struktur-review`
