# Einträge: Unterkategorien farblich absetzen, Kategoriesummen hervorheben

Status: umgesetzt

## Zweck

In der Karte „Einträge" stehen Unterkategorie-Gruppen auf demselben Hintergrund wie ihre Kategorie. Wo eine Gruppe
anfängt und aufhört, erkennt man nur an der Einrückung und an dünnen Linien. Außerdem sieht „Summe ‹Kategorie›" fast
genauso aus wie „Summe ‹Unterkategorie›" darüber: Wer wissen will, was Lebensunterhalt insgesamt kostet, muss die
richtige Zeile erst suchen.

Diese Spec setzt die Gruppen farblich ab und hebt die Summen der Kategorien und der Einnahmen deutlich hervor. Sie
ändert nur die Darstellung nach EG-7 und UK-19, keine Werte und keine Anordnung.

## Fachliche Regeln

- **EF-1**: Jede Unterkategorie-Gruppe (auch „Sonstiges") steht auf einer leicht getönten Fläche mit abgerundeten
  Ecken. Die Tönung ist ein schwacher Anteil der Farbe ihres Bereichs bzw. ihrer Kategorie (UK-19): bei Einnahmen
  grünlich, bei Lebensunterhalt bräunlich, bei Freizeit & Spaß gelblich, bei Sparen & Investieren bläulich. Zwischen
  zwei Gruppen bleibt ein Spalt im Hintergrund der Karte, sodass jede Gruppe als eigener Block erkennbar ist.
- **EF-2**: Die Zusammenfassung einer Kategorie („Summe Lebensunterhalt" usw.) und der Einnahmen („Summe Einnahmen")
  hebt sich deutlich von den Summen der Unterkategorien ab: fette, größere Schrift, eine kräftigere Tönung in der
  Farbe der Kategorie und darüber eine Linie in dieser Farbe. Die Summe einer Unterkategorie bleibt zurückhaltend
  (normale Schrift, gedämpfte Farbe, dünne Linie), damit die Rangfolge Posten → Unterkategorie → Kategorie auf einen
  Blick zu lesen ist.
- **EF-3**: „Summe Ausgaben" bleibt ohne Tönung, steht aber wie die Kategoriesummen in fetter Schrift, sodass die
  Gesamtsumme nicht schwächer wirkt als die Summen der Kategorien.
- **EF-4**: Ist die Karte schmal (MA-2), halten die Beträge etwas Abstand zum rechten Rand der Tönung; die
  Spaltenköpfe „Monat" und „Jahr" und „Summe Ausgaben" rücken mit, sodass alle Beträge weiter rechtsbündig in ihren
  Spalten stehen (MA-3, MA-7). Auf breiten Karten stehen rechts davon ohnehin die Knöpfe.
- **EF-5**: Die Tönungen sind im hellen und im dunklen Farbschema gut zu unterscheiden; Texte und Beträge bleiben auf
  ihnen gut lesbar (Kontrast mindestens 4,5 : 1). Ein als wichtig markierter Posten (PM-9) behält seine eigene
  violette Tönung und hebt sich damit weiter von seiner Gruppe ab.
- **EF-6**: Beim Verschieben per Ziehen (UK-14) hat die gezogene Zeile einen undurchsichtigen Hintergrund, damit sie
  über anderen Zeilen lesbar bleibt.

## Akzeptanzkriterien

- **Gegeben** die Gruppen „Auto" und „Haus" in Lebensunterhalt, **dann** haben beide Gruppen einen Hintergrund, der
  sich vom Hintergrund der Karte unterscheidet und bräunlich getönt ist, und zwischen ihnen ist der Hintergrund der
  Karte zu sehen (EF-1).
- **Gegeben** dieselben Gruppen, **dann** ist „Summe Lebensunterhalt" fett und kräftiger getönt als die Gruppen, und
  „Summe Auto" ist nicht fett (EF-2).
- **Gegeben** die Gruppe „Arbeit" bei den Einnahmen, **dann** ist „Arbeit" grünlich getönt und „Summe Einnahmen" fett
  (EF-1, EF-2).
- **Gegeben** derselbe Plan im dunklen Farbschema, **dann** gelten dieselben Unterschiede (EF-5).
- **Gegeben** derselbe Plan auf einem 412 px breiten Bildschirm mit Schrift auf 120 %, **dann** ist die Seite nicht
  breiter als der Bildschirm, und die Beträge von Posten, „Summe Haus", „Summe Lebensunterhalt" und „Summe Ausgaben"
  stehen rechtsbündig unter „Monat" und „Jahr" (EF-4, MA-1, MA-7).

## Nicht im Umfang

- Andere Farben für die Kategorien selbst; sie bleiben nach UK-19
- Gruppen ein- und ausklappen
- Die Karte „50/30/20-Regel", die Bilanz und die Torte
- Ein vom Benutzer wählbares Farbschema oder abschaltbare Tönung

## Umsetzung

- `budget/ui/budget-entries.scss`: Jeder Bereich setzt `--group-background` und `--total-background` als `color-mix`
  aus `--section-color` und `--mat-sys-surface` (hell 8 % bzw. 20 %, dunkel 12 % bzw. 26 %). `.group` nutzt die
  schwache Tönung (EF-1), `.section > .summary` die kräftige mit Linie in `--section-color`, fett in `title-medium`;
  `.group-summary` steht in `on-surface-variant` mit Linie in `outline-variant` (EF-2). `.area-summary` ist fett
  (EF-3). Unter 600 px Kartenbreite bekommen Gruppen, Kategoriesummen, „Summe Ausgaben" und die Spaltenköpfe 8 px
  Abstand rechts (EF-4). Posten haben keinen eigenen Hintergrund mehr, `.cdk-drag-preview` bekommt
  `--mat-sys-surface` (EF-6)
- E2E: `e2e/tests/eintraege-farben.spec.ts` (EF-1 bis EF-5, hell, dunkel und 412 px mit Schrift auf 120 %); die
  übrigen Darstellungsregeln sichern weiter `mobile-ansicht.spec.ts` (MA-1 bis MA-7) und `unterkategorie.spec.ts`
  (UK-19). EF-6 ist reine Darstellung beim Ziehen und ohne eigenen Test

## Offene Fragen

Keine. Entschieden bei der Freigabe: Tönung in der Farbe der Kategorie, „Summe Ausgaben" nur fett ohne Tönung (EF-3),
Beträge der Kategoriesummen neutral wie bisher.
