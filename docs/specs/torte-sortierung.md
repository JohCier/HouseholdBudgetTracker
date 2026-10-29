# Sortierung der Torte

Status: geplant

## Zweck

Die Legende der Torte folgt heute fest der Reihenfolge der Karte „Einträge“ ([torte-ausschnitt.md](torte-ausschnitt.md),
TK-6). Wer in einer Kategorie mit vielen Unterkategorien oder in einer Unterkategorie mit vielen Posten wissen will, wo
das meiste Geld hingeht, muss die Beträge in der Legende selbst vergleichen.

Mit dieser Spec kann der Benutzer die Stücke eines Ausschnitts nach Betrag oder Name sortieren, ohne die Reihenfolge
der Einträge zu ändern. Die Spec ersetzt das Nicht-Ziel „Umsortieren in der Torte“ in
[torte-ausschnitt.md](torte-ausschnitt.md).

## Fachliche Regeln

- **TS-1**: In einem Ausschnitt einer Kategorie oder Unterkategorie (TK-1) steht neben der Auswahl „Ausschnitt“ die
  Auswahl „Sortierung“ mit drei Möglichkeiten:
  - „Einträge“: die Reihenfolge der Karte „Einträge“ wie bisher (TK-6, UK-14 bis UK-16),
  - „Betrag“: nach dem Betrag je Monat (MJ-7, UK-12),
  - „Name“: nach dem Namen der Unterkategorie bzw. der Beschreibung des Postens.

  Beim Öffnen der Übersicht ist „Einträge“ gewählt. Im Ausschnitt „50/30/20-Regel“ und in der Balkenansicht gibt es
  die Auswahl nicht; die drei Kategorien behalten ihre feste Reihenfolge (TD-2).
- **TS-2**: Bei „Betrag“ und „Name“ steht neben der Auswahl ein Knopf, der die Richtung umkehrt. Nach dem Wählen gilt
  bei „Betrag“ absteigend (größter Betrag oben) und bei „Name“ aufsteigend (A–Z). Der Knopf zeigt die Richtung als Pfeil
  und nennt sie für Screenreader („absteigend“, „aufsteigend“). Bei „Einträge“ gibt es den Knopf nicht.
- **TS-3**: Namen werden nach deutscher Schreibweise verglichen, ohne Rücksicht auf Groß- und Kleinschreibung
  („auto“ und „Auto“ gleich, „Ärztin“ bei A). Bei gleichem Betrag bzw. gleichem Namen bleibt die Reihenfolge der
  Einträge.
- **TS-4**: Torte und Legende haben immer dieselbe Reihenfolge: Das erste Stück der Legende beginnt in der Torte bei
  zwölf Uhr, die weiteren folgen im Uhrzeigersinn (TK-6). Die Abstufung der Farbe folgt der sortierten Reihenfolge,
  das erste Stück trägt den kräftigsten Ton (TK-10).
- **TS-5**: Das Stück „nicht verplant“ (TK-9) wird nicht mitsortiert und steht immer als letztes Stück. Stücke mit
  0,00 € stehen in der Legende an der Stelle, die die Sortierung ergibt (bei „Betrag“ absteigend also am Ende).
- **TS-6**: Die Sortierung gilt für alle Ausschnitte gleichermaßen und bleibt erhalten, wenn der Benutzer den
  Ausschnitt wechselt, zwischen Balken und Torte wechselt, Posten erfasst, ändert oder löscht und wenn er das Jahr
  wechselt (wie TK-5). Über das Neuladen der Seite hinaus wird nichts gespeichert.
- **TS-7**: Die Sortierung ändert nichts an der Reihenfolge der Karte „Einträge“ und nichts an der Reihenfolge der
  Unterkategorien in der Auswahl „Ausschnitt“ (TK-2).
- **TS-8**: Auf schmalen Bildschirmen (ab 360 px, MA-1) passen „Ausschnitt“, „Zurück“, „Sortierung“ und der Knopf für
  die Richtung ohne waagerechtes Scrollen auf den Bildschirm; sie dürfen dazu umbrechen.

