# Unterkategorie und gruppierte Einträge

Status: geplant

## Zweck

Die Kategorien der 50/30/20-Regel sind grob: Unter Lebensunterhalt stehen Miete, Lebensmittel, Auto und Versicherungen
nebeneinander, und die Tabelle der Einträge zeigt alle Posten in einer langen Liste. Wer wissen will, was ihn „das
Auto" im Jahr kostet (Wartung, Steuer, Sprit, Versicherung), muss die Beträge selbst zusammenrechnen.

Mit dieser Spec kann der Besitzer eines Plans jedem Posten zusätzlich zur Kategorie eine frei benennbare
Unterkategorie geben, z. B. „Auto". Die Anwendung schlägt übliche Unterkategorien vor. Die Einträge werden nach
Einnahmen und Ausgaben, nach Kategorie und nach Unterkategorie gruppiert angezeigt, jeweils mit Summe.

Diese Spec ersetzt das Nicht-Ziel „Eigene Kategorien oder Unterkategorien" in [posten.md](posten.md) zur Hälfte:
Unterkategorien gibt es danach, eigene Kategorien der 50/30/20-Regel weiterhin nicht.

## Fachliche Regeln

### Unterkategorie am Posten

- **UK-1**: Jeder Posten, Einnahme wie Ausgabe, kann eine Unterkategorie haben. Die Unterkategorie ist optional.
- **UK-2**: Eine Unterkategorie ist ein frei wählbarer Name mit 1 bis 50 Zeichen. Führende und abschließende
  Leerzeichen werden entfernt; ein leerer Name gilt als „keine Unterkategorie". Ein längerer Name wird abgelehnt,
  nicht gekürzt.
- **UK-3**: Die Unterkategorie ist unabhängig von der Kategorie der 50/30/20-Regel (PO-5): Dieselbe Unterkategorie,
  z. B. „Auto", kann bei Ausgaben verschiedener Kategorien vorkommen. Sie hat keinen Einfluss auf die Auswertung nach
  der 50/30/20-Regel.
- **UK-4**: Die Unterkategorie wird beim Erfassen eines Postens angegeben und beim Bearbeiten geändert oder entfernt
  (PB-1). Eine Änderung ist eine Änderung des Postens im Sinne von PB-5 und wird als `BudgetItemChanged` protokolliert.
  Wird das Löschen rückgängig gemacht (PO-9), wird auch die Unterkategorie wiederhergestellt.
- **UK-5**: Zwei Unterkategorien sind dieselbe, wenn ihre Namen ohne Beachtung der Groß- und Kleinschreibung gleich
  sind („auto" und „Auto"). Angezeigt wird die Schreibweise des ältesten Postens der Gruppe.

### Vorschläge

- **UK-6**: Beim Erfassen und Bearbeiten schlägt die Oberfläche Unterkategorien vor; der Benutzer kann einen Vorschlag
  wählen oder einen eigenen Namen eingeben. Die Vorschläge filtern sich beim Tippen.
- **UK-7**: Vorgeschlagen werden die vordefinierten Unterkategorien passend zur Art des Postens (UK-8) und alle
  Unterkategorien, die im angezeigten Plan schon bei Posten derselben Art verwendet werden, alphabetisch und ohne
  Dubletten (UK-5).
- **UK-8**: Vordefiniert sind
  - für Ausgaben: Abonnements, Altersvorsorge, Auto, Bildung, Energie, Entertainment, Essen und Trinken, Geschenke und
    Spenden, Gesundheit, Haus, Haustiere, Hobbys, Internet und Handy, Investieren, Kinder, Kleidung, Kredite, Miete,
    Mobilität, Rücklagen, Sonstiges, Urlaub, Vereine, Versicherungen;
  - für Einnahmen: Gehalt, Kapitalerträge, Kindergeld, Mieteinnahmen, Nebenjob, Sonstiges.

  Die vordefinierten Unterkategorien sind nur Vorschläge; sie sind nicht an eine Kategorie der 50/30/20-Regel gebunden.

