# Unterkategorie und gruppierte Einträge

Status: umgesetzt

## Zweck

Die Kategorien der 50/30/20-Regel sind grob: Unter Lebensunterhalt stehen Miete, Lebensmittel, Auto und Versicherungen
nebeneinander, und die Tabelle der Einträge zeigt alle Posten in einer langen Liste. Wer wissen will, was ihn „das
Auto" im Jahr kostet (Wartung, Steuer, Sprit, Versicherung), muss die Beträge selbst zusammenrechnen.

Mit dieser Spec kann der Besitzer eines Plans jedem Posten zusätzlich zur Kategorie eine frei benennbare
Unterkategorie geben, z. B. „Auto". Die Anwendung schlägt übliche Unterkategorien vor. Die Einträge werden nach
Einnahmen und Ausgaben, nach Kategorie und nach Unterkategorie gruppiert angezeigt, jeweils mit Summe. Die
Reihenfolge der Unterkategorien und der Posten darin kann der Benutzer selbst festlegen.

Diese Spec ersetzt das Nicht-Ziel „Eigene Kategorien oder Unterkategorien" in [posten.md](posten.md) zur Hälfte:
Unterkategorien gibt es danach, eigene Kategorien der 50/30/20-Regel weiterhin nicht. Für die Karte „Einträge"
ersetzt sie außerdem die feste Reihenfolge nach PO-10 durch die frei wählbare Reihenfolge (UK-14 bis UK-18), und sie
legt die Farben der Kategorien neu fest (UK-19), auch für die Torte (TD-2).

## Fachliche Regeln

### Unterkategorie am Posten

- **UK-1**: Jeder Posten, Einnahme wie Ausgabe, kann eine Unterkategorie haben. Die Unterkategorie ist optional; ein
  Posten ohne Unterkategorie gehört zur Unterkategorie „Sonstiges". Das gilt auch für alle bestehenden Posten.
- **UK-2**: Eine Unterkategorie ist ein frei wählbarer Name mit 1 bis 50 Zeichen. Führende und abschließende
  Leerzeichen werden entfernt; ein leerer Name gilt als „keine Unterkategorie", der Posten gehört also zu „Sonstiges"
  (UK-1). Ein längerer Name wird abgelehnt,
  nicht gekürzt.
- **UK-3**: Die Unterkategorie ist unabhängig von der Kategorie der 50/30/20-Regel (PO-5): Dieselbe Unterkategorie,
  z. B. „Auto", kann bei Ausgaben verschiedener Kategorien vorkommen. Sie hat keinen Einfluss auf die Auswertung nach
  der 50/30/20-Regel.
- **UK-4**: Die Unterkategorie wird beim Erfassen eines Postens angegeben und beim Bearbeiten geändert oder entfernt
  (PB-1). Eine Änderung ist eine Änderung des Postens im Sinne von PB-5 und wird als `BudgetItemChanged` protokolliert.
  Wird das Löschen rückgängig gemacht (PO-9), wird auch die Unterkategorie wiederhergestellt.
- **UK-5**: Zwei Unterkategorien sind dieselbe, wenn ihre Namen ohne Beachtung der Groß- und Kleinschreibung gleich
  sind („auto" und „Auto"). Angezeigt wird die Schreibweise des ältesten Postens der Gruppe; Posten ohne
  Unterkategorie gehören zu „Sonstiges" (UK-1), das immer so geschrieben angezeigt wird.

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
  „Sonstiges" ist für beide Arten vordefiniert; wer es wählt, landet in derselben Gruppe wie Posten ohne
  Unterkategorie.

### Gruppierte Anzeige der Einträge

- **UK-9**: Die Karte „Einträge" gliedert die Posten zuerst nach Art: oben „Einnahmen", darunter „Ausgaben". Ein
  Bereich ohne Posten entfällt.
- **UK-10**: Im Bereich „Ausgaben" folgen die Kategorien in der festen Reihenfolge Lebensunterhalt, Freizeit & Spaß,
  Sparen & Investieren. Eine Kategorie ohne Posten entfällt. Bereiche und Kategorien sind farblich unterschieden
  (UK-19).
- **UK-11**: Innerhalb einer Kategorie bzw. im Bereich „Einnahmen" sind die Posten nach Unterkategorie gruppiert, in
  der Reihenfolge nach UK-14 bis UK-18. Die Spalten „Art" und „Kategorie" der bisherigen Tabelle entfallen, weil die
  Gruppen sie zeigen.
