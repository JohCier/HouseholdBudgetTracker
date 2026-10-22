# Sparpotential je Unterkategorie

Status: umgesetzt

## Zweck

Die Bilanz zeigt das gesamte Sparpotential eines Plans (PM-14 in [posten-markierungen.md](posten-markierungen.md)).
Wo es steckt, sieht der Benutzer daran nicht: ob vor allem bei den Abonnements, beim Auto oder beim Essen und Trinken.
Dafür muss er heute die Karte „Einträge" durchgehen und die Beträge unter den Posten selbst zusammenzählen.

Mit dieser Spec steht links neben der Bilanz eine Liste „Sparpotential je Unterkategorie": für jede Unterkategorie,
in der Ausgaben mit Sparpotential stecken, die Summe ihres Sparpotentials je Monat und je Jahr. Sie ersetzt für diese
Liste das Nicht-Ziel „Sparpotential je Gruppe oder Kategorie" in [posten-markierungen.md](posten-markierungen.md).

Außerdem zeigt die Summenzeile jeder Unterkategorie in der Karte „Einträge" (EG-5) das Sparpotential ihrer Posten,
links neben dem Monatsbetrag (SU-10 bis SU-14). So sieht der Benutzer es auch dort, wo die Posten stehen; der Kasten
neben der Bilanz bleibt als Überblick.

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

### In den Summenzeilen der Einträge

- **SU-10**: Die Summenzeile „Summe ‹Gruppe›" einer Unterkategorie-Gruppe von Ausgaben (EG-5) zeigt das Sparpotential
  der Gruppe: die Summe der Sparpotentiale ihrer Posten, als Monatsbetrag. Das Backend rechnet es. Anders als im
  Kasten (SU-2) gilt es je Gruppe, also je Kategorie: „Auto" unter Lebensunterhalt und „Auto" unter Freizeit & Spaß
  haben jede ihren eigenen Wert.
- **SU-11**: Es steht links neben dem Monatsbetrag der Summenzeile, in der Spalte, in der bei den Posten darüber die
  Knöpfe „Wichtig" und „Sparpotential" stehen (PM-12), mit einem Sparschwein davor wie beim Knopf (PM-10). Es ist
  kleiner und blasser als die Summe selbst und nicht wie ein Ausgabenbetrag eingefärbt. Tooltip und Bezeichnung für
  Screenreader: „Sparpotential ‹Gruppe›: ‹Betrag› im Monat".
- **SU-12**: Ohne Sparpotential in der Gruppe bleibt die Stelle leer. Einnahmen-Gruppen und die Summenzeilen von
  Kategorie, Einnahmen und Ausgaben (EG-6) zeigen kein Sparpotential.
- **SU-13**: Auf schmaler Karte (MA-3) gehört es zu den Beträgen der Summenzeile: Es steht links neben dem
  Monatsbetrag und bricht mit beiden Beträgen gemeinsam in die zweite Zeile um. Monats- und Jahresbetrag bleiben in
  ihren Spalten, und die Seite ist ab 360 px nicht breiter als der Bildschirm, auch mit vergrößerter Schrift (MA-1,
  MA-7).
- **SU-14**: Wie alle Summen ändert es sich mit jeder Änderung eines Sparpotentials oder einer Ausgabe sofort (AU-1).

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

- **Gegeben** die Ausgaben „Streaming" 15,00 € und „Zeitung" 20,00 € monatlich in „Abonnements" mit 5,00 € und
  10,00 € Sparpotential, **dann** zeigt „Summe Abonnements" links neben 35,00 € das Sparschwein mit 15,00 €
  (SU-10, SU-11).
- **Gegeben** „Tanken" in Lebensunterhalt und „Ausflüge" in Freizeit & Spaß, beide in „Auto" mit je 10,00 €
  Sparpotential, **dann** zeigen beide Zeilen „Summe Auto" je 10,00 €, der Kasten neben der Bilanz 20,00 € (SU-2,
  SU-10).
- **Gegeben** die Gruppe „Miete" ohne Sparpotential und die Einnahmen-Gruppe „Gehalt", **dann** zeigt keine der beiden
  Summenzeilen ein Sparpotential, ebenso wenig „Summe Lebensunterhalt" und „Summe Ausgaben" (SU-12).
- **Gegeben** „Summe Abonnements" mit Sparpotential, **wenn** der Benutzer bei „Zeitung" das Sparpotential entfernt,
  **dann** zeigt die Zeile sofort 5,00 € (SU-14).
- **Gegeben** ein 390 px breiter Bildschirm, **dann** steht das Sparpotential von „Summe Abonnements" links neben dem
  Monatsbetrag, die Beträge stehen unter „Monat" und „Jahr", und die Seite ist nicht breiter als der Bildschirm
  (SU-13).

## Nicht im Umfang

- Sparpotential in den Summenzeilen von Kategorie, Einnahmen und Ausgaben, Jahreswert des Sparpotentials in den
  Summenzeilen
- Sparpotential je Kategorie der 50/30/20-Regel
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
- Summenzeilen (SU-10 bis SU-14): `OverviewItemGroup.monthlySavingPotential()` summiert die Sparpotentiale der Posten
  einer Gruppe; `ItemGroupResponse.monthlySavingPotential` liefert es („0.00" ohne Sparpotential), und
  `savingPotentialsBySubcategory()` baut darauf auf. `budget/ui/budget-entries.html` zeigt es als
  `.group-saving-potential` in den `.amounts` der Gruppen-Summenzeile vor dem Monatsbetrag, auf breiter Karte 96 px
  breit unter den Markierungen der Posten, auf schmaler Karte mit den Beträgen umbrechend (SU-11, SU-13);
  `BudgetEntries.hasSavingPotential` blendet es bei 0,00 € aus (SU-12)
- Tests: `HouseholdPlanOverviewSavingPotentialTest`, `BudgetItemMarksControllerTest`, `saving-potential-summary.spec.ts`,
  `overview-summary.spec.ts`, `budget-entries.spec.ts`
- E2E: `e2e/tests/sparpotential-unterkategorie.spec.ts` (SU-1 bis SU-4, SU-6 bis SU-14; SU-5 folgt aus PM-8 und ist dort
  abgedeckt). `openSavingPotential`, `setSavingPotential` und `savingPotentialOfSubcategory` stehen in `support.ts`;
  `balanceSheet` liest nur noch aus dem Kasten „Bilanz"

## Offene Fragen

Keine. Entschieden für SU-10 bis SU-14: Der Kasten neben der Bilanz bleibt (Benutzer). Zur Freigabe vorgeschlagen:
nur der Monatsbetrag in der Summenzeile, weil der Monat die maßgebliche Kenngröße ist und die Zeile sonst zu voll würde
(SU-10); Wert je Gruppe und damit je Kategorie, wie die Summe daneben (SU-10); nur Unterkategorie-Gruppen, nicht die
Summen darüber (SU-12).

Frühere Entscheidungen zu SU-1 bis SU-9: Entschieden bei der Freigabe: eigener Kasten links neben der Bilanz statt weiterer Zeilen in ihr (SU-6), auf
dem Handy darunter (SU-8), Unterkategorien über alle Kategorien zusammengefasst (SU-2), nach Betrag geordnet (SU-4),
nur Unterkategorien mit Sparpotential (SU-3), Monat und Jahr wie in der Bilanz, Monat als maßgebliche Kenngröße
links (SU-1, SU-6).
