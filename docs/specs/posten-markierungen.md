# Posten markieren: wichtig und Sparpotential

Status: geplant

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
- **PM-2**: Jede Ausgabe kann als „Sparpotential" markiert sein. Einnahmen haben kein Sparpotential. Eine neue Ausgabe
  hat kein Sparpotential.
- **PM-3**: Beide Markierungen sind voneinander unabhängig; ein Posten kann wichtig sein, Sparpotential haben, beides
  oder keins.
- **PM-4**: Markiert und entmarkiert wird nur in der Karte „Einträge", direkt am Posten, mit je einem Umschalter
  „Wichtig" und „Sparpotential" (letzterer nur bei Ausgaben). Das Formular „Neuer Eintrag", der Dialog „Neuer Eintrag
  in ‹Gruppe›" (EG-10) und der Dialog „Eintrag bearbeiten" (PB-1) haben keine Felder dafür. Ein Klick genügt, ohne
  Rückfrage; die Markierung wird sofort gespeichert. Wie alle Werte eines Postens ändert sie nur der Besitzer (PO-6).
- **PM-5**: Bearbeiten (PB-1) lässt beide Markierungen unverändert. Wird eine Ausgabe mit Sparpotential beim Bearbeiten
  zur Einnahme, verliert sie das Sparpotential (PM-2).
- **PM-6**: Wird das Löschen eines Postens rückgängig gemacht (PO-9), hat er wieder seine Markierungen. Beim Übernehmen
  als Vorlage (PU-3) werden die Markierungen mit übernommen.
- **PM-7**: Das Setzen oder Entfernen einer Markierung wird protokolliert (`BudgetItemMarked`), mit Posten und
  Markierung. Die Reihenfolge der Posten ändert sich dadurch nicht.
- **PM-8**: Die Markierungen haben keinen Einfluss auf die Auswertung nach der 50/30/20-Regel.

### Darstellung

- **PM-9**: Ein wichtiger Posten ist in der Karte „Einträge" farblich hervorgehoben: Seine Zeile hat einen getönten
  Hintergrund und einen farbigen Streifen am linken Rand, in hellem und dunklem Farbschema gut lesbar. Der Umschalter
  „Wichtig" zeigt dann einen gefüllten Stern, sonst einen leeren.
- **PM-10**: Der Umschalter „Sparpotential" zeigt bei markierten Ausgaben ein gefülltes Symbol (Sparschwein), sonst ein
  leeres. Die Zeile selbst wird dafür nicht eingefärbt.
- **PM-11**: Beide Umschalter haben einen Tooltip und für Screenreader die Bezeichnung „‹Beschreibung› als wichtig
  markieren" bzw. „Markierung wichtig bei ‹Beschreibung› entfernen", entsprechend für Sparpotential; ihr Zustand ist
  per `aria-pressed` erkennbar.
- **PM-12**: Auf schmaler Karte (MA-2) stehen die Umschalter in der oberen Zeile des Postens neben „Bearbeiten" und
  „Löschen". Die Beschreibung bricht weiterhin nur zwischen Wörtern um (MA-4), und die Seite ist ab 360 px nicht
  breiter als der Bildschirm (MA-1).

### Sparpotential gesamt

- **PM-13**: Das Sparpotential eines Plans ist die Summe der Jahresbeträge aller Ausgaben mit Sparpotential; das
  monatliche Sparpotential ist diese Summe durch 12, auf den Cent gerundet (wie MJ-9). Das Backend rechnet beide.
- **PM-14**: Der Kasten „Bilanz" (BI-1) hat unter dem Saldo eine Zeile „Sparpotential" mit Monats- und Jahreswert,
  von den Zeilen darüber abgesetzt und nicht in die Saldo-Rechnung einbezogen. Ohne markierte Ausgaben zeigt sie
  0,00 €.

## Akzeptanzkriterien

- **Gegeben** ein Plan mit der Ausgabe „Miete", **wenn** der Benutzer in der Karte „Einträge" bei „Miete" den Stern
  wählt, **dann** ist die Zeile „Miete" hervorgehoben, und nach dem Neuladen der Seite ist sie es immer noch; ein
  weiterer Klick hebt die Hervorhebung auf (PM-1, PM-4, PM-9).