### Gruppierte Anzeige der Einträge

- **UK-9**: Die Karte „Einträge" gliedert die Posten zuerst nach Art: oben „Einnahmen", darunter „Ausgaben". Ein
  Bereich ohne Posten entfällt.
- **UK-10**: Im Bereich „Ausgaben" folgen die Kategorien in der Reihenfolge Lebensunterhalt, Freizeit & Spaß, Sparen &
  Investieren. Eine Kategorie ohne Posten entfällt. Jede Kategorie hat dieselbe feste Farbe wie in der Torte (TD-2).
- **UK-11**: Innerhalb einer Kategorie bzw. im Bereich „Einnahmen" sind die Posten nach Unterkategorie gruppiert. Die
  Gruppen stehen alphabetisch; Posten ohne Unterkategorie stehen in einer letzten Gruppe „Ohne Unterkategorie".
  Innerhalb einer Gruppe gilt weiterhin PO-10 (neuester Posten oben).
- **UK-12**: Jeder Bereich, jede Kategorie und jede Unterkategorie-Gruppe zeigt die Summe ihrer Posten. Die Summe von
  „Einnahmen" ist das Einkommen, die von „Ausgaben" die Ausgaben (AW-1). Die Summe einer Kategorie enthält nur die
  erfassten Ausgaben, nicht den Überschuss (AW-10); der steht weiterhin nur in der Auswertung.
- **UK-13**: Alle Summen der gruppierten Anzeige rechnet das Backend (wie AW-9); die Oberfläche addiert nichts selbst.

## Akzeptanzkriterien

- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Wartung", 400,00 €, Lebensunterhalt, Unterkategorie
  „Auto" erfasst, **dann** antwortet die API mit 201, und die Karte „Einträge" zeigt unter „Ausgaben" →
  „Lebensunterhalt" eine Gruppe „Auto" mit „Wartung" und der Summe 400,00 € (UK-1, UK-10 bis UK-12).
- **Gegeben** Ausgaben „Wartung" 400,00 €, „Steuer" 150,00 € und „Sprit" 1.200,00 €, alle Lebensunterhalt und „Auto",
  sowie „Miete" 9.600,00 €, Lebensunterhalt, Unterkategorie „Miete", **dann** zeigt die Gruppe „Auto" 1.750,00 €, die
  Gruppe „Miete" 9.600,00 €, „Auto" steht vor „Miete", und Lebensunterhalt zeigt 11.350,00 € (UK-11, UK-12).
- **Gegeben** eine Ausgabe mit „Auto" und eine mit „auto" in derselben Kategorie, **dann** stehen beide in einer
  Gruppe, die so heißt wie beim älteren Posten (UK-5).
- **Gegeben** eine Ausgabe „Ausflug" in Freizeit & Spaß mit „Auto", **dann** steht sie in einer eigenen Gruppe „Auto"
  unter Freizeit & Spaß, und die Auswertung nach der 50/30/20-Regel ändert sich durch die Unterkategorie nicht (UK-3).
- **Gegeben** Einnahmen „Gehalt" mit Unterkategorie „Gehalt" und Ausgaben in allen drei Kategorien, **dann** steht der
  Bereich „Einnahmen" über „Ausgaben", und die Kategorien folgen in der Reihenfolge Lebensunterhalt, Freizeit & Spaß,
  Sparen & Investieren, jeweils in ihrer Farbe (UK-9, UK-10).
- **Gegeben** ein Posten ohne Unterkategorie, **dann** steht er in der Gruppe „Ohne Unterkategorie" am Ende seiner
  Kategorie bzw. der Einnahmen (UK-1, UK-11).
- **Gegeben** das Formular zum Erfassen einer Ausgabe, **wenn** der Benutzer „Au" in das Feld „Unterkategorie
  (optional)" tippt, **dann** werden „Auto" und weitere passende Vorschläge angeboten; ein eigener Name wie „Boot" wird
  ebenfalls übernommen (UK-6, UK-7).