- **UK-12**: Jeder Bereich, jede Kategorie und jede Unterkategorie-Gruppe zeigt die Summe ihrer Posten, seit
  [eintraege-gruppen.md](eintraege-gruppen.md) in einer Zeile unter den Posten (EG-5, EG-6). Die Summe von „Einnahmen"
  ist das Einkommen, die von „Ausgaben" die Ausgaben (AW-1). Die Summe einer Kategorie enthält nur die erfassten
  Ausgaben, nicht den Überschuss (AW-10); der steht weiterhin nur in der Auswertung.
- **UK-13**: Alle Summen der gruppierten Anzeige rechnet das Backend (wie AW-9); die Oberfläche addiert nichts selbst.

### Reihenfolge

- **UK-14**: Der Benutzer kann die Unterkategorie-Gruppen innerhalb einer Kategorie bzw. im Bereich „Einnahmen" frei
  umsortieren, ebenso die Posten innerhalb einer Gruppe. Er verschiebt dazu eine Gruppe oder einen Posten per Ziehen und
  Ablegen an einem Griff; eine Gruppe greift er außerdem an ihrer ganzen Kopfzeile mit dem Namen. Die Bereiche
  „Einnahmen" und „Ausgaben" und die Kategorien behalten ihre feste Reihenfolge (UK-9, UK-10), und ein Posten wechselt
  durch Verschieben nicht seine Gruppe; dafür ändert der Benutzer seine Unterkategorie (UK-4).
- **UK-15**: Die Reihenfolge gehört zum Plan und wird gespeichert: Sie gilt nach dem Neuladen, nach erneuter Anmeldung
  und auf anderen Geräten. Jeder Plan (jedes Jahr) hat seine eigene Reihenfolge. Wie die übrigen Werte des Plans ändert
  sie nur der Besitzer (PO-6).
- **UK-16**: Solange der Benutzer nichts verschoben hat, stehen die Gruppen alphabetisch mit „Sonstiges" am Ende, und
  die Posten einer Gruppe stehen nach PO-10.
- **UK-17**: Ein neuer Posten steht unten in seiner Gruppe (EG-1; bis zu [eintraege-gruppen.md](eintraege-gruppen.md)
  oben). Eine neue Gruppe (der erste Posten mit einer Unterkategorie, die es in der Kategorie bzw. bei den Einnahmen
  noch nicht gibt) steht am Ende, vor „Sonstiges", sofern „Sonstiges" am Ende steht; mehrere neue Gruppen stehen
  untereinander alphabetisch. Ein bearbeiteter Posten behält seinen Platz (PB-4); bekommt er eine andere Unterkategorie
  oder Kategorie, steht er unten in seiner neuen Gruppe (EG-2). Ein durch „Rückgängig" wiederhergestellter Posten (PO-9)
  steht wie ein neuer Posten unten in seiner Gruppe (EG-3).
- **UK-18**: Verschwindet der letzte Posten einer Gruppe, verschwindet die Gruppe; kommt sie später wieder, gilt sie als
  neue Gruppe (UK-17).

### Farben

- **UK-19**: Die Bereiche und Kategorien haben feste Farben, in hellem und dunklem Farbschema jeweils gut lesbar:
  Einnahmen grün, Lebensunterhalt ein sanftes Braun, Freizeit & Spaß ein Mango-Senfgelb, Sparen & Investieren blau.
  Die Torte und ihre Legende verwenden für die Kategorien dieselben Farben (TD-2).

## Akzeptanzkriterien

- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Wartung", 400,00 €, Lebensunterhalt, Unterkategorie
  „Auto" erfasst, **dann** antwortet die API mit 201, und die Karte „Einträge" zeigt unter „Ausgaben" →
  „Lebensunterhalt" eine Gruppe „Auto" mit „Wartung" und der Summe 400,00 € (UK-1, UK-10 bis UK-12).
- **Gegeben** Ausgaben „Wartung" 400,00 €, „Steuer" 150,00 € und „Sprit" 1.200,00 €, alle Lebensunterhalt und „Auto",
  sowie „Miete" 9.600,00 €, Lebensunterhalt, Unterkategorie „Miete", **dann** zeigt die Gruppe „Auto" 1.750,00 €, die
  Gruppe „Miete" 9.600,00 €, „Auto" steht vor „Miete", und Lebensunterhalt zeigt 11.350,00 € (UK-12, UK-16).
- **Gegeben** eine Ausgabe mit „Auto" und eine mit „auto" in derselben Kategorie, **dann** stehen beide in einer
  Gruppe, die so heißt wie beim älteren Posten (UK-5).
- **Gegeben** eine Ausgabe „Ausflug" in Freizeit & Spaß mit „Auto", **dann** steht sie in einer eigenen Gruppe „Auto"
  unter Freizeit & Spaß, und die Auswertung nach der 50/30/20-Regel ändert sich durch die Unterkategorie nicht (UK-3).