- **Gegeben** das Formular „Neuer Eintrag", der Dialog „Neuer Eintrag in ‹Gruppe›" und der Dialog „Eintrag
  bearbeiten", **dann** hat keiner ein Feld „Wichtig" oder „Sparpotential" (PM-4).
- **Gegeben** die Ausgaben „Streaming" 15,00 € monatlich und „Fitnessstudio" 360,00 € jährlich, **wenn** der Benutzer
  bei beiden „Sparpotential" wählt, **dann** zeigt die Bilanz in der Zeile „Sparpotential" 45,00 € im Monat und
  540,00 € im Jahr; Saldo und 50/30/20-Auswertung bleiben unverändert (PM-2, PM-8, PM-13, PM-14).
- **Gegeben** derselbe Plan, **wenn** der Benutzer bei „Streaming" das Sparpotential wieder entfernt, **dann** zeigt
  die Bilanz 30,00 € und 360,00 € (PM-13).
- **Gegeben** die Einnahme „Gehalt", **dann** hat sie einen Umschalter „Wichtig", aber keinen für Sparpotential
  (PM-2, PM-4).
- **Gegeben** die Ausgabe „Streaming" mit Sparpotential, **wenn** der Benutzer sie bearbeitet und den Betrag auf
  20,00 € ändert, **dann** hat sie weiterhin Sparpotential, und die Bilanz zeigt 20,00 € im Monat (PM-5).
- **Gegeben** eine wichtige Ausgabe mit Sparpotential, **wenn** der Benutzer sie löscht und „Rückgängig" wählt,
  **dann** erscheint sie wieder hervorgehoben und mit Sparpotential (PM-6).
- **Gegeben** ein Plan mit wichtigen Posten und Ausgaben mit Sparpotential, **wenn** er als Vorlage übernommen wird,
  **dann** hat der neue Plan dieselben Markierungen (PM-6).
- **Gegeben** eine Einnahme, **wenn** per API Sparpotential gesetzt werden soll, **dann** antwortet die API mit 400
  (PM-2).
- **Gegeben** ein Plan auf einem 390 px breiten Bildschirm, **dann** sind bei jedem Posten beide Umschalter sichtbar,
  und die Seite ist nicht breiter als der Bildschirm (PM-12).

## Nicht im Umfang

- Ein Teilbetrag als Sparpotential (z. B. „davon 5,00 € einsparbar"); es zählt immer der ganze Betrag des Postens
- Sparpotential je Gruppe oder Kategorie in den Summenzeilen der Karte „Einträge"
- Filtern oder Sortieren nach Markierungen, alle Posten auf einmal markieren
- Sparpotential in der Jahresübersicht
- Weitere oder selbst definierte Markierungen und Farben

## Umsetzung

Wird nach der Umsetzung ergänzt.

## Offene Fragen

1. **Bedienung in der Zeile**: Zwei sichtbare Umschalter (Stern, Sparschwein) neben „Bearbeiten" und „Löschen", wie in
   PM-4 beschrieben? Oder ein Menü „⋮" je Posten mit „Als wichtig markieren" und „Sparpotential markieren", das auf dem
   Handy weniger Platz braucht, aber einen Klick mehr kostet und den Zustand nicht direkt zeigt?
2. **Wo das Sparpotential steht**: Nur als Zeile in der Bilanz (PM-14)? Oder zusätzlich je Gruppe und Kategorie in den
   Summenzeilen der Karte „Einträge" (heute „Nicht im Umfang")?
3. **Nur Ausgaben**: Sparpotential nur für Ausgaben (PM-2), oder auch für Einnahmen?
4. **Ganzer Betrag**: Zählt immer der ganze Betrag des Postens als Sparpotential (PM-13), oder soll man einen
   Teilbetrag angeben können?
5. **Übernehmen als Vorlage**: Sollen die Markierungen ins neue Jahr mitgehen (PM-6), oder startet das neue Jahr ohne
   Markierungen?
