# Posten markieren: wichtig und Sparpotential

Status: umgesetzt

## Zweck

Beim Durchgehen eines Plans fallen dem Benutzer zwei Dinge auf, die er heute nirgends festhalten kann: Manche Posten
sind ihm besonders wichtig und sollen in der langen Liste sofort ins Auge fallen. Bei anderen Ausgaben sieht er
Sparpotential, etwa bei einem Abo, das er kündigen könnte. Wie viel Geld insgesamt in diesen Ausgaben steckt, muss er
heute im Kopf zusammenrechnen.

Mit dieser Spec kann der Besitzer eines Plans Posten in der Karte „Einträge" als „wichtig" und als „Sparpotential"
markieren. Wichtige Posten sind farblich hervorgehoben, und die Bilanz zeigt das gesamte Sparpotential je Monat und
Jahr.

## Fachliche Regeln

### Markierungen

- **PM-1**: Jeder Posten kann als „wichtig" markiert sein. Ein neuer Posten ist nicht wichtig.
- **PM-2**: Jede Ausgabe kann ein Sparpotential haben: den Teil ihres Betrags, den der Benutzer für einsparbar hält.
  Einnahmen haben kein Sparpotential. Eine neue Ausgabe hat kein Sparpotential.
- **PM-2a**: Das Sparpotential ist immer ein Monatsbetrag, auch bei Ausgaben mit jährlicher Angabe: Es zählt, was im
  Monat mehr zur Verfügung stünde. Es ist größer als 0,00 €, hat höchstens zwei Nachkommastellen und ist höchstens so
  groß wie der Monatsbetrag der Ausgabe (MJ-2). Sonst wird es abgelehnt.
- **PM-3**: Beide Markierungen sind voneinander unabhängig; ein Posten kann wichtig sein, Sparpotential haben, beides
  oder keins.
- **PM-4**: Markiert und entmarkiert wird nur in der Karte „Einträge", direkt am Posten, mit je einem Knopf
  „Wichtig" und „Sparpotential" (letzterer nur bei Ausgaben). Das Formular „Neuer Eintrag", der Dialog „Neuer Eintrag
  in ‹Gruppe›" (EG-10) und der Dialog „Eintrag bearbeiten" (PB-1) haben keine Felder dafür. Wie alle Werte eines
  Postens ändert sie nur der Besitzer (PO-6).
- **PM-4a**: „Wichtig" ist ein Umschalter: Ein Klick setzt oder entfernt die Markierung ohne Rückfrage und speichert
  sofort.
