# Monats- und Jahresbetrag

Status: geplant

## Zweck

Heute ist jeder Posten ein Jahresbetrag (PO-2). Viele Einnahmen und Ausgaben kennt man aber als Monatsbetrag: Gehalt,
Miete, Abonnements, Sparrate. Wer sie erfasst, muss selbst mal 12 rechnen, und in der Übersicht fehlt der Blick auf
den Monat, also darauf, was jeden Monat ein- und ausgeht.

Mit dieser Spec gibt der Benutzer beim Erfassen und Bearbeiten an, ob ein Betrag monatlich oder jährlich gilt. Jeder
Posten hat dann einen Monats- und einen Jahresbetrag, und die Übersicht zeigt beide, für die einzelnen Posten wie für
die Summen.

Diese Spec ändert PO-2 in [posten.md](posten.md) und das Nicht-Ziel „Monatsbeträge" dort sowie das Nicht-Ziel „keine
Monatsansicht" in [produkt.md](../produkt.md): Ein Posten hat weiterhin kein Datum und gilt für das ganze Jahr seines
Plans, lässt sich aber als Monatsbetrag angeben und so anzeigen.

## Fachliche Regeln

### Angabe am Posten

- **MJ-1**: Jeder Posten, Einnahme wie Ausgabe, hat eine Angabe: monatlich oder jährlich. Sie sagt, ob der erfasste
  Betrag ein Monats- oder ein Jahresbetrag ist.
- **MJ-2**: Gespeichert werden der Betrag so, wie der Benutzer ihn eingegeben hat, und seine Angabe. Für den
  eingegebenen Betrag gilt PO-4 unverändert (positiv, höchstens zwei Nachkommastellen).
- **MJ-3**: Alle bestehenden Posten haben die Angabe „jährlich"; ihr Betrag bleibt unverändert.
- **MJ-4**: Die Angabe wird beim Erfassen gewählt und beim Bearbeiten geändert (PB-1). Ändert sich die Angabe oder der
  Betrag, ist das eine Änderung des Postens im Sinne von PB-5 und wird als `BudgetItemChanged` protokolliert. Wird das
  Löschen rückgängig gemacht (PO-9), wird auch die Angabe wiederhergestellt.
- **MJ-5**: Ein Wechsel der Angabe beim Bearbeiten rechnet den Betrag nicht um: Wer „850" von monatlich auf jährlich
  stellt, hat danach einen Jahresbetrag von 850,00 €.

### Monats- und Jahresbetrag

- **MJ-6**: Bei der Angabe „monatlich" ist der Jahresbetrag der Monatsbetrag × 12.
- **MJ-7**: Bei der Angabe „jährlich" ist der Monatsbetrag der Jahresbetrag ÷ 12, kaufmännisch auf ganze Cent gerundet
  (1.000,00 € jährlich ergibt 83,33 € monatlich).
- **MJ-8**: Gerechnet wird immer mit dem Jahresbetrag: Summen, Saldo und die Auswertung nach der 50/30/20-Regel
  (AW-1 bis AW-10) gehen vom Jahresbetrag aus. Der Monatsbetrag ist eine Anzeige und fließt in keine Rechnung ein.
- **MJ-9**: Jede Summe, die die Übersicht zeigt (Einnahmen, Ausgaben, Saldo, Bereiche, Kategorien, Unterkategorie-
  Gruppen nach UK-12), gibt es auch je Monat: die Jahressumme ÷ 12, kaufmännisch auf ganze Cent gerundet. Dadurch
  kann eine Monatssumme um wenige Cent von der Summe der angezeigten Monatsbeträge abweichen; das ist so gewollt.
- **MJ-10**: Monatsbeträge und Monatssummen rechnet das Backend (wie AW-9, UK-13); die Oberfläche rechnet nichts selbst.

### Anzeige

- **MJ-11**: Im Formular „Neuer Eintrag" und im Dialog „Bearbeiten" wählt der Benutzer die Angabe neben dem Betrag mit
  einem Umschalter „Monatlich | Jährlich". Beim Erfassen ist „Monatlich" vorausgewählt; nach dem Erfassen bleibt die
  gewählte Angabe wie die Art (PO-1). Der Dialog „Bearbeiten" zeigt den eingegebenen Betrag mit seiner Angabe.
