# Ausschnitt der Torte: Kategorie und Unterkategorie

Status: umgesetzt

## Zweck

Die Torte zeigt heute nur die Aufteilung des Einkommens auf die drei Kategorien der 50/30/20-Regel
([tortendiagramm.md](tortendiagramm.md)). Wer wissen will, wofür innerhalb von Lebensunterhalt das Geld ausgegeben
wird oder woraus sich „das Auto" zusammensetzt, muss in der Karte „Einträge" die Summen der Gruppen vergleichen.

Mit dieser Spec kann der Benutzer in der Torte einen Ausschnitt wählen: die ganze 50/30/20-Regel wie bisher, eine
einzelne Kategorie aufgeteilt nach ihren Unterkategorien oder eine einzelne Unterkategorie aufgeteilt nach ihren Posten.

Diese Spec ersetzt das Nicht-Ziel „Aufteilung einer Kategorie nach einzelnen Posten" in [tortendiagramm.md](tortendiagramm.md)
und „Unterkategorien in der Torte" in [unterkategorie.md](unterkategorie.md), soweit es die Torte betrifft. Die Balken
bleiben unverändert.

## Fachliche Regeln

### Ausschnitt wählen

- **TK-1**: In der Tortenansicht (TD-1) steht über der Torte die Auswahl „Ausschnitt" mit drei Ebenen:
  - „50/30/20-Regel": die bisherige Torte (TD-2 bis TD-9),
  - eine Kategorie (Lebensunterhalt, Freizeit & Spaß, Sparen & Investieren): ein Stück je Unterkategorie-Gruppe dieser
    Kategorie,
  - eine Unterkategorie einer Kategorie, z. B. „Lebensunterhalt › Auto": ein Stück je Posten dieser Gruppe.

  Beim Öffnen der Übersicht ist „50/30/20-Regel" gewählt. In der Balkenansicht gibt es die Auswahl nicht.
- **TK-2**: Die Auswahl bietet die drei Kategorien immer an und darunter jeweils die Unterkategorie-Gruppen, die die
  Kategorie im angezeigten Plan hat, in der Reihenfolge der Karte „Einträge" (UK-14 bis UK-16).
- **TK-3**: Eine Unterkategorie gehört immer zu einer Kategorie: „Auto" unter Lebensunterhalt und „Auto" unter
  Freizeit & Spaß sind zwei verschiedene Ausschnitte (UK-3). Eine Summe über alle Kategorien gibt es nicht.
- **TK-4**: Ein Klick auf ein Stück (oder Enter auf dem fokussierten Stück) wählt den nächsttieferen Ausschnitt: in
  der 50/30/20-Regel die Kategorie, in einer Kategorie die Unterkategorie. Ein Stück eines Postens und das Stück
  „nicht verplant" (TK-9) wählen nichts aus. Ein Knopf „Zurück" neben der Auswahl führt eine Ebene nach oben.
- **TK-5**: Der gewählte Ausschnitt bleibt erhalten, wenn der Benutzer zwischen Balken und Torte wechselt, Posten
  erfasst, ändert oder löscht und wenn er das Jahr wechselt (wie TD-1). Gibt es den gewählten Ausschnitt nicht mehr
  (die Gruppe hat keine Posten mehr, UK-18, oder der andere Plan hat sie nicht), springt die Auswahl auf die
  Kategorie. Über das Neuladen der Seite hinaus wird nichts gespeichert.

### Torte und Legende eines Ausschnitts

- **TK-6**: Maßgeblich sind in allen Ausschnitten die Monatsbeträge: In einer Kategorie entspricht die Größe eines
  Stücks der Summe der Gruppe je Monat (UK-12), in einer Unterkategorie dem Betrag des Postens je Monat (MJ-7), auch
  wenn er jährlich erfasst wurde. Die Stücke stehen in der Reihenfolge der Karte „Einträge". Stücke mit 0,00 €
  entfallen, stehen aber in der Legende (wie TD-6).
- **TK-7**: Über der Legende steht der Ausschnitt mit seiner Summe je Monat, bei einer Kategorie zusätzlich ihr
  Anteil am Einkommen mit Zielanteil und gegebenenfalls Zielhinweis, mit denselben Texten wie in der 50/30/20-Regel
  (TD-5).
- **TK-8**: Die Legende nennt je Stück Name (Unterkategorie bzw. Beschreibung des Postens), Betrag je Monat (MJ-14)
  und seinen Anteil an der Summe des Ausschnitts je Monat in Prozent, z. B. „Auto 145,83 € · 13 %". Die Anteile rechnet das Backend (wie AW-9,
  UK-13); die Oberfläche teilt den Kreis nur im Verhältnis der Beträge auf (wie TD-8).
