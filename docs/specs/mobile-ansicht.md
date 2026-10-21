# Ansicht auf schmalen Bildschirmen

Status: umgesetzt

## Zweck

Auf dem Handy ist die Planansicht nicht zu gebrauchen: In der Karte „Einträge" belegen Griff, Monats- und
Jahresbetrag und die Knöpfe feste Breiten, sodass für die Beschreibung fast nichts übrig bleibt und sie Buchstabe für
Buchstabe umbricht. Die Summenzeilen verrutschen, und die Seite wird breiter als der Bildschirm. Auch die
Jahresübersicht ragt über den Rand, und im Dialog „Neuer Eintrag in ‹Gruppe›" wird die Auswahl „Ausgabe/Einnahme"
zusammengedrückt.

Diese Spec legt fest, wie die Oberfläche auf schmalen Bildschirmen aussieht. Fachliche Regeln anderer Specs ändern sich
nicht, nur die Anordnung (insbesondere MJ-12 und EG-5 bis EG-7).

## Fachliche Regeln

- **MA-1**: Ab einer Bildschirmbreite von 360 px ist keine Seite breiter als der Bildschirm. Die Seiten scrollen nur
  senkrecht; nichts ragt über den Rand oder wird abgeschnitten.
- **MA-2**: Ist die Karte „Einträge" schmaler als 600 px (auf dem Handy, aber auch auf mittleren Bildschirmen neben dem
  Formular), steht ein Posten in zwei Zeilen: oben Griff, Beschreibung und die Knöpfe „Bearbeiten" und „Löschen",
  darunter rechtsbündig Monats- und Jahresbetrag. Die Spaltenköpfe „Monat" und „Jahr" stehen über diesen Beträgen.
  Ist die Karte breit genug, bleibt alles in einer Zeile wie bisher (MJ-12).
- **MA-3**: Bei schmaler Karte bleibt eine Summenzeile (EG-5, EG-6) einzeilig, wenn „Summe ‹…›", gegebenenfalls der
  Knopf „Posten hinzufügen" (EG-9) und beide Beträge nebeneinander passen. Sonst stehen oben „Summe ‹…›" und der
  Knopf, darunter die Beträge. Die Beträge stehen in beiden Fällen in den Spalten „Monat" und „Jahr"; der Knopf steht
  bei schmaler Karte neben der Bezeichnung.
- **MA-4**: Die Beschreibung eines Postens bricht nur zwischen Wörtern oder mit Silbentrennung um, nie Buchstabe für
  Buchstabe. Posten ohne Griff (allein in ihrer Gruppe) stehen bündig mit Posten mit Griff.
- **MA-5**: In der Jahresübersicht passen auf dem Handy alle Spalten auf den Bildschirm; die Knöpfe „Als Vorlage
  übernehmen" und „Plan löschen" stehen dafür untereinander. Werden die Beträge so lang, dass die Tabelle trotzdem
  nicht passt, scrollt die Tabelle in ihrer Karte waagerecht, nicht die Seite.
- **MA-6**: In Dialogen (Neuer Eintrag in ‹Gruppe›, Eintrag bearbeiten, Plan übernehmen) behalten alle Felder ihre
  Höhe; reicht die Höhe des Bildschirms nicht, scrollt der Inhalt des Dialogs.
- **MA-7**: Auch mit vergrößerter Schrift (bis 120 %, wie in den Bedienungshilfen des Handys einstellbar) gilt MA-1
  auf einem 360 px breiten Bildschirm, und die Beträge stehen weiter unter „Monat" und „Jahr". Passen bei einem
  Posten die Markierungen (PM-12) und die Beträge nicht nebeneinander, rücken die Beträge in eine eigene Zeile unter
  die Markierungen.

## Akzeptanzkriterien

- **Gegeben** ein Plan mit Einnahmen und Ausgaben in mehreren Gruppen auf einem 390 px breiten Bildschirm, **wenn** der
  Benutzer den Plan öffnet, **dann** ist die Seite nicht breiter als der Bildschirm (MA-1).
