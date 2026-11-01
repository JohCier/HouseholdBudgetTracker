# Struktur: nichts zweimal pflegen

Gilt für jede Änderung, nicht nur für Java oder CI. Doppelungen entstehen schleichend über viele PRs; deshalb wird
beim Anlegen geprüft, nicht erst im Review.

- **Zweites Vorkommen zentralisieren.** Wer eine Datei, Klasse, Komponente oder Konfiguration anlegt, die es in
  ähnlicher Form schon gibt, sucht zuerst danach (`git ls-files | grep`, `diff`) und legt das Gemeinsame an einer
  Stelle ab. Beispiele: Test-Hilfen beider Java-Services gehören nach `libs/test-support`;
  fast gleiche Dialoge mehrerer Features nach `apps/frontend/src/app/shared` (wie `copy-year-dialog`, siehe
  `.claude/rules/frontend.md`).
- **Versionen an genau einer Stelle.** Java-, Node-, Postgres-, Bibliotheks- und Image-Versionen stehen einmal (Parent-POM,
  `.nvmrc`, `compose.yaml` …) und werden von dort gelesen. Wer eine Version ein zweites Mal hinschreibt, verweist auf die
  Quelle oder nennt im PR, warum es nicht anders geht. Dependabot muss jede Stelle erreichen. Die Quellen:

  | Version | Quelle | Wer sie liest bzw. prüft |
  |---|---|---|
  | Java | `.java-version` und `java.version` in `pom.xml` | `setup-java` (`java-version-file`), Session-Hooks; die `eclipse-temurin`-Images in den Dockerfiles zieht Dependabot nach |
  | Node | `apps/frontend/.nvmrc` | `setup-node`, Session-Hook, Image-Build in `ci.yml`; den Default von `ARG NODE_VERSION` im Frontend-Dockerfile prüft der Job „frontend" |
  | Postgres | `image:` in `compose.yaml` | `TestcontainersConfiguration`, Session-Hook; `compose.prod.yaml` hält Dependabot (`docker-compose`) gleich |
  | Bibliotheken | Parent-POM bzw. `package.json` | Dependabot (`maven`, `npm` in `/` und `/e2e`) |

  Ein neues Paket mit eigener `package-lock.json`, ein neues Dockerfile oder eine neue Compose-Datei bekommt im selben
  PR einen Eintrag in `.github/dependabot.yml`.
- **Muster statt Aufzählung.** Wächst eine Datei mit jedem Feature um denselben Block (eine weitere `@ExceptionHandler`-Methode
  für „nicht gefunden", ein weiterer Pfad in einer Liste), wird das Muster verallgemeinert, z. B. über eine gemeinsame
  Basisklasse (so `NotFoundException` und `AlreadyExistsException` im Backend). Gleiche Schritte in mehreren
  CI-Jobs stehen einmal als YAML-Anker (`&setup-java`).
- **Größe im Blick.** Eine Datei über etwa 400 Zeilen (ohne Tests) wird beim Ändern daraufhin geprüft, ob sie geteilt
  werden sollte. Angular-Komponenten mit mehr als etwa 80 Zeilen Template bekommen eine eigene `.html`-Datei.
- **Nicht still lassen.** Lässt sich eine Doppelung im Auftrag nicht sinnvoll beheben, steht sie im PR unter
  „Offen" mit Fundstelle, statt stillschweigend eine weitere Kopie anzulegen.

Wiederholbar prüft das der Skill `struktur-review`.