- **TK-9**: Bei Freizeit & Spaß bekommt ein Überschuss (AW-10) ein eigenes Stück „nicht verplant", damit die
  Summe des Ausschnitts dieselbe ist wie in der 50/30/20-Regel. Es steht als letztes Stück in der Legende.
- **TK-10**: Die Stücke eines Ausschnitts tragen Abstufungen der Farbe ihrer Kategorie (UK-19), vom kräftigsten
  Ton für das erste Stück bis zum hellsten; benachbarte Stücke sind durch eine helle Trennlinie unterschieden. Das
  Stück „nicht verplant" ist grau. In hellem und dunklem Farbschema bleibt jedes Stück vom Hintergrund
  unterscheidbar.
- **TK-11**: Hat der gewählte Ausschnitt keine Beträge über 0,00 € (eine Kategorie ohne Posten), zeigt die Torte einen
  leeren grauen Kreis mit dem Hinweis „Keine Ausgaben in ‹Kategorie›".
- **TK-12**: Die Stücke sind für Screenreader beschriftet: Name, Betrag je Monat und Anteil am Ausschnitt; Stücke, die
  einen Ausschnitt wählen (TK-4), sind als Knopf erreichbar und nennen das Ziel („Lebensunterhalt anzeigen").
- **TK-13**: Auf schmalen Bildschirmen (ab 360 px, MA-1) passen Auswahl, „Zurück", Torte und Legende untereinander auf
  den Bildschirm; lange Namen in der Legende brechen zwischen Wörtern um (MA-4).

- **TK-14**: Fährt der Benutzer mit der Maus über ein Stück, leuchtet seine Zeile in der Legende auf, damit er sieht,
  um welche Kategorie, Unterkategorie oder welchen Posten es sich handelt; das Stück selbst wird etwas heller. Das gilt in
  allen Ausschnitten, für Stücke, die einen Ausschnitt wählen (TK-4), wie für Posten und „nicht verplant", und ebenso
  für das per Tastatur fokussierte Stück. Verlässt die Maus das Stück, erlischt die Hervorhebung. Auf Touch-Geräten
  bleibt es bei TK-4: Ein Tippen auf ein Stück, das einen Ausschnitt wählt, springt hinein, ohne dass im neuen
  Ausschnitt etwas hervorgehoben ist; ein Tippen auf ein Stück ohne Ziel hebt seine Zeile hervor, bis der Benutzer
  woanders hin tippt. Die Hervorhebung nutzt die Farben des Farbschemas und ist hell wie dunkel gut lesbar.

## Akzeptanzkriterien

- **Gegeben** ein Plan mit Ausgaben, **wenn** der Benutzer auf „Torte" umschaltet, **dann** ist im Ausschnitt
  „50/30/20-Regel" gewählt, und die Torte sieht aus wie bisher (TK-1).
- **Gegeben** Ausgaben in Lebensunterhalt „Miete" 1.000,00 € monatlich (Unterkategorie „Miete") und „Sprit" 200,00 €
  sowie „Versicherung" 50,00 € monatlich (Unterkategorie „Auto"), **wenn** der Benutzer den Ausschnitt
  „Lebensunterhalt" wählt, **dann** zeigt die Torte zwei Stücke und die Legende „Auto 250,00 € · 20 %" und
  „Miete 1.000,00 € · 80 %" in der Reihenfolge der Einträge, darüber „Lebensunterhalt 1.250,00 €" mit dem Anteil am
  Einkommen (TK-1, TK-2, TK-6 bis TK-8).
- **Gegeben** derselbe Plan, **wenn** der Benutzer „Lebensunterhalt › Auto" wählt, **dann** zeigt die Torte zwei
  Stücke, und die Legende zeigt „Sprit 200,00 € · 80 %" und „Versicherung 50,00 € · 20 %" (TK-1, TK-6, TK-8).
- **Gegeben** in Lebensunterhalt „Auto" nur „Kfz-Steuer" 120,00 € jährlich und „Sprit" 30,00 € monatlich, **wenn**
  der Benutzer „Lebensunterhalt › Auto" wählt, **dann** zeigt die Legende „Kfz-Steuer 10,00 € · 25 %" und
  „Sprit 30,00 € · 75 %", und die Stücke stehen im Verhältnis 1 : 3 (TK-6, TK-8).
- **Gegeben** die Torte im Ausschnitt „50/30/20-Regel", **wenn** der Benutzer auf das Stück Lebensunterhalt klickt
  und dann auf das Stück „Auto", **dann** ist „Lebensunterhalt › Auto" gewählt; **wenn** er dann „Zurück" wählt,
  **dann** ist „Lebensunterhalt" gewählt (TK-4).
- **Gegeben** „Auto" in Lebensunterhalt und in Freizeit & Spaß, **dann** bietet die Auswahl „Auto" unter beiden
  Kategorien getrennt an, und jeder Ausschnitt zeigt nur die Posten seiner Kategorie (TK-2, TK-3).
