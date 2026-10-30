# Bilanz in der Übersicht

Status: umgesetzt

## Zweck

Oben in der Planansicht stehen heute drei Kacheln nebeneinander: Einnahmen, Ausgaben und Saldo, jede mit ihrem
Monats- und ihrem Jahreswert untereinander (MJ-13). Wer Monat mit Monat oder Jahr mit Jahr vergleichen will, muss
zwischen den Kacheln hin und her springen, und auf schmalen Bildschirmen rutschen die Kacheln untereinander.

Diese Spec fasst die drei Summen in einem gemeinsamen Kasten zusammen, wie eine kleine Bilanz: Gesamteinnahmen,
darunter Gesamtausgaben und darunter der Saldo, links für den Monat und rechts daneben für das Jahr. Sie ersetzt MJ-13
in [monats-jahresbetrag.md](monats-jahresbetrag.md). Welche Werte gezeigt werden und wie sie gerechnet sind (AW-1,
AW-2, MJ-8, MJ-9), ändert sich nicht.

## Fachliche Regeln

- **BI-1**: Oben in der Planansicht steht ein Kasten „Bilanz" mit zwei Spalten: „Monat" links und „Jahr" rechts. Jede
  Spalte zeigt untereinander „Gesamteinnahmen", „Gesamtausgaben" und „Saldo".
- **BI-2**: Die Bezeichnungen stehen einmal am linken Rand des Kastens, die Beträge rechtsbündig in den Spalten, sodass
  sich Monats- und Jahreswert einer Zeile nebeneinander ablesen lassen.
- **BI-3**: Der Saldo ist durch eine Linie von den beiden Zeilen darüber getrennt und hervorgehoben. Die
  Gesamteinnahmen sind wie bisher in der Farbe der Einnahmen, die Gesamtausgaben und ein negativer Saldo in der
  Fehlerfarbe dargestellt.
- **BI-4**: Die Beträge sind die bisherigen: Monatssummen nach MJ-9, Jahressummen nach AW-1 und AW-2.
- **BI-5**: Ist der Kasten zu schmal für Bezeichnung, Monats- und Jahreswert nebeneinander (auf dem Handy, auch mit
  vergrößerter Schrift), stehen die Spalten untereinander: oben „Monat" mit allen Zeilen, darunter „Jahr" mit
  denselben Zeilen. Linien, Hervorhebung und Farben (BI-3) gelten in beiden Teilen. Der Kasten ist nie breiter als der
  Bildschirm (ab 360 px Breite, MA-1 in [mobile-ansicht.md](mobile-ansicht.md)).
- **BI-6**: Die Warnung „Deine Ausgaben übersteigen deine Einnahmen." (AW-6) steht unverändert unter dem Kasten; ohne
  Defizit steht dort die Meldung mit Haken aus AP-17.

## Akzeptanzkriterien

- **Gegeben** Einnahmen „Gehalt" 3.000,00 € monatlich und Ausgaben „Miete" 850,00 € monatlich, **dann** zeigt der
  Kasten „Bilanz" in der Spalte „Monat" Gesamteinnahmen 3.000,00 €, Gesamtausgaben 850,00 €, Saldo 2.150,00 € und in
  der Spalte „Jahr" 36.000,00 €, 10.200,00 € und 25.800,00 € (BI-1, BI-4).
- **Gegeben** derselbe Plan, **dann** steht jeder Jahreswert auf derselben Höhe rechts neben seinem Monatswert, und
  Gesamtausgaben stehen unter Gesamteinnahmen, Saldo unter Gesamtausgaben (BI-1, BI-2).
- **Gegeben** Ausgaben, die die Einnahmen übersteigen, **dann** ist der Saldo in beiden Spalten negativ und in der
  Fehlerfarbe, und unter dem Kasten steht die Warnung (BI-3, BI-6).
- **Gegeben** Einnahmen von 123.456,78 € monatlich auf einem 390 px breiten Bildschirm, **dann** steht der Teil „Jahr"
  unter dem Teil „Monat", die Beträge beider Teile rechtsbündig untereinander, und die Seite ist nicht breiter als der
  Bildschirm (BI-5).

## Nicht im Umfang

- Weitere Zeilen im Kasten, etwa die Summen der 50/30/20-Kategorien; sie stehen in der Karte „50/30/20-Regel".
  Ausnahme: die Zeile „Sparpotential" unter dem Saldo, siehe PM-14 in [posten-markierungen.md](posten-markierungen.md)
- Ein Umschalter, der nur Monat oder nur Jahr zeigt
- Änderungen an den Summen selbst oder an der API

## Umsetzung

- Frontend: `budget/ui/overview-summary.ts` zeigt statt der drei Kacheln eine Karte „Bilanz" mit einer Tabelle:
  Zeilenköpfe „Gesamteinnahmen", „Gesamtausgaben", „Saldo" links, Spalten „Monat" und „Jahr" rechtsbündig (BI-1,
  BI-2); die Zeile `balance` hat eine Linie darüber und größere Beträge, die Farben bleiben (BI-3). Die Werte kommen
  unverändert aus `HouseholdPlanOverview` (BI-4). Die Tabelle entsteht aus einer Vorlage je Spaltenauswahl: einmal
  mit beiden Spalten, einmal je eine Tabelle für Monat und Jahr. Eine Container-Abfrage auf den Karteninhalt
  (`max-width: 28em`, wächst mit der Schrift) zeigt unter dieser Breite die beiden einzelnen Tabellen untereinander
  statt der gemeinsamen (BI-5); die Warnung steht weiter unter der Karte (BI-6)
- Tests: `overview-summary.spec.ts` (auch die Tabellen untereinander, BI-5)
- E2E: `e2e/tests/bilanz.spec.ts` (BI-1 bis BI-6). Der Helfer `balanceSheet` (vorher `tile`) liest einen Wert über den
  Zeilenkopf; die übrigen E2E-Tests nutzen ihn mit den neuen Bezeichnungen

## Offene Fragen