- **PM-4b**: „Sparpotential" öffnet den Dialog „Sparpotential bei ‹Beschreibung›" mit dem Feld „Betrag pro Monat".
  Ohne Sparpotential ist der Monatsbetrag der Ausgabe vorausgefüllt, sonst das bisherige Sparpotential. „Speichern"
  prüft PM-2a (Meldung „Höchstens ‹Monatsbetrag der Ausgabe›." bzw. wie bei PO-4) und speichert; „Entfernen" (nur
  bei vorhandenem Sparpotential) entfernt es; „Abbrechen" und Escape schließen den Dialog ohne Änderung.
- **PM-5**: Bearbeiten (PB-1) lässt beide Markierungen unverändert. Ist das Sparpotential danach größer als der
  Monatsbetrag der Ausgabe, wird es auf diesen Monatsbetrag gesenkt.
- **PM-6**: Wird das Löschen eines Postens rückgängig gemacht (PO-9), hat er wieder seine Markierungen. Beim Übernehmen
  als Vorlage (PU-3) werden die Markierungen mit übernommen.
- **PM-7**: Das Setzen oder Entfernen einer Markierung wird protokolliert (`BudgetItemMarked`), mit Posten und
  Markierung. Die Reihenfolge der Posten ändert sich dadurch nicht.
- **PM-8**: Die Markierungen haben keinen Einfluss auf die Auswertung nach der 50/30/20-Regel.

### Darstellung

- **PM-9**: Ein wichtiger Posten ist in der Karte „Einträge" farblich hervorgehoben: Seine Zeile hat einen getönten
  Hintergrund und einen farbigen Streifen am linken Rand, in hellem und dunklem Farbschema gut lesbar. Der Umschalter
  „Wichtig" zeigt dann einen gefüllten Stern, sonst einen leeren.
- **PM-10**: Der Knopf „Sparpotential" zeigt bei Ausgaben mit Sparpotential ein hervorgehobenes Sparschwein, sonst ein
  blasses. Unter der Beschreibung steht dann klein „Sparpotential: ‹Betrag› im Monat" (wie die Notiz, PN-7). Die Zeile
  selbst wird dafür nicht eingefärbt.
- **PM-11**: Beide Knöpfe haben einen Tooltip und für Screenreader eine Bezeichnung: „‹Beschreibung› als wichtig
  markieren" bzw. „Markierung wichtig bei ‹Beschreibung› entfernen" (Zustand per `aria-pressed`) und „Sparpotential
  bei ‹Beschreibung›".
- **PM-12**: Auf breiter Karte stehen die beiden Knöpfe zwischen Beschreibung und Beträgen. Auf schmaler Karte (MA-2)
  stehen sie in der unteren Zeile des Postens links neben Monats- und Jahresbetrag, sodass die Beschreibung oben ihre
  Breite behält. Die Beschreibung bricht weiterhin nur zwischen Wörtern um (MA-4), und die Seite ist ab 360 px nicht
  breiter als der Bildschirm (MA-1).

### Sparpotential gesamt

- **PM-13**: Das monatliche Sparpotential eines Plans ist die Summe aller Sparpotentiale; das jährliche ist das
  Zwölffache davon. Das Backend rechnet beide.
- **PM-14**: Der Kasten „Bilanz" (BI-1) hat unter dem Saldo eine Zeile „Sparpotential" mit Monats- und Jahreswert,
  von den Zeilen darüber abgesetzt und nicht in die Saldo-Rechnung einbezogen. Ohne markierte Ausgaben zeigt sie
  0,00 €.

## Akzeptanzkriterien

- **Gegeben** ein Plan mit der Ausgabe „Miete", **wenn** der Benutzer in der Karte „Einträge" bei „Miete" den Stern
  wählt, **dann** ist die Zeile „Miete" hervorgehoben, und nach dem Neuladen der Seite ist sie es immer noch; ein
  weiterer Klick hebt die Hervorhebung auf (PM-1, PM-4, PM-9).
- **Gegeben** das Formular „Neuer Eintrag", der Dialog „Neuer Eintrag in ‹Gruppe›" und der Dialog „Eintrag
  bearbeiten", **dann** hat keiner ein Feld „Wichtig" oder „Sparpotential" (PM-4).
- **Gegeben** die Ausgabe „Fitnessstudio" 360,00 € jährlich, **wenn** der Benutzer dort „Sparpotential" wählt,
  **dann** öffnet sich „Sparpotential bei Fitnessstudio" mit 30,00 € als Betrag pro Monat (PM-2a, PM-4b).
- **Gegeben** die Ausgaben „Streaming" 15,00 € monatlich und „Fitnessstudio" 360,00 € jährlich, **wenn** der Benutzer
  bei „Streaming" 5,00 € und bei „Fitnessstudio" den vorausgefüllten Betrag speichert, **dann** steht unter
  „Streaming" „Sparpotential: 5,00 € im Monat", und die Bilanz zeigt in der Zeile „Sparpotential" 35,00 € im Monat und
  420,00 € im Jahr; Saldo und 50/30/20-Auswertung bleiben unverändert (PM-2, PM-2a, PM-8, PM-10, PM-13, PM-14).
- **Gegeben** derselbe Plan, **wenn** der Benutzer bei „Streaming" das Sparpotential mit „Entfernen" löscht, **dann**
  zeigt die Bilanz 30,00 € und 360,00 € (PM-4b, PM-13).
- **Gegeben** „Streaming" 15,00 € monatlich, **wenn** der Benutzer 20,00 € als Sparpotential speichern will,
  **dann** zeigt der Dialog „Höchstens 15,00 €.", und nichts wird gespeichert; die API lehnt das mit 400 ab
  (PM-2a).
- **Gegeben** die Einnahme „Gehalt", **dann** hat sie einen Umschalter „Wichtig", aber keinen für Sparpotential
  (PM-2, PM-4).
- **Gegeben** „Streaming" 15,00 € monatlich mit Sparpotential 5,00 €, **wenn** der Benutzer den Betrag auf 20,00 €
  ändert, **dann** bleibt das Sparpotential 5,00 €; ändert er ihn auf 36,00 € jährlich, sinkt es auf 3,00 € (PM-5).
- **Gegeben** eine wichtige Ausgabe mit Sparpotential, **wenn** der Benutzer sie löscht und „Rückgängig" wählt,
  **dann** erscheint sie wieder hervorgehoben und mit Sparpotential (PM-6).
- **Gegeben** ein Plan mit wichtigen Posten und Ausgaben mit Sparpotential, **wenn** er als Vorlage übernommen wird,
  **dann** hat der neue Plan dieselben Markierungen (PM-6).
- **Gegeben** eine Einnahme, **wenn** per API Sparpotential gesetzt werden soll, **dann** antwortet die API mit 400
  (PM-2).
- **Gegeben** ein Plan auf einem 390 px breiten Bildschirm, **dann** sind bei jedem Posten beide Umschalter sichtbar,
  und die Seite ist nicht breiter als der Bildschirm (PM-12).

## Nicht im Umfang

- Sparpotential je Gruppe oder Kategorie in den Summenzeilen der Karte „Einträge"; je Unterkategorie steht es seit
  [sparpotential-unterkategorie.md](sparpotential-unterkategorie.md) in einem Kasten neben der Bilanz und in der
  Summenzeile jeder Unterkategorie-Gruppe
- Filtern oder Sortieren nach Markierungen, alle Posten auf einmal markieren
- Sparpotential in der Jahresübersicht
- Weitere oder selbst definierte Markierungen und Farben

## Umsetzung

- Domäne: Value Objects `ItemMarks` (wichtig, Sparpotential) und `SavingPotential` (Monatsbetrag, positiv;
  `exceeds`, `cappedTo`, `annual`) sowie `ItemMark` für das Protokoll. `BudgetItem` trägt die Markierungen und prüft
  PM-2 und PM-2a; `changedTo` behält sie und senkt das Sparpotential (PM-5), `copy` übernimmt sie (PM-6).
  `HouseholdPlan.markImportant` und `changeSavingPotential` ändern sie und zeichnen `BudgetItemMarked` auf, ohne die
  Reihenfolge anzutasten (PM-4, PM-7); `addIncome`/`addExpense` nehmen Markierungen für „Rückgängig" entgegen (PM-6).
  `BudgetSummary` kennt sie nicht (PM-8)
- Commands: `MarkBudgetItemImportantCommand`, `ChangeSavingPotentialCommand`; `AddIncomeCommand` und
  `AddExpenseCommand` mit optionalen `ItemMarks`
- Persistenz: Spalten `budget_item.important` und `budget_item.saving_potential` (Migration
  `V9__budget_item_marks.sql`, nur für Ausgaben), gelesen und geschrieben über `ItemMarksColumns`
- Read Model: `OverviewItem.marks`, `HouseholdPlanOverview.monthlySavingPotential()` und `savingPotential()` (PM-13)
- API: `PUT /api/plans/{planId}/items/{itemId}/important` (`{"important": true}`),
  `PUT …/saving-potential` (`{"monthlyAmount": "5.00"}`, 400 bei Einnahmen oder zu großem Betrag) und
  `DELETE …/saving-potential`, jeweils mit der Übersicht als Antwort. `BudgetItemResponse` liefert `important` und
  `savingPotential`, `HouseholdPlanOverviewResponse` `monthlySavingPotential` und `savingPotential`; `AddIncomeRequest`
  und `AddExpenseRequest` nehmen optional `important` bzw. `savingPotential` an, damit „Rückgängig" sie wiederherstellt
- Frontend: `budget/ui/budget-entries` zeigt Stern und Sparschwein je Posten, hebt wichtige Zeilen hervor und zeigt
  das Sparpotential unter der Beschreibung (PM-9 bis PM-12; auf schmaler Karte sind die Betragsspalten dafür 6 rem
  statt 7,5 rem breit und die Knöpfe etwas kleiner, siehe MA-7; Farben `--app-important-color` und
  `--app-important-background` in `styles.scss`); `budget/ui/saving-potential-dialog.ts` ist der Dialog (PM-4b);
  `BudgetStore.markImportant`, `changeSavingPotential` und `restoreItem` (PM-6); `budget/ui/overview-summary.ts` zeigt
  die Zeile „Sparpotential" (PM-14)
- Tests: `SavingPotentialTest`, `HouseholdPlanMarksTest`, `BudgetItemMarksControllerTest`,
  `JdbcHouseholdPlanRepositoryTest`, `JdbcHouseholdPlanOverviewQueryHandlerTest`, `BudgetItemIntegrationTest`,
  `budget-entries.spec.ts`, `saving-potential-dialog.spec.ts`, `budget-store.spec.ts`, `overview-summary.spec.ts`
- E2E: `e2e/tests/posten-markierungen.spec.ts` (PM-1, PM-2, PM-2a, PM-4 bis PM-6, PM-8 bis PM-10, PM-12 bis PM-14;
  PM-3, PM-7 und PM-11 decken die Backend- und Unit-Tests ab); `bilanz.spec.ts` erwartet die neue Zeile

## Offene Fragen

Keine. Entschieden bei der Freigabe: sichtbare Knöpfe statt Menü (PM-4), Sparpotential nur in der Bilanz (PM-14), nur
für Ausgaben (PM-2), als Teilbetrag und immer monatlich, weil der Monat die maßgebliche Kenngröße ist (PM-2a), sinkt
beim Bearbeiten automatisch mit (PM-5), Markierungen gehen beim Übernehmen mit (PM-6).