## Akzeptanzkriterien

- **Gegeben** in Lebensunterhalt die Unterkategorien „Auto“ 250,00 €, „Miete“ 1.000,00 € und „Versicherungen“
  80,00 € je Monat, **wenn** der Benutzer den Ausschnitt „Lebensunterhalt“ wählt, **dann** ist „Einträge“ gewählt,
  und die Legende zeigt „Auto“, „Miete“, „Versicherungen“ (TS-1).
- **Gegeben** derselbe Plan im Ausschnitt „Lebensunterhalt“, **wenn** der Benutzer „Betrag“ wählt, **dann** zeigt die
  Legende „Miete 1.000,00 €“, „Auto 250,00 €“, „Versicherungen 80,00 €“, und das Stück „Miete“ beginnt bei zwölf Uhr
  im kräftigsten Ton (TS-1, TS-2, TS-4).
- **Gegeben** „Betrag“ ist gewählt, **wenn** der Benutzer den Knopf für die Richtung drückt, **dann** zeigt die
  Legende „Versicherungen“, „Auto“, „Miete“, und der Knopf nennt „aufsteigend“ (TS-2).
- **Gegeben** in Lebensunterhalt › Auto die Posten „Versicherung“, „sprit“ und „Kfz-Steuer“, **wenn** der Benutzer
  „Name“ wählt, **dann** zeigt die Legende „Kfz-Steuer“, „sprit“, „Versicherung“ (TS-1, TS-3).
- **Gegeben** Einnahmen 3.000,00 € und in Freizeit & Spaß „Urlaub“ 200,00 € und „Hobby“ 300,00 € je Monat, **wenn**
  der Benutzer „Freizeit & Spaß“ wählt und „Betrag“ aufsteigend sortiert, **dann** zeigt die Legende „Urlaub“,
  „Hobby“ und als Letztes „nicht verplant 2.500,00 €“ (TS-5).
- **Gegeben** „Betrag“ ist im Ausschnitt „Lebensunterhalt“ gewählt, **wenn** der Benutzer „Lebensunterhalt › Auto“
  wählt, auf „Balken“ und zurück auf „Torte“ schaltet oder das Jahr wechselt, **dann** ist weiterhin „Betrag“ gewählt
  (TS-6).
- **Gegeben** „Betrag“ ist gewählt, **dann** stehen die Unterkategorien in der Karte „Einträge“ und in der Auswahl
  „Ausschnitt“ weiterhin in ihrer bisherigen Reihenfolge (TS-7).
- **Gegeben** der Ausschnitt „50/30/20-Regel“, **dann** gibt es keine Auswahl „Sortierung“ (TS-1).
- **Gegeben** ein Bildschirm mit 360 px Breite, **wenn** der Benutzer „Betrag“ wählt, **dann** ist die Seite nicht
  breiter als der Bildschirm (TS-8, MA-1).

## Nicht im Umfang

- Sortieren der drei Kategorien im Ausschnitt „50/30/20-Regel“ (TS-1)
- Sortieren nach dem Anteil in Prozent; er ergibt dieselbe Reihenfolge wie der Betrag
- Sortieren der Karte „Einträge“ oder der Balken; die Reihenfolge der Einträge legt der Benutzer weiter per Ziehen
  fest (UK-14)
- Je Ausschnitt eine eigene Sortierung (TS-6)
- Speichern der Sortierung über das Neuladen der Seite hinaus
- Änderungen an Backend, API oder Auswertung; die Oberfläche ordnet nur die Stücke um, alle Beträge und Anteile kommen
  weiter aus der Übersicht (TK-8)

## Umsetzung

Noch nicht umgesetzt. Geplant: `PieSort` in `budget/budget.model.ts`; Auswahl „Sortierung“ (`mat-select`) und Knopf
für die Richtung in `budget/ui/category-pie.ts`, die Sortierung hält `budget/budget-dashboard.ts` wie `pieSelection`
(TS-6). Tests in `category-pie.spec.ts`, E2E in `e2e/tests/torte-sortierung.spec.ts`.

## Offene Fragen