- **Gegeben** Einnahmen „Gehalt" mit Unterkategorie „Gehalt" und Ausgaben in allen drei Kategorien, **dann** steht der
  Bereich „Einnahmen" über „Ausgaben", und die Kategorien folgen in der Reihenfolge Lebensunterhalt, Freizeit & Spaß,
  Sparen & Investieren; Einnahmen sind grün, Lebensunterhalt braun, Freizeit & Spaß mango-senfgelb, Sparen &
  Investieren blau, und die Torte zeigt dieselben Farben (UK-9, UK-10, UK-19).
- **Gegeben** ein Posten ohne Unterkategorie und ein Posten mit der Unterkategorie „Sonstiges" in derselben Kategorie,
  **dann** stehen beide in der Gruppe „Sonstiges" am Ende der Kategorie (UK-1, UK-8, UK-16).
- **Gegeben** das Formular zum Erfassen einer Ausgabe, **wenn** der Benutzer „Au" in das Feld „Unterkategorie
  (optional)" tippt, **dann** werden „Auto" und weitere passende Vorschläge angeboten; ein eigener Name wie „Boot" wird
  ebenfalls übernommen (UK-6, UK-7).
- **Gegeben** ein Plan, in dem schon die Unterkategorie „Boot" verwendet wird, **wenn** der Benutzer eine weitere
  Ausgabe erfasst, **dann** wird „Boot" vorgeschlagen (UK-7).
- **Gegeben** ein Posten mit Unterkategorie „Auto", **wenn** der Benutzer „Bearbeiten" wählt, **dann** zeigt der
  Dialog „Auto"; nach dem Ändern auf „Mobilität" und „Speichern" steht der Posten in der Gruppe „Mobilität", nach dem
  Leeren des Feldes in „Sonstiges" (UK-1, UK-4).
- **Gegeben** ein gelöschter Posten mit Unterkategorie, **wenn** der Benutzer „Rückgängig" wählt, **dann** erscheint
  der Posten wieder in seiner Gruppe (UK-4).
- **Gegeben** ein Posten, **wenn** eine Unterkategorie mit mehr als 50 Zeichen erfasst oder gespeichert werden soll,
  **dann** antwortet die API mit 400, und die Oberfläche zeigt „Höchstens 50 Zeichen.", ohne die Anfrage zu senden
  (UK-2).
- **Gegeben** die Gruppen „Auto" und „Miete" in Lebensunterhalt, **wenn** der Benutzer „Miete" vor „Auto" zieht und
  die Seite neu lädt, **dann** steht „Miete" weiterhin vor „Auto" (UK-14, UK-15).
- **Gegeben** die Posten „Wartung", „Steuer" und „Sprit" in der Gruppe „Auto", **wenn** der Benutzer „Sprit" nach
  oben zieht und die Seite neu lädt, **dann** steht „Sprit" oben in der Gruppe (UK-14, UK-15).
- **Gegeben** eine umsortierte Gruppe „Auto", **wenn** der Benutzer dort einen neuen Posten erfasst, **dann** steht er
  unten in „Auto", und die übrigen Posten behalten ihre Reihenfolge (UK-17, EG-1).
- **Gegeben** die Gruppen „Auto" und „Sonstiges", **wenn** der Benutzer einen Posten mit der neuen Unterkategorie
  „Urlaub" erfasst, **dann** steht „Urlaub" nach „Auto" und vor „Sonstiges" (UK-17).
- **Gegeben** ein Posten in „Auto", **wenn** der Benutzer seine Unterkategorie auf „Miete" ändert, **dann** steht er
  unten in „Miete" (UK-17, EG-2).
- **Gegeben** die Pläne für 2026 und 2027, **wenn** der Benutzer im Plan 2026 umsortiert, **dann** bleibt die
  Reihenfolge im Plan 2027 unverändert (UK-15).
- **Gegeben** der Plan eines anderen Benutzers, **wenn** dessen Reihenfolge geändert werden soll, **dann** antwortet die
  API mit 404 (UK-15, HP-4).
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
- Auf- und Zuklappen von Gruppen, Filter
- Sortieren nach Betrag oder Name per Knopfdruck; die Reihenfolge legt der Benutzer von Hand fest (UK-14)
- Umsortieren der Bereiche und der Kategorien der 50/30/20-Regel

## Umsetzung