- **MJ-12**: Die Karte „Einträge" zeigt bei jedem Posten und bei jeder Summe (Bereich, Kategorie, Gruppe) zwei
  Spalten: „Monat" und „Jahr". Der vom Benutzer eingegebene Betrag ist hervorgehoben, der abgeleitete schwächer
  dargestellt.
- **MJ-13**: Die Kacheln oben in der Übersicht zeigen Einnahmen, Ausgaben und Saldo jeweils je Monat und je Jahr.
- **MJ-14**: Die Karte „50/30/20-Regel" (Balken und Torte) zeigt weiterhin nur Jahresbeträge; die Anteile sind für
  Monat und Jahr ohnehin gleich.

## Akzeptanzkriterien

- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Miete", 850,00 €, „Monatlich", Lebensunterhalt erfasst,
  **dann** zeigt die Karte „Einträge" bei „Miete" 850,00 € je Monat und 10.200,00 € je Jahr, und Lebensunterhalt in
  der Auswertung steigt um 10.200,00 € (MJ-6, MJ-8, MJ-12).
- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Versicherung", 1.000,00 €, „Jährlich" erfasst, **dann**
  zeigt die Karte „Einträge" 83,33 € je Monat und 1.000,00 € je Jahr (MJ-7).
- **Gegeben** Einnahmen „Gehalt" 3.000,00 € monatlich und Ausgaben „Miete" 850,00 € monatlich, **dann** zeigen die
  Kacheln Einnahmen 3.000,00 € / 36.000,00 €, Ausgaben 850,00 € / 10.200,00 € und Saldo 2.150,00 € / 25.800,00 €
  (je Monat / je Jahr, MJ-9, MJ-13).
- **Gegeben** drei Ausgaben von je 100,00 € jährlich in derselben Gruppe, **dann** zeigt jeder Posten 8,33 € je Monat,
  die Gruppe aber 25,00 € je Monat und 300,00 € je Jahr (MJ-9).
- **Gegeben** ein Posten, der vor dieser Änderung erfasst wurde, **dann** hat er die Angabe „jährlich", und sein
  Jahresbetrag ist der bisherige Betrag (MJ-3).
- **Gegeben** eine Ausgabe 850,00 € monatlich, **wenn** der Benutzer „Bearbeiten" wählt, **dann** zeigt der Dialog
  850 und „Monatlich"; nach dem Umstellen auf „Jährlich" und „Speichern" ist der Jahresbetrag 850,00 € und ein Event
  `BudgetItemChanged` wird protokolliert (MJ-4, MJ-5, MJ-11).
- **Gegeben** ein gelöschter Posten mit Angabe „monatlich", **wenn** der Benutzer „Rückgängig" wählt, **dann** hat der
  Posten wieder dieselbe Angabe und denselben Betrag (MJ-4).
- **Gegeben** das Formular „Neuer Eintrag", **wenn** es geöffnet wird, **dann** ist „Monatlich" gewählt; nach dem
  Erfassen eines Postens mit „Jährlich" bleibt „Jährlich" gewählt (MJ-11).
- **Gegeben** ein Plan, **wenn** ein Posten mit unbekannter Angabe erfasst oder geändert werden soll, **dann**
  antwortet die API mit 400 (MJ-1).

## Nicht im Umfang

- Weitere Angaben wie wöchentlich, vierteljährlich oder halbjährlich
- Beträge für einzelne Monate, Posten, die nur in bestimmten Monaten anfallen, eine Ansicht je Kalendermonat
- Monatsbeträge in der Karte „50/30/20-Regel" (MJ-14)
- Ein Schalter, der die ganze Übersicht nur monatlich oder nur jährlich zeigt

## Umsetzung

Wird nach der Umsetzung ergänzt.

## Offene Fragen

1. Kacheln oben (MJ-13): Beide Werte gleichzeitig in jeder Kachel (Vorschlag), oder ein Umschalter „Monat | Jahr",
   der jeweils nur einen Wert zeigt?
2. Vorauswahl im Formular (MJ-11): „Monatlich" (Vorschlag, weil die meisten Posten Monatsbeträge sind) oder
   „Jährlich" wie bisher?
3. Umstellen der Angabe beim Bearbeiten (MJ-5): Betrag bleibt stehen (Vorschlag), oder soll er umgerechnet werden
   (850 monatlich wird zu 10.200 jährlich)?
4. Gruppen- und Kategoriesummen (MJ-12): auch dort beide Spalten (Vorschlag), oder nur bei den einzelnen Posten?