- **Gegeben** derselbe Plan, **dann** steht die Beschreibung „Grundsteuer und Gebäudeversicherung" auf höchstens drei
  Zeilen, und Monats- und Jahresbetrag stehen unter ihr, rechtsbündig unter „Monat" und „Jahr" (MA-2, MA-4).
- **Gegeben** die Gruppe „Haus" auf einem 390 px breiten Bildschirm, **dann** steht unter „Summe Haus" der Monats- und
  der Jahresbetrag, unter „Monat" und „Jahr"; der Knopf „Posten in Haus hinzufügen" ist sichtbar (MA-3).
- **Gegeben** derselbe Plan auf einem 900 px breiten Bildschirm (Karte neben dem Formular), **dann** stehen die Posten
  zweizeilig, „Summe Haus" aber mit ihren Beträgen in einer Zeile (MA-2, MA-3).
- **Gegeben** derselbe Plan auf einem 1280 px breiten Bildschirm, **dann** stehen Beschreibung und Beträge eines
  Postens in einer Zeile (MA-2).
- **Gegeben** die Jahresübersicht auf einem 390 px breiten Bildschirm, **dann** ist die Seite nicht breiter als der
  Bildschirm, und „Plan löschen" liegt auf dem Bildschirm (MA-1, MA-5).
- **Gegeben** derselbe Plan mit weiteren Einnahmen von 12.345,67 € monatlich auf einem 360 px breiten Bildschirm
  mit Schrift auf 120 %, **dann** ist die Seite nicht breiter als der Bildschirm, und Posten und „Summe Einnahmen"
  stehen rechtsbündig unter „Monat" und „Jahr" (MA-7).
- **Gegeben** der Dialog „Neuer Eintrag in Haus" auf einem 390 × 844 px großen Bildschirm, **dann** ist die Auswahl
  „Ausgabe/Einnahme" vollständig zu sehen (MA-6).

## Nicht im Umfang

- Eine eigene Navigation oder ein eigenes Layout für das Handy (z. B. Reiter statt untereinander gestapelter Karten)
- Verschieben per Ziehen auf Touch-Geräten verbessern; die Griffe bleiben, wie sie sind
- Querformat und Tablets gesondert gestalten; sie bekommen die schmale oder die breite Anordnung je nach Kartenbreite

## Umsetzung

- `budget/ui/budget-entries.scss`: Die Karte ist ein Container (`container-type: inline-size`); unter 600 px
  Kartenbreite ordnet ein Grid die Posten zweizeilig an (MA-2). In Summenzeilen bilden die Beträge eine Einheit
  (`.amounts`), die als Ganzes in die nächste Zeile umbricht, wenn sie nicht neben die Bezeichnung passt (MA-3). Die Beschreibung trennt mit
  `hyphens: auto`, der Platzhalter für den Griff ist so breit wie der Griff (MA-4). Markierungen und Beträge eines
  Postens bilden die zweite Zeile (`.details`, umbrechend), die Beträge darin eine Einheit (`.amounts`), die bei
  Platzmangel unter die Markierungen rückt; Betragsspalten 6rem, Markierungen 32 px (MA-7)
- `budget/budget-dashboard.ts`: Die Spalten des Rasters dürfen schmaler als ihr Inhalt werden (`minmax(0, 1fr)`,
  `min-width: 0`), sodass keine Karte die Seite verbreitert (MA-1)
- `budget/yearly-overview-page.ts`: Unter 600 px Bildschirmbreite stehen die Knöpfe untereinander, die Karte scrollt
  im Notfall waagerecht (MA-1, MA-5)
- `budget/ui/add-item-dialog.ts`, `edit-item-dialog.ts`, `copy-plan-dialog.ts`: Felder schrumpfen nicht (MA-6)
- E2E: `e2e/tests/mobile-ansicht.spec.ts` (MA-1 bis MA-7) mit Viewports von 390 × 844 px, 900 px und 1280 px

## Offene Fragen

Keine. Entschieden bei der Freigabe: Auf dem Handy bleiben Monats- und Jahresbetrag zu sehen (MA-2); Summenzeilen
stehen einzeilig, wenn alles nebeneinander passt, sonst zweizeilig (MA-3).
