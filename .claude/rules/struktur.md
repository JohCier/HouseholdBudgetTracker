# Struktur: nichts zweimal pflegen

Gilt für jede Änderung, nicht nur für Java oder CI. Doppelungen entstehen schleichend über viele PRs; deshalb wird
beim Anlegen geprüft, nicht erst im Review.

- **Zweites Vorkommen zentralisieren.** Wer eine Datei, Klasse, Komponente oder Konfiguration anlegt, die es in
  ähnlicher Form schon gibt, sucht zuerst danach (`git ls-files | grep`, `diff`) und legt das Gemeinsame an einer
  Stelle ab. Beispiele: Test-Hilfen wie `IntegrationTest` oder `TestcontainersConfiguration` in `apps/backend` und
  `apps/users`; fast gleiche Dialoge wie `copy-plan-dialog` und `copy-net-worth-dialog`.
- **Versionen an genau einer Stelle.** Java-, Node-, Postgres-, Bibliotheks- und Image-Versionen stehen einmal (Parent-POM,
  `.nvmrc`, `compose.yaml` …) und werden von dort gelesen. Wer eine Version ein zweites Mal hinschreibt, verweist auf die
  Quelle oder nennt im PR, warum es nicht anders geht. Dependabot muss jede Stelle erreichen.
- **Muster statt Aufzählung.** Wächst eine Datei mit jedem Feature um denselben Block (eine weitere `@ExceptionHandler`-Methode
  für „nicht gefunden", ein weiterer Pfad in einer Liste), wird das Muster verallgemeinert, z. B. über eine gemeinsame
  Basisklasse.
- **Größe im Blick.** Eine Datei über etwa 400 Zeilen (ohne Tests) wird beim Ändern daraufhin geprüft, ob sie geteilt
  werden sollte. Angular-Komponenten mit mehr als etwa 80 Zeilen Template bekommen eine eigene `.html`-Datei.
- **Nicht still lassen.** Lässt sich eine Doppelung im Auftrag nicht sinnvoll beheben, steht sie im PR unter
  „Offen" mit Fundstelle, statt stillschweigend eine weitere Kopie anzulegen.

Wiederholbar prüft das der Skill `struktur-review`.
