---
name: struktur-review
description: Das ganze Repository auf strukturelle Verbesserungen scannen (doppelter Code und doppelte Konfiguration, verstreute Versionen, wachsende Aufzählungen, zu große Dateien, Build, CI, Docker) und die Funde als Liste oder GitHub-Issue vorlegen. Verwenden bei „Struktur-Review", „Repo scannen", „was ist doppelt" und in der monatlichen Routine.
---

# Struktur-Review

Ziel: Doppelungen und Strukturschwächen finden, die bei der Arbeit Spec für Spec nicht auffallen. Maßstab ist
`.claude/rules/struktur.md`. Der Skill ändert keinen Code; er liefert Funde mit Fundstelle und Vorschlag.

## 1. Bekanntes ausschließen

- Offene Issues mit Label `struktur` lesen (`gh api -X GET search/issues -f q='repo:JohCier/HouseholdBudgetTracker label:struktur is:open'`
  oder GitHub-MCP `list_issues`); was dort steht, nicht erneut melden, außer es ist schlimmer geworden.
- Offene PRs überfliegen: was gerade umgebaut wird, nicht melden.

## 2. Prüfen

Jeweils nur die Ausgabe der Befehle auswerten; Dateien erst lesen, wenn ein Treffer belegt werden muss.

1. **Gleich benannte Dateien in mehreren Apps** und wie stark sie sich unterscheiden:

   ```bash
   for f in $(cd apps/backend/src && find . -name '*.java' -printf '%f\n' | sort -u); do
     u=$(find apps/users/src -name "$f" | head -1); b=$(find apps/backend/src -name "$f" | head -1)
     [ -n "$u" ] && echo "$f $(diff <(sed 's/\.users//g' "$b") <(sed 's/\.users//g' "$u") | grep -c '^[<>]')"
   done
   ```

   `package-info.java` ignorieren. 0 bis 5 abweichende Zeilen: Kandidat für eine gemeinsame Bibliothek
   (Test-Hilfen in die Test-Bibliothek).
2. **Fast gleiche Dateien im Frontend**: Komponenten mit ähnlichem Namen paarweise vergleichen, z. B.
   `diff budget/ui/copy-plan-dialog.ts net-worth/ui/copy-net-worth-dialog.ts | grep -c '^[<>]'` im Verhältnis zur Länge.
   Ab etwa 50 % Übereinstimmung melden.
3. **Verstreute Versionen**:

   ```bash
   git grep -nE 'postgres:[0-9]|temurin:[0-9]|java-version|<java.version>|node:[0-9$]|NODE_VERSION=[0-9]|openjdk-[0-9]+' -- ':!**/package-lock.json'
   ```

   Mit der Tabelle der Quellen in `.claude/rules/struktur.md` abgleichen: Jede Version, die außerhalb ihrer Quelle von
   Hand steht und weder von dort gelesen noch von der CI geprüft wird, melden. Prüfen, ob `.github/dependabot.yml` alle
   Stellen erreicht (jede `package-lock.json`, jedes Dockerfile, jede Compose-Datei).
4. **Wachsende Aufzählungen**: Klassen mit vielen gleich gebauten Methoden, z. B.
   `grep -c '@ExceptionHandler' $(find apps -name GlobalExceptionHandler.java)`; Listen in CI, compose oder Dependabot,
   die je App einen fast gleichen Block haben.
5. **Große Dateien** (ohne Tests, Lock-Dateien und `openapi/*.json`):

   ```bash
   git ls-files 'apps/*/src/main/**' 'apps/frontend/src/app/**' | grep -v '\.spec\.ts$' | xargs wc -l | sort -rn | head -15
   ```

   Über etwa 400 Zeilen: sagen, wie sich die Datei teilen ließe.
6. **Build, CI, Docker**: Dockerfiles, Workflows und Pakete paarweise vergleichen (`diff apps/backend/Dockerfile
   apps/users/Dockerfile`); Pakete außerhalb des npm-Workspaces (eigene `package-lock.json`); Schritte, die in mehreren
   Jobs gleich stehen.
7. **Gegenprobe**: Ist eine Doppelung Absicht (z. B. getrennte Services, die unabhängig deploybar bleiben sollen), steht
   das in README, Javadoc oder einem Kommentar. Dann nicht melden.

## 3. Ergebnis

Je Fund eine Zeile Befund mit Fundstelle und Zahl (z. B. „`IntegrationTest.java` in backend und users identisch"),
eine Zeile Vorschlag, Aufwand (klein/mittel/groß), nach Nutzen sortiert. Höchstens zehn Funde; der Rest als Stichwort.

- **Im Gespräch**: Liste vorlegen und fragen, welche Punkte umgesetzt werden sollen. Nichts umbauen ohne Zustimmung.
- **In der Routine**: ein GitHub-Issue „Struktur-Review <Monat Jahr>" mit Label `struktur` anlegen, Inhalt wie oben,
  auf Deutsch. Gibt es keine neuen Funde, kein Issue anlegen.

Taucht ein Fund-Typ wiederholt auf, der in `.claude/rules/struktur.md` noch nicht steht, die Ergänzung der Regel als
eigenen Punkt vorschlagen.
