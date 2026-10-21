# Sparpotential je Unterkategorie

Status: umgesetzt

## Zweck

Die Bilanz zeigt das gesamte Sparpotential eines Plans (PM-14 in [posten-markierungen.md](posten-markierungen.md)).
Wo es steckt, sieht der Benutzer daran nicht: ob vor allem bei den Abonnements, beim Auto oder beim Essen und Trinken.
Dafür muss er heute die Karte „Einträge" durchgehen und die Beträge unter den Posten selbst zusammenzählen.

Mit dieser Spec steht links neben der Bilanz eine Liste „Sparpotential je Unterkategorie": für jede Unterkategorie,
in der Ausgaben mit Sparpotential stecken, die Summe ihres Sparpotentials je Monat und je Jahr. Sie ersetzt für diese
Liste das Nicht-Ziel „Sparpotential je Gruppe oder Kategorie" in [posten-markierungen.md](posten-markierungen.md); in
den Summenzeilen der Karte „Einträge" bleibt es weiterhin weg.

## Fachliche Regeln

### Summen

- **SU-1**: Das Sparpotential einer Unterkategorie ist die Summe der Sparpotentiale (PM-2a) aller Ausgaben dieser
  Unterkategorie, als Monatsbetrag; der Jahreswert ist das Zwölffache. Das Backend rechnet beide.
- **SU-2**: Unterkategorien werden über alle Kategorien der 50/30/20-Regel hinweg zusammengefasst: „Auto" unter
  Lebensunterhalt und „Auto" unter Freizeit & Spaß ergeben eine Zeile „Auto" (UK-3). Gleich sind Namen ohne Beachtung
  der Groß- und Kleinschreibung (UK-5); angezeigt wird die Schreibweise, mit der die Unterkategorie zuerst in der Karte
  „Einträge" steht. Ausgaben ohne Unterkategorie zählen zu „Sonstiges" (UK-1).
- **SU-3**: Aufgeführt sind nur Unterkategorien, deren Sparpotential größer als 0,00 € ist. Die Summe aller Zeilen ist
  das Sparpotential des Plans in der Bilanz (PM-13).
- **SU-4**: Die Zeilen sind nach dem monatlichen Sparpotential geordnet, das größte oben; bei gleichem Betrag
  alphabetisch nach Namen.
- **SU-5**: Die Liste hat keinen Einfluss auf Saldo und 50/30/20-Auswertung (PM-8).

### Darstellung

- **SU-6**: Die Liste ist ein eigener Kasten „Sparpotential je Unterkategorie" links neben dem Kasten „Bilanz" (BI-1),
  oben bündig mit ihm. Jede Zeile zeigt links den Namen der Unterkategorie und rechtsbündig daneben den Monats- und den
  Jahreswert, in den Spalten „Monat" und „Jahr" wie in der Bilanz (BI-2).
- **SU-7**: Ohne Ausgaben mit Sparpotential entfällt der Kasten; die Bilanz steht dann allein wie bisher, mit der Zeile
  „Sparpotential" 0,00 € (PM-14).
- **SU-8**: Ist neben der Bilanz kein Platz für den Kasten (auf dem Handy, auch mit vergrößerter Schrift), steht er
  unter der Bilanz. Ist der Kasten selbst zu schmal für Name, Monats- und Jahreswert nebeneinander, steht der Name
  oben und die beiden Beträge darunter. Lange Namen brechen nur zwischen Wörtern um (MA-4), und die Seite ist ab 360 px
  nicht breiter als der Bildschirm (MA-1).
- **SU-9**: Jede Änderung eines Sparpotentials oder einer Ausgabe (Betrag, Unterkategorie, Löschen, Rückgängig) zeigt
  sich sofort in der Liste, ohne Neuladen (AU-1).

## Akzeptanzkriterien