- Domäne: Value Object `BudgetItemSubcategory` (UK-2; `ofNullable` macht aus leerem Text „keine Unterkategorie",
  `isSameAs` vergleicht ohne Groß- und Kleinschreibung, `groupOf` ordnet Posten ohne Unterkategorie „Sonstiges" zu),
  optionales Feld in `BudgetItem`, Value Object `ItemSection` für die Einnahmen bzw. eine Kategorie der Ausgaben.
  `HouseholdPlan` hält die Posten in Anzeigereihenfolge: `addIncome` und `addExpense` setzen den neuen Posten ans
  Ende, `changeItem` setzt ihn nur bei einer neuen Gruppe dorthin (UK-17, EG-1, EG-2). Die Schreibweise wird beim Speichern
  vereinheitlicht: Ein Posten übernimmt die Schreibweise eines vorhandenen Postens der Gruppe, „sonstiges" wird immer
  als „Sonstiges" gespeichert; so zeigt die Gruppe die Schreibweise ihres ältesten Postens (UK-5). Die Reihenfolge der
  Gruppen hält je Bereich das Value Object `SubcategoryOrder`; `arrange` stellt erst die verschobenen Gruppen, dann
  die übrigen alphabetisch vor ein am Ende stehendes „Sonstiges" (UK-16, UK-17). `reorderItems` und
  `reorderSubcategories` prüfen, dass genau die Posten einer Gruppe bzw. die Gruppen eines Bereichs genannt sind, und
  zeichnen `BudgetItemsReordered` bzw. `SubcategoriesReordered` auf, wenn sich die Reihenfolge ändert (UK-14). Gruppen
  ohne Posten fallen aus der Reihenfolge (UK-18). `BudgetSummary` bleibt unverändert (UK-3)
- Commands: `AddIncomeCommand`, `AddExpenseCommand`, `ChangeBudgetItemCommand` mit optionaler Unterkategorie, neu
  `ReorderBudgetItemsCommand` und `ReorderSubcategoriesCommand`
- Persistenz: Migration `V7__budget_item_subcategory_and_order.sql` mit den Spalten `budget_item.subcategory
  VARCHAR(50)` und `budget_item.position` (höchste oben, vorhandene Posten nach Erfassungszeitpunkt) und der Tabelle
  `subcategory_order` (UK-15). `JdbcHouseholdPlanRepository` behält gespeicherte Positionen, solange sie zur
  Reihenfolge passen, sodass Erfassen, Löschen und Verschieben eines Postens nur eine Zeile schreiben
- Read Model: `HouseholdPlanOverview` liefert zusätzlich `incomeGroups` und `expenseCategories` (Read Models
  `OverviewItemGroup`, `OverviewCategoryItems`) mit den Summen je Kategorie und Gruppe (UK-9 bis UK-13)
- API: optionales Feld `subcategory` (höchstens 50 Zeichen, sonst 400) in `AddIncomeRequest`, `AddExpenseRequest`,
  `ChangeBudgetItemRequest` und `BudgetItemResponse`; `PUT /api/plans/{planId}/item-order` (`itemIds` der Posten
  einer Gruppe) und `PUT /api/plans/{planId}/subcategory-order` (`type`, bei Ausgaben `category`, `subcategories`)
  antworten mit der Übersicht (AU-4), bei einem fremden Plan mit 404 (UK-15)
- Frontend: `budget/ui/subcategory-field.ts` (Feld „Unterkategorie (optional)" mit `mat-autocomplete`, UK-6) in
  `add-item-form` und `edit-item-dialog`; Vorschläge aus `subcategorySuggestions` in `budget.model.ts` (UK-7, UK-8).
  `budget/ui/budget-entries.ts` ersetzt die Tabelle `budget-item-table.ts`: Bereiche, Kategorien und Gruppen mit Summen,
  Verschieben mit `@angular/cdk/drag-drop` am Griff (bei Gruppen an der ganzen Kopfzeile) oder mit den Pfeiltasten auf
  dem Griff (UK-14); `BudgetStore.reorderItems` und `reorderSubcategories` zeigen die neue Reihenfolge sofort und laden
  die Übersicht neu, wenn das Speichern scheitert. Farbvariablen `--app-income-color` und `--app-<kategorie>-color` in
  `styles.scss`, auch für die Torte (UK-19)
- Tests: `BudgetItemSubcategoryTest`, `SubcategoryOrderTest`, `HouseholdPlanSubcategoryTest`, `HouseholdPlanTest`,
  `HouseholdPlanOverviewGroupsTest`, `HouseholdPlanControllerTest`, `JdbcHouseholdPlanRepositoryTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `budget-entries.spec.ts`, `add-item-form.spec.ts`,
  `edit-item-dialog.spec.ts`, `budget-store.spec.ts`, `budget-api.spec.ts`, `budget-dashboard.spec.ts`
- E2E: `e2e/tests/unterkategorie.spec.ts` (UK-1 bis UK-7, UK-9 bis UK-12, UK-14 bis UK-17, UK-19; UK-13, UK-18, der
  Fall 404 und das Protokoll decken die Backend-Tests ab)

## Offene Fragen