- **Gegeben** ein Plan, in dem schon die Unterkategorie „Boot" verwendet wird, **wenn** der Benutzer eine weitere
  Ausgabe erfasst, **dann** wird „Boot" vorgeschlagen (UK-7).
- **Gegeben** ein Posten mit Unterkategorie „Auto", **wenn** der Benutzer „Bearbeiten" wählt, **dann** zeigt der
  Dialog „Auto"; nach dem Ändern auf „Mobilität" und „Speichern" steht der Posten in der Gruppe „Mobilität", nach dem
  Leeren des Feldes in „Ohne Unterkategorie" (UK-4).
- **Gegeben** ein gelöschter Posten mit Unterkategorie, **wenn** der Benutzer „Rückgängig" wählt, **dann** erscheint
  der Posten wieder in seiner Gruppe (UK-4).
- **Gegeben** ein Posten, **wenn** eine Unterkategorie mit mehr als 50 Zeichen erfasst oder gespeichert werden soll,
  **dann** antwortet die API mit 400, und die Oberfläche zeigt „Höchstens 50 Zeichen.", ohne die Anfrage zu senden
  (UK-2).
- **Gegeben** ein Posten, **wenn** nur die Unterkategorie geändert wird, **dann** wird ein Event `BudgetItemChanged`
  protokolliert (UK-4).

## Nicht im Umfang

- Eigene Kategorien der 50/30/20-Regel oder andere Zielanteile
- Mehrere Unterkategorien je Posten, verschachtelte Unterkategorien
- Unterkategorien über alle Pläne hinweg verwalten, umbenennen oder löschen; eine Unterkategorie existiert nur, solange
  ein Posten sie trägt
- Eine Summe je Unterkategorie über alle Kategorien hinweg (z. B. „Auto" in Lebensunterhalt und Freizeit & Spaß
  zusammen)
- Unterkategorien in der Torte oder in den Balken der Auswertung
- Auf- und Zuklappen von Gruppen, Sortierung oder Filter durch den Benutzer

## Umsetzung

Noch nicht umgesetzt. Geplant:

- Domäne: Value Object `BudgetItemSubcategory` (UK-2, UK-5), optionales Feld in `BudgetItem`; `HouseholdPlan.addIncome`,
  `addExpense` und `changeItem` nehmen es entgegen; `BudgetSummary` bleibt unverändert (UK-3)
- Persistenz: Spalte `budget_item.subcategory VARCHAR(50)` (Migration `V7__budget_item_subcategory.sql`)
- Read Model: `HouseholdPlanOverview` liefert zusätzlich die gruppierte Sicht mit Summen je Bereich, Kategorie und
  Unterkategorie (UK-9 bis UK-13)
- API: optionales Feld `subcategory` in `AddIncomeRequest`, `AddExpenseRequest`, `ChangeBudgetItemRequest` und
  `BudgetItemResponse`; Vertrag `plans.json` neu schreiben
- Frontend: Feld „Unterkategorie (optional)" mit Vorschlägen (`mat-autocomplete`) in Formular und Dialog; die Karte
  „Einträge" zeigt die Gruppen statt der flachen Tabelle, Farben aus der Torte
- E2E: `e2e/tests/unterkategorie.spec.ts`

## Offene Fragen

1. Einnahmen haben keine Kategorie der 50/30/20-Regel (PO-5). Vorschlag: Im Bereich „Einnahmen" werden die Posten
   direkt nach Unterkategorie gruppiert, ohne Kategorien dazwischen (UK-11). Passt das?
2. Ist die Unterkategorie optional, mit einer Gruppe „Ohne Unterkategorie" für Posten ohne (so geplant, UK-1, UK-11),
   oder soll sie Pflicht sein? Bestehende Posten haben keine.
3. Sollen „auto" und „Auto" als dieselbe Unterkategorie gelten (so geplant, UK-5)?
4. Passt die Liste der vordefinierten Unterkategorien (UK-8)?
5. Die Spalten „Art" und „Kategorie" der heutigen Tabelle entfallen, weil die Gruppen sie zeigen. Einverstanden?
