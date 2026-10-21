# Tortendiagramm der 50/30/20-Regel

Status: umgesetzt

## Zweck

Die Auswertung zeigt heute je Kategorie einen Balken mit dem Anteil am Einkommen (siehe
[auswertung-50-30-20.md](auswertung-50-30-20.md)). Wie sich das Einkommen insgesamt auf die drei Töpfe verteilt, sieht
man daran schlecht. Mit dieser Spec kann der Benutzer die Karte „50/30/20-Regel" zusätzlich als Tortendiagramm ansehen
und jederzeit zwischen beiden Darstellungen wechseln.

## Fachliche Regeln

- **TD-1**: Die Karte „50/30/20-Regel" hat zwei Darstellungen: „Balken" (die bisherige Ansicht mit Prozentangaben) und
  „Torte". Ein Umschalter in der Karte wechselt zwischen beiden. Beim Öffnen der Übersicht ist „Balken" gewählt.
- **TD-2**: Die Torte hat je Kategorie ein Stück: Lebensunterhalt, Freizeit & Spaß, Sparen & Investieren. Jede Kategorie
  hat eine feste Farbe, die in Torte und Legende gleich ist.
- **TD-3**: Die Größe eines Stücks entspricht der Summe der Kategorie aus der Auswertung, bei Sparen & Investieren
  einschließlich des Überschusses (AW-10). Ohne Defizit ergeben die drei Stücke damit genau das Einkommen, und jedes
  Stück entspricht dem Anteil der Kategorie am Einkommen.
- **TD-4**: Bei einem Defizit (AW-3) zeigt die Torte die Aufteilung der Ausgaben; die Stücke ergeben dann zusammen mehr
  als das Einkommen. Die Legende zeigt weiterhin den Anteil am Einkommen (AW-5), sodass die Prozente zusammen über
  100 % liegen können.
- **TD-5**: Neben der Torte steht eine Legende mit je Kategorie: Name, Summe in Euro je Monat ([MJ-14](monats-jahresbetrag.md)), Anteil am Einkommen, Zielanteil
  mit Zielrichtung („von max. 50 %", „von mind. 20 %") und bei verfehltem Ziel der Hinweis „Ziel überschritten" bzw.
  „Ziel nicht erreicht" – mit denselben Texten wie in der Balkenansicht. Bei Sparen & Investieren weist die Legende
  wie die Balkenansicht den Überschuss getrennt aus („davon nicht verplant", AW-10).
- **TD-6**: Kategorien mit 0,00 € bekommen kein Stück, stehen aber in der Legende (AW-8).
- **TD-7**: Sind alle drei Summen 0,00 €, zeigt die Torte statt der Stücke einen leeren grauen Kreis mit dem Hinweis
  „Noch keine Ausgaben oder Einnahmen".
- **TD-8**: Die Torte rechnet keine Anteile oder Zielerreichungen selbst; alle Zahlen kommen aus der Auswertung (AW-9).
  Die Oberfläche teilt den Kreis nur im Verhältnis der Summen auf.
- **TD-9**: Die Torte ist für Screenreader beschriftet: Jedes Stück nennt Kategorie, Summe und Anteil am Einkommen.

## Akzeptanzkriterien

- **Gegeben** ein Plan mit Einträgen, **wenn** der Benutzer die Übersicht öffnet, **dann** zeigt die Karte
  „50/30/20-Regel" die Balkenansicht, und im Umschalter ist „Balken" gewählt (TD-1).
- **Gegeben** Einnahmen von 3.000,00 € und Ausgaben Lebensunterhalt 1.500,00 €, Freizeit & Spaß 600,00 €, Sparen &
  Investieren 900,00 €, **wenn** der Benutzer auf „Torte" umschaltet, **dann** zeigt die Karte eine Torte mit drei
  Stücken und eine Legende mit 50 %, 20 % und 30 % und ohne Zielhinweis (TD-2, TD-3, TD-5).
- **Gegeben** Einnahmen von 2.000,00 € und Ausgaben Lebensunterhalt 1.200,00 €, **wenn** der Benutzer auf „Torte"
  umschaltet, **dann** zeigt die Legende bei Lebensunterhalt 60 % und „Ziel überschritten", bei Sparen & Investieren
  „davon nicht verplant: 800,00 €" und 40 %; Freizeit & Spaß steht mit 0,00 € in der Legende, hat aber kein Stück (TD-3, TD-5, TD-6).
- **Gegeben** die Tortenansicht ist gewählt, **wenn** der Benutzer auf „Balken" umschaltet, **dann** sieht er wieder
  die bisherige Ansicht mit den Balken (TD-1).
- **Gegeben** die Tortenansicht ist gewählt, **wenn** der Benutzer einen Posten erfasst, **dann** bleibt die
  Tortenansicht gewählt und zeigt die neuen Zahlen (TD-1, TD-3).
- **Gegeben** Ausgaben Lebensunterhalt 100,00 € und keine Einnahmen, **wenn** der Benutzer auf „Torte" umschaltet,
  **dann** besteht die Torte nur aus dem Stück Lebensunterhalt (TD-4).
- **Gegeben** ein leerer Plan, **wenn** der Benutzer auf „Torte" umschaltet, **dann** zeigt die Karte einen leeren
  Kreis mit „Noch keine Ausgaben oder Einnahmen" und alle drei Kategorien mit 0,00 € in der Legende (TD-7).

## Nicht im Umfang

- Andere Diagrammarten (Säulen, Ringe über mehrere Jahre, Verlauf über die Zeit)
- Eine zweite Torte mit der Soll-Aufteilung 50/30/20 zum Vergleich
- Aufteilung einer Kategorie nach einzelnen Posten; seit [torte-ausschnitt.md](torte-ausschnitt.md) nach
  Unterkategorien und Posten möglich (TK-1)
- Speichern der gewählten Darstellung über das Neuladen der Seite oder die Abmeldung hinaus; beim Wechsel des Jahres
  bleibt sie erhalten
- Ein eigenes Stück „Defizit"; bei einem Defizit zeigt die Torte die Aufteilung der Ausgaben (TD-4)
- Markierung der Zielanteile 50/30/20 in der Torte selbst; die Ziele stehen in der Legende (TD-5)
- Änderungen an Backend, API oder Auswertung

## Umsetzung

- Frontend: `budget/ui/category-breakdown.ts` (Umschalter „Balken | Torte", Ansicht als `model` `view`),
  `budget/ui/category-pie.ts` (Torte als eigenes SVG ohne Diagramm-Bibliothek, Legende), `BreakdownView`,
  `targetLabel` und `missedTargetLabel` in `budget/budget.model.ts`. Die gewählte Darstellung hält
  `budget/budget-dashboard.ts` (`breakdownView`), damit sie den Wechsel des Jahres übersteht
- Farben je Kategorie als CSS-Variablen in `category-pie.scss`, mit eigenen Werten für den dunklen Modus
- Tests: `category-pie.spec.ts`, `category-breakdown.spec.ts`
- E2E: `e2e/tests/tortendiagramm.spec.ts`

## Offene Fragen