- **Gegeben** Einnahmen 3.000,00 € und Ausgaben Freizeit & Spaß 500,00 € monatlich, sonst keine, **wenn** der
  Benutzer „Freizeit & Spaß" wählt, **dann** zeigt die Torte ein Stück für die Gruppe und ein graues Stück
  „nicht verplant 2.500,00 €" (TK-9, TK-10).
- **Gegeben** „Lebensunterhalt › Auto" ist gewählt, **wenn** der Benutzer einen weiteren Posten in „Auto" erfasst,
  **dann** bleibt der Ausschnitt gewählt und zeigt ein Stück mehr (TK-5).
- **Gegeben** „Lebensunterhalt › Auto" ist gewählt, **wenn** der Benutzer den letzten Posten in „Auto" löscht,
  **dann** springt der Ausschnitt auf „Lebensunterhalt" (TK-5).
- **Gegeben** „Lebensunterhalt" ist gewählt, **wenn** der Benutzer auf „Balken" und wieder auf „Torte" umschaltet
  oder das Jahr wechselt, **dann** ist weiterhin „Lebensunterhalt" gewählt (TK-5).
- **Gegeben** ein Plan ohne Ausgaben in Freizeit & Spaß, **wenn** der Benutzer „Freizeit & Spaß" wählt, **dann**
  zeigt die Torte einen leeren Kreis mit „Keine Ausgaben in Freizeit & Spaß" (TK-11).
- **Gegeben** die Torte im Ausschnitt „50/30/20-Regel", **wenn** der Benutzer mit der Maus über das Stück
  Lebensunterhalt fährt, **dann** ist in der Legende nur die Zeile „Lebensunterhalt" hervorgehoben; **wenn** er die
  Maus wegbewegt, **dann** ist keine Zeile mehr hervorgehoben (TK-14).
- **Gegeben** „Lebensunterhalt › Auto" ist gewählt, **wenn** der Benutzer mit der Maus über das Stück „Versicherung" fährt,
  **dann** ist in der Legende die Zeile „Versicherung" hervorgehoben (TK-14).
- **Gegeben** ein Bildschirm mit 360 px Breite, **wenn** der Benutzer einen Ausschnitt wählt, **dann** ist die Seite
  nicht breiter als der Bildschirm (TK-13, MA-1).

## Nicht im Umfang

- Ein Ausschnitt für die Einnahmen (Aufteilung nach Gehalt, Kindergeld …)
- Eine Unterkategorie über alle Kategorien hinweg (TK-3)
- Ausschnitte in der Balkenansicht
- Speichern des Ausschnitts über das Neuladen der Seite hinaus
- Vergleich zweier Ausschnitte oder zweier Jahre nebeneinander
- Umsortieren in der Torte; die Reihenfolge kommt aus der Karte „Einträge"

## Umsetzung

- Backend: Das Read Model `HouseholdPlanOverview` gibt jeder Gruppe `percentageOfSection` mit, den Anteil ihrer Summe
  je Monat an der Summe der Kategorie je Monat aus der Auswertung (bei Freizeit & Spaß mit Überschuss, TK-9), bei
  den Einnahmen am Einkommen. `OverviewItemGroup.percentageOf` rechnet den Anteil eines Postens an seiner Gruppe aus
  den Monatsbeträgen, `CategoryBreakdown.monthlyUnplannedShare` den Anteil von „nicht verplant" (TK-6, TK-8)
- API: `ItemGroupResponse.percentageOfSection`, `BudgetItemResponse.percentageOfGroup` (nur bei Posten einer Gruppe
  gesetzt), `CategoryBreakdownResponse.unplannedPercentageOfTotal`
- Frontend: `PieSelection` und `isSameSubcategory` in `budget/budget.model.ts`; `budget/ui/category-pie.ts` mit der
  Auswahl „Ausschnitt" (`mat-select`, Gruppen eingerückt unter ihrer Kategorie), „Zurück", anklickbaren Stücken
  (Rolle `button`) und Abstufungen der Kategoriefarbe per `color-mix` (TK-10). Das Stück unter der Maus bzw. mit dem
  Fokus hält `highlighted` (ein `linkedSignal`, das beim Wechsel des Ausschnitts zurückgesetzt wird); seine
  Legendenzeile bekommt die Klasse `highlighted` mit `--mat-sys-secondary-container` (TK-14). Den Ausschnitt hält
  `budget/budget-dashboard.ts` (`pieSelection`) wie die Darstellung; eine Gruppe ohne Posten zeigt die Torte als ihre
  Kategorie (TK-5)
- Tests: `HouseholdPlanOverviewSharesTest`, `HouseholdPlanOverviewControllerTest`, `category-pie.spec.ts`
- E2E: `e2e/tests/torte-ausschnitt.spec.ts`

## Offene Fragen