- **Gegeben** die Ausgaben „Streaming" 15,00 € monatlich (Unterkategorie „Abonnements", Sparpotential 5,00 €),
  „Zeitung" 20,00 € monatlich („abonnements", Sparpotential 10,00 €) und „Fitnessstudio" 360,00 € jährlich
  (Unterkategorie „Hobbys", Sparpotential 30,00 €), **dann** zeigt der Kasten „Sparpotential je Unterkategorie" die
  Zeilen „Hobbys" 30,00 € im Monat und 360,00 € im Jahr und darunter „Abonnements" 15,00 € und 180,00 € (SU-1, SU-2,
  SU-4, SU-6).
- **Gegeben** „Tanken" in Lebensunterhalt und „Ausflüge" in Freizeit & Spaß, beide mit Unterkategorie „Auto" und je
  10,00 € Sparpotential, **dann** zeigt die Liste eine Zeile „Auto" mit 20,00 € im Monat (SU-2).
- **Gegeben** eine Ausgabe ohne Unterkategorie mit Sparpotential 4,00 € und eine Ausgabe „Miete" ohne Sparpotential
  in der Unterkategorie „Miete", **dann** zeigt die Liste „Sonstiges" 4,00 € und keine Zeile „Miete" (SU-2, SU-3).
- **Gegeben** ein Plan ohne Sparpotential, **dann** gibt es keinen Kasten „Sparpotential je Unterkategorie"; setzt
  der Benutzer bei einer Ausgabe ein Sparpotential, erscheint er sofort, entfernt er es, verschwindet er wieder
  (SU-7, SU-9).
- **Gegeben** ein breiter Bildschirm und ein Plan mit Sparpotential, **dann** steht der Kasten links neben der Bilanz,
  beide oben auf gleicher Höhe (SU-6).
- **Gegeben** ein 390 px breiter Bildschirm, **dann** steht der Kasten unter der Bilanz, und die Seite ist nicht
  breiter als der Bildschirm (SU-8).

## Nicht im Umfang

- Sparpotential je Kategorie der 50/30/20-Regel oder in den Summenzeilen der Karte „Einträge"
- Klicken auf eine Zeile, um die Posten der Unterkategorie zu zeigen oder zu filtern
- Sparpotential in der Jahresübersicht oder in der Torte
- Sortieren der Liste durch den Benutzer

## Umsetzung

- Read Model: `SubcategorySavingPotential` (Unterkategorie, Monatsbetrag, `annual`) und
  `HouseholdPlanOverview.savingPotentialsBySubcategory()`: summiert die Sparpotentiale der Gruppen aller Kategorien,
  fasst gleiche Unterkategorien über `BudgetItemSubcategory.isSameAs` zusammen, lässt Summen von 0,00 € weg und ordnet
  nach Betrag, dann Name (SU-1 bis SU-4)
- API: `HouseholdPlanOverviewResponse.savingPotentialsBySubcategory` mit `SubcategorySavingPotentialResponse`
  (`subcategory`, `monthlyAmount`, `amount`); kommt mit jeder Übersicht, also auch nach jeder Änderung (SU-9, AU-1)
- Frontend: `budget/ui/saving-potential-summary.ts` ist der Kasten mit der Tabelle; unter 22em Kartenbreite (Container-
  Abfrage) steht der Name über den Beträgen (SU-8). `budget/ui/overview-summary.ts` stellt ihn nur bei vorhandenem
  Sparpotential neben die Bilanz (SU-7): Flexbox in umgekehrter Reihenfolge mit Umbruch, sodass er auf breitem
  Bildschirm links und auf schmalem unter der Bilanz steht (SU-6, SU-8)
- Tests: `HouseholdPlanOverviewSavingPotentialTest`, `BudgetItemMarksControllerTest`, `saving-potential-summary.spec.ts`,
  `overview-summary.spec.ts`
- E2E: `e2e/tests/sparpotential-unterkategorie.spec.ts` (SU-1 bis SU-4, SU-6 bis SU-9; SU-5 folgt aus PM-8 und ist dort
  abgedeckt). `openSavingPotential`, `setSavingPotential` und `savingPotentialOfSubcategory` stehen in `support.ts`;
  `balanceSheet` liest nur noch aus dem Kasten „Bilanz"

## Offene Fragen

Keine. Entschieden bei der Freigabe: eigener Kasten links neben der Bilanz statt weiterer Zeilen in ihr (SU-6), auf
dem Handy darunter (SU-8), Unterkategorien über alle Kategorien zusammengefasst (SU-2), nach Betrag geordnet (SU-4),
nur Unterkategorien mit Sparpotential (SU-3), Monat und Jahr wie in der Bilanz, Monat als maßgebliche Kenngröße
links (SU-1, SU-6).
