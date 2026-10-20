# Monatsbeträge in der Jahresübersicht

Status: umgesetzt

## Zweck

Die Jahresübersicht zeigt Einnahmen, Ausgaben und Überschuss nur als Jahresbeträge (JU-2). Viele Benutzer denken aber
in Monaten: Was geht jeden Monat ein, was bleibt jeden Monat übrig? In der Planansicht sehen sie das schon
([MJ-13](monats-jahresbetrag.md)), in der Übersicht aller Pläne nicht.

Mit dieser Spec schaltet der Benutzer die Jahresübersicht zwischen Jahres- und Monatsbeträgen um.

Diese Spec ändert JU-2 und das Nicht-Ziel „Monatsbeträge in der Jahresübersicht" in
[jahresuebersicht.md](jahresuebersicht.md).

## Fachliche Regeln

- **JM-1**: Über der Tabelle der Jahresübersicht steht ein Umschalter „Monatlich | Jährlich" im selben Aussehen wie
  der Umschalter beim Erfassen eines Postens ([MJ-11](monats-jahresbetrag.md)). Er wird nur gezeigt, wenn es
  mindestens einen Plan gibt.
- **JM-2**: Bei „Jährlich" zeigt die Jahresübersicht Einnahmen, Ausgaben und Überschuss wie bisher als Jahresbeträge
  (JU-2).
- **JM-3**: Bei „Monatlich" zeigt sie für jeden Plan Einnahmen, Ausgaben und Überschuss je Monat: die Jahressumme
  ÷ 12, kaufmännisch auf ganze Cent gerundet, wie die Monatssummen der Planansicht ([MJ-9](monats-jahresbetrag.md)).
  Der monatliche Überschuss ist der Jahresüberschuss ÷ 12, nicht die Differenz der gerundeten Monatswerte; er kann
  daher um einen Cent von ihr abweichen.
- **JM-4**: Die Kennzeichnung als Defizit (AW-3) hängt nicht von der Darstellung ab.
- **JM-5**: Die Veränderung zum Vorjahr (JU-4 bis JU-8) ist in beiden Darstellungen dieselbe und wird aus den
  Jahreswerten berechnet; Monatswerte fließen in keine Rechnung ein ([MJ-8](monats-jahresbetrag.md)).
- **JM-6**: Die Monatswerte rechnet das Backend (JU-9, [MJ-10](monats-jahresbetrag.md)); die Oberfläche wählt nur aus,
  welche Werte sie zeigt.
- **JM-7**: Beim ersten Öffnen ist „Monatlich" gewählt. Die Anwendung merkt sich die Wahl im Browser: Nach dem Öffnen
  eines Plans und „Alle Jahre", nach dem Neuladen und nach einer neuen Anmeldung im selben Browser ist die zuletzt
  gewählte Darstellung wieder gewählt.

## Akzeptanzkriterien

- **Gegeben** ein Benutzer ohne Plan, **dann** zeigt die Jahresübersicht keinen Umschalter (JM-1).
- **Gegeben** 2025 mit Einnahmen 30.000,00 € und Ausgaben 25.000,00 €, 2026 mit Einnahmen 30.900,00 € und Ausgaben
  24.250,00 €, **wenn** der Benutzer die Jahresübersicht zum ersten Mal öffnet, **dann** ist „Monatlich" gewählt, und
  die Zeile 2026 zeigt Einnahmen 2.575,00 € (+3,00 %), Ausgaben 2.020,83 € (−3,00 %) und Überschuss 554,17 €
  (+33,00 %) (JM-3, JM-5, JM-7).
- **Gegeben** dieselben Pläne, **wenn** der Benutzer „Jährlich" wählt, **dann** zeigt die Zeile 2026 Einnahmen
  30.900,00 €, Ausgaben 24.250,00 € und Überschuss 6.650,00 € mit denselben Veränderungen (JM-2, JM-5).
- **Gegeben** Einnahmen 1.000,00 € und Ausgaben 1.100,00 € im Jahr, **dann** zeigt die Zeile bei „Monatlich"
  Überschuss −8,33 € und „Defizit" (JM-3, JM-4).
- **Gegeben** „Jährlich" gewählt, **wenn** der Benutzer einen Plan öffnet und mit „Alle Jahre" zurückkehrt oder die
  Seite neu lädt, **dann** ist „Jährlich" weiter gewählt (JM-7).

## Nicht im Umfang

- Ein Umschalter in der Planansicht; dort stehen Monats- und Jahreswerte schon nebeneinander (MJ-12, MJ-13)
- Monats- und Jahreswerte gleichzeitig in der Jahresübersicht
- Die Wahl über Browser oder Geräte hinweg merken (etwa am Benutzerkonto)

## Umsetzung

- Read Model: `YearlyOverviewEntry.monthlyTotalIncome`, `monthlyTotalExpense`, `monthlyBalance` teilen die
  Jahreswerte mit `Money.perMonth` durch 12 (JM-3, JM-6); die Veränderungen bleiben bei den Jahreswerten (JM-5)
- API: `YearlyOverviewEntryResponse` mit `monthlyTotalIncome`, `monthlyTotalExpense`, `monthlyBalance`
- Frontend: Umschalter mit `AMOUNT_PERIODS` in `budget/yearly-overview-page.ts`, nur bei mindestens einem Plan (JM-1);
  die Seite zeigt je nach Wahl die Monats- oder Jahreswerte aus der API (JM-2, JM-3). `YearlyOverviewStore.period`
  beginnt mit „Monatlich" und merkt sich die Wahl unter `hbt.yearlyOverview.period` im `localStorage` (JM-7)
- Tests: `YearlyOverviewTest`, `HouseholdPlanControllerTest`, `yearly-overview-page.spec.ts`
- E2E: `e2e/tests/jahresuebersicht-monatsbetraege.spec.ts` (JM-1 bis JM-5, JM-7). Die übrigen E2E-Tests, die
  Beträge der Jahresübersicht prüfen, wählen vorher mit `showAmounts` „Jährlich"

## Offene Fragen

Keine. Entschieden bei der Freigabe: Vorauswahl „Monatlich"; die Wahl merkt sich der Browser (JM-7).
