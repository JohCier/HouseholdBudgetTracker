# Einträge in Gruppen: neue Posten unten, Zusammenfassungen, Erfassen in der Gruppe

Status: umgesetzt

## Zweck

Die Karte „Einträge" gliedert die Posten seit [unterkategorie.md](unterkategorie.md) nach Bereich, Kategorie und
Unterkategorie. Drei Dinge passen dabei noch nicht zu der Art, wie Benutzer ihre Gruppen lesen und pflegen:

- Ein neuer Posten erscheint oben in seiner Gruppe. Wer eine Gruppe wie eine Liste von oben nach unten aufbaut,
  erwartet ihn unten und muss ihn heute jedes Mal nach unten ziehen.
- Die Summen stehen in der Kopfzeile neben dem Namen. Wie in einer Tabellenkalkulation gehört eine Summe unter die
  Posten, die sie zusammenfasst.
- Um einer Gruppe einen Posten hinzuzufügen, muss der Benutzer im Formular „Neuer Eintrag" Art, Kategorie und
  Unterkategorie erneut auswählen, obwohl er die Gruppe gerade vor sich hat.

Diese Spec ändert dafür UK-12 und UK-17 in [unterkategorie.md](unterkategorie.md), PO-10 in [posten.md](posten.md),
PB-4 in [posten-bearbeiten.md](posten-bearbeiten.md) und die Anordnung der Summen nach MJ-12 in
[monats-jahresbetrag.md](monats-jahresbetrag.md).

## Fachliche Regeln

### Neue Posten unten

- **EG-1**: Ein neu erfasster Posten steht unten in seiner Gruppe, nach den vorhandenen Posten. Die vorhandenen Posten
  behalten ihre Reihenfolge; bestehende Pläne werden nicht umsortiert. Ersetzt „steht oben" in PO-10 und UK-17.
- **EG-2**: Wechselt ein Posten beim Bearbeiten seine Gruppe (andere Unterkategorie, Kategorie oder Art), steht er unten
  in seiner neuen Gruppe. Bleibt er in seiner Gruppe, behält er seinen Platz (PB-4).
- **EG-3**: Ein durch „Rückgängig" wiederhergestellter Posten (PO-9) steht wie ein neuer Posten unten in seiner Gruppe.
- **EG-4**: Für neue Gruppen gilt weiterhin UK-17: Sie stehen am Ende, vor „Sonstiges", sofern „Sonstiges" am Ende
  steht.

### Summen als Zusammenfassung

- **EG-5**: Jede Unterkategorie-Gruppe endet mit einer Zusammenfassungszeile „Summe ‹Gruppe›" unter ihrem letzten
  Posten, mit den Spalten „Monat" und „Jahr" (MJ-12). Die Kopfzeile der Gruppe zeigt nur noch ihren Namen.
- **EG-6**: Ebenso endet jede Kategorie mit „Summe ‹Kategorie›" unter ihrer letzten Gruppe, der Bereich „Einnahmen"
  mit „Summe Einnahmen" und der Bereich „Ausgaben" mit „Summe Ausgaben". Ihre Kopfzeilen zeigen nur noch den Namen.
- **EG-7**: Zusammenfassungszeilen sind von den Posten abgesetzt (hervorgehobene Schrift, Linie darüber) und eingerückt
  wie die Ebene, die sie zusammenfassen. Sie lassen sich nicht verschieben, bearbeiten oder löschen.
- **EG-8**: Die Werte bleiben dieselben wie bisher: Das Backend rechnet alle Summen (UK-13, MJ-10), die Summe einer
  Kategorie enthält nur die erfassten Ausgaben (UK-12).

### Posten direkt in der Gruppe erfassen

- **EG-9**: Jede Unterkategorie-Gruppe bietet in ihrer Zusammenfassungszeile einen Knopf „Posten hinzufügen" (Symbol
  „+", für Screenreader „Posten in ‹Gruppe› hinzufügen").
- **EG-10**: Der Knopf öffnet den Dialog „Neuer Eintrag in ‹Gruppe›" mit denselben Feldern wie das Formular „Neuer
  Eintrag" (PO-1, MJ-11, UK-6). Art, Kategorie und Unterkategorie sind mit denen der Gruppe ausgefüllt, bei
  „Sonstiges" bleibt die Unterkategorie leer (UK-1). Der Fokus steht in „Beschreibung". Der Benutzer kann alle Felder
  ändern; dann landet der Posten in der Gruppe, die zu seinen Werten passt.
- **EG-11**: „Hinzufügen" prüft die Felder wie das Formular (PO-3 bis PO-5, UK-2), legt den Posten an, schließt den
  Dialog und zeigt den Posten unten in seiner Gruppe (EG-1). „Abbrechen" und die Escape-Taste schließen den Dialog,
  ohne etwas anzulegen. Schlägt das Speichern fehl, bleibt der Dialog mit den Eingaben offen und zeigt den Fehler.
- **EG-12**: Neben dem Dialog in der Gruppe gibt es den Dialog „Posten erfassen" über den Einträgen (AP-14; bis zu
  [planansicht-aufbau.md](planansicht-aufbau.md) das Formular „Neuer Eintrag" neben den Einträgen).

## Akzeptanzkriterien

- **Gegeben** die Gruppe „Auto" mit „Wartung" und „Steuer" (in dieser Reihenfolge), **wenn** der Benutzer „Sprit" mit
  Unterkategorie „Auto" erfasst, **dann** steht „Auto" in der Reihenfolge „Wartung", „Steuer", „Sprit" (EG-1).
- **Gegeben** ein Plan mit Posten aus der Zeit vor dieser Spec, **wenn** er angezeigt wird, **dann** stehen seine
  Posten in derselben Reihenfolge wie vorher (EG-1).
- **Gegeben** „Miete" mit „Kaltmiete" und „Auto" mit „Sprit", **wenn** der Benutzer bei „Sprit" die Unterkategorie
  auf „Miete" ändert, **dann** steht „Sprit" unten in „Miete" (EG-2).
- **Gegeben** die Gruppe „Auto" mit „Wartung" und „Steuer", **wenn** der Benutzer „Wartung" löscht und „Rückgängig"
  wählt, **dann** steht „Wartung" unten in „Auto" (EG-3).
- **Gegeben** „Wartung" 400,00 € jährlich und „Steuer" 150,00 € jährlich in „Auto", Lebensunterhalt, **dann** steht
  unter „Steuer" die Zeile „Summe Auto" mit 45,83 € im Monat und 550,00 € im Jahr, unter der letzten Gruppe von
  Lebensunterhalt „Summe Lebensunterhalt" und am Ende der Ausgaben „Summe Ausgaben"; die Kopfzeilen zeigen keine
  Beträge (EG-5, EG-6).
- **Gegeben** die Gruppe „Auto" in Lebensunterhalt, **wenn** der Benutzer dort „Posten hinzufügen" wählt, **dann**
  öffnet sich „Neuer Eintrag in Auto" mit Art „Ausgabe", Kategorie „Lebensunterhalt" und Unterkategorie „Auto"; nach
  Eingabe von „Sprit", 100,00 € monatlich und „Hinzufügen" steht „Sprit" unten in „Auto" (EG-9 bis EG-11).
- **Gegeben** die Gruppe „Sonstiges" bei den Einnahmen, **wenn** der Benutzer dort „Posten hinzufügen" wählt, **dann**
  ist Art „Einnahme" gewählt, und die Unterkategorie ist leer (EG-10).
- **Gegeben** der Dialog „Neuer Eintrag in Auto", **wenn** der Benutzer „Abbrechen" wählt, **dann** wird nichts
  angelegt (EG-11).

## Nicht im Umfang

- Neue Gruppen direkt in der Karte anlegen (ohne vorhandene Gruppe); dafür bleibt das Formular „Neuer Eintrag"
- Einen Posten zwischen zwei bestimmten Posten einfügen; er steht unten und lässt sich danach verschieben (UK-14)
- Posten direkt in der Zeile bearbeiten (Inline-Bearbeitung)
- Zwischensummen in der Karte „50/30/20-Regel"

## Umsetzung

- Domäne: `HouseholdPlan.addIncome` und `addExpense` hängen den neuen Posten ans Ende der Anzeigereihenfolge,
  `changeItem` setzt einen Posten, der die Gruppe wechselt, ebenfalls ans Ende (EG-1, EG-2). „Rückgängig" legt den
  Posten neu an und landet so auch unten (EG-3). Die Reihenfolge der Gruppen nach UK-17 bleibt unverändert (EG-4)
- Persistenz: `JdbcHouseholdPlanRepository.positionsOf` vergibt die Positionen von oben nach unten; ein Posten behält
  seine gespeicherte Position, solange sie unter der des Postens darüber liegt. Ein neuer Posten unten bekommt die
  nächste Position darunter (auch unter null), sodass Erfassen weiterhin nur eine Zeile schreibt. Bestehende Pläne
  behalten ihre Positionen und damit ihre Reihenfolge (EG-1)
- Frontend: `budget/ui/budget-entries` zeigt die Summen in Zeilen „Summe ‹…›" unter Gruppe, Kategorie, Einnahmen und
  Ausgaben; die Kopfzeilen zeigen nur noch Namen (EG-5 bis EG-7). Die Summenzeile einer Gruppe hat den Knopf „Posten
  hinzufügen" und meldet mit `add` ein `NewItemPreset` (EG-9). `budget/ui/add-item-dialog.ts` ist der Dialog „Neuer
  Eintrag in ‹Gruppe›" mit den Feldern des Formulars, ausgefüllt aus dem Preset, Fokus auf „Beschreibung"; das
  Dashboard öffnet ihn (EG-10 bis EG-12). Die Summen kommen unverändert vom Backend (EG-8)
- Tests: `HouseholdPlanTest`, `HouseholdPlanSubcategoryTest`, `JdbcHouseholdPlanRepositoryTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `HouseholdPlanIntegrationTest`, `budget-entries.spec.ts`,
  `add-item-dialog.spec.ts`
- E2E: `e2e/tests/eintraege-gruppen.spec.ts` (EG-1 bis EG-3, EG-5, EG-6, EG-9 bis EG-11; EG-4 deckt
  `unterkategorie.spec.ts` ab, EG-7, EG-8 und EG-12 die Unit-Tests bzw. die übrigen E2E-Tests)

## Offene Fragen

Keine. Entschieden bei der Freigabe: Ein wiederhergestellter Posten steht unten (EG-3), auch „Ausgaben" bekommt eine
Summenzeile (EG-6), Erfassen in der Gruppe geschieht in einem Dialog (EG-10), alle drei Teile kommen in einem PR.
