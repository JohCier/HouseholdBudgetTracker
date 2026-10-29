# Jahresübersicht

Status: umgesetzt

## Zweck

Heute landet der Benutzer nach der Anmeldung im Plan des aktuellen Jahres und sieht andere Jahre nur, wenn er mit den
Pfeilen einzeln dorthin wechselt. Wie sich Einnahmen, Ausgaben und Überschuss über die Jahre entwickeln, sieht er
nirgends.

Die Jahresübersicht listet alle Haushaltspläne des Benutzers mit Jahr, Einnahmen, Ausgaben und Überschuss und zeigt je
Jahr die Veränderung gegenüber dem Vorjahr in Prozent. Sie ist die Startseite nach der Anmeldung; von dort öffnet der
Benutzer den Plan eines Jahres.

## Fachliche Regeln

### Inhalt

- **JU-1**: Die Jahresübersicht enthält alle Pläne des angemeldeten Benutzers (HP-4) mit Jahr im erlaubten Bereich
  (HP-1), das neueste Jahr oben. Pläne anderer Benutzer erscheinen nie.
- **JU-2**: Je Plan zeigt sie das Jahr, die Einnahmen (AW-1), die Ausgaben (AW-1) und den Überschuss. Der Überschuss
  ist der Saldo nach AW-2 (Einnahmen − Ausgaben) und darf negativ sein; ein negativer Überschuss wird als Defizit
  gekennzeichnet (AW-3). Die Beträge sind Jahresbeträge oder, nach Wahl des Benutzers, Monatsbeträge
  ([JM-1 bis JM-7](jahresuebersicht-monatsbetraege.md)).
- **JU-3**: Ein Plan ohne Posten erscheint mit 0,00 € in allen drei Spalten.

### Veränderung zum Vorjahr

- **JU-4**: Vorjahr eines Plans ist der Plan des unmittelbar vorhergehenden Kalenderjahres. Gibt es keinen Plan für
  dieses Jahr, zeigt die Zeile keine Veränderung, auch wenn es ältere Pläne gibt (2026 wird nicht mit 2024
  verglichen, wenn 2025 fehlt).
- **JU-5**: Die Veränderung wird für Einnahmen, Ausgaben und Überschuss getrennt berechnet:
  (Wert des Jahres − Wert des Vorjahres) ÷ |Wert des Vorjahres| × 100, kaufmännisch auf zwei Nachkommastellen gerundet.
  Durch den Betrag im Nenner bedeutet ein positiver Wert immer „mehr als im Vorjahr", auch wenn der Überschuss im
  Vorjahr negativ war (−1.000 € → −500 € ist +50,00 %).
- **JU-6**: Ist der Wert des Vorjahres 0,00 €, gibt es für diese Spalte keine Veränderung in Prozent; angezeigt wird
  „–".
- **JU-7**: Die Veränderung wird mit Vorzeichen angezeigt: „+3,00 %", „−3,00 %", „±0,00 %".
- **JU-8**: Die Veränderung wird nach ihrer Bedeutung eingefärbt: mehr Einnahmen und mehr Überschuss grün, mehr
  Ausgaben rot, jeweils umgekehrt bei weniger; keine Veränderung neutral. Die Farbe ist nie die einzige Information,
  Vorzeichen und Text bleiben lesbar.
- **JU-9**: Alle Zahlen der Jahresübersicht, auch die Veränderungen, rechnet das Backend (wie AW-9); die Oberfläche
  rechnet nichts selbst.

### Navigation

- **JU-10**: Nach Anmeldung und Registrierung zeigt die Anwendung die Jahresübersicht unter `/`. Sie ist auch die
  Startseite für angemeldete Benutzer, die die Anwendung öffnen.
- **JU-11**: Ein Klick auf eine Zeile öffnet den Plan dieses Jahres unter `/jahre/<Jahr>`, wie er bisher aussah
  (Posten, Auswertung, Wechsel des Jahres mit den Pfeilen). Von dort führt „Alle Jahre" zurück zur Jahresübersicht,
  die dann die Änderungen am Plan berücksichtigt. Eine Adresse mit einem Jahr außerhalb von HP-1 führt zur
  Jahresübersicht.
- **JU-12**: Die Jahresübersicht bietet „Haushaltsplan anlegen" für das aktuelle Jahr (Zeitzone des Browsers) an,
  solange es dafür keinen Plan gibt, mit dem Hinweis „Für <Jahr> gibt es noch keinen Haushaltsplan.". Nach dem
  Anlegen öffnet sich der neue Plan. Pläne anderer Jahre legt der
  Benutzer wie bisher in der Planansicht über die Pfeile an.
- **JU-13**: Hat der Benutzer noch keinen Plan, zeigt die Jahresübersicht statt dieses Hinweises „Noch keine
  Haushaltspläne." und „Haushaltsplan anlegen".

## Akzeptanzkriterien

- **Gegeben** ein Benutzer mit Plänen für 2025 und 2026, **wenn** er sich anmeldet, **dann** sieht er die
  Jahresübersicht mit den Zeilen 2026 und 2025 in dieser Reihenfolge (JU-1, JU-10).
- **Gegeben** 2025 mit Einnahmen 30.000,00 € und Ausgaben 25.000,00 €, 2026 mit Einnahmen 30.900,00 € und Ausgaben
  24.250,00 €, **dann** zeigt die Zeile 2026 Einnahmen 30.900,00 € (+3,00 %), Ausgaben 24.250,00 € (−3,00 %) und
  Überschuss 6.650,00 € (+33,00 %); Einnahmen und Überschuss grün, Ausgaben grün (JU-2, JU-5, JU-7, JU-8).
- **Gegeben** Pläne für 2024 und 2026, aber nicht für 2025, **dann** zeigen die Zeilen 2024 und 2026 keine Veränderung
  (JU-4).
- **Gegeben** 2025 ohne Einnahmen und 2026 mit Einnahmen 1.000,00 €, **dann** zeigt 2026 bei den Einnahmen „–" (JU-6).
- **Gegeben** 2025 mit Überschuss −1.000,00 € und 2026 mit Überschuss −500,00 €, **dann** zeigt 2026 beim Überschuss
  +50,00 % und kennzeichnet −500,00 € als Defizit (JU-2, JU-5).
- **Gegeben** ein leerer Plan für 2026, **dann** zeigt die Zeile 2026 dreimal 0,00 € (JU-3).
- **Gegeben** die Jahresübersicht, **wenn** der Benutzer auf die Zeile 2025 klickt, **dann** öffnet sich der Plan 2025;
  nach dem Erfassen einer Einnahme und „Alle Jahre" zeigt die Zeile 2025 die neuen Einnahmen (JU-11).
- **Gegeben** ein neu registrierter Benutzer, **dann** zeigt die Jahresübersicht „Noch keine Haushaltspläne."; nach
  „Haushaltsplan anlegen" ist der Plan des aktuellen Jahres geöffnet (JU-12, JU-13).
- **Gegeben** Benutzer A und B mit je einem Plan, **wenn** A die Jahresübersicht abruft, **dann** enthält sie nur den
  Plan von A (JU-1).

## Nicht im Umfang

- Veränderung der einzelnen Kategorien der 50/30/20-Regel zum Vorjahr
- Diagramme über mehrere Jahre, außer dem Verlauf des Nettovermögens in der Karte „Vermögen": siehe
  [vermoegen.md](vermoegen.md)
- Vergleich mit einem frei gewählten Jahr statt dem Vorjahr
- Pläne aus der Jahresübersicht löschen und kopieren: siehe [plan-loeschen.md](plan-loeschen.md) und
  [plan-uebernehmen.md](plan-uebernehmen.md)
- Monatsbeträge in der Jahresübersicht: siehe [jahresuebersicht-monatsbetraege.md](jahresuebersicht-monatsbetraege.md)

## Umsetzung

- Domäne: JU-5, JU-6 in `Percentage.changeFrom`; das Read Model `YearlyOverview` (JU-1, JU-4) mit
  `YearlyOverviewEntry` (JU-2) nimmt Summen und Saldo aus `BudgetSummary` (AW-1 bis AW-3)
- Lesen: `HouseholdPlanOverviewQueryHandler.findYearlyOverviewByOwner`, umgesetzt in
  `JdbcHouseholdPlanOverviewQueryHandler` mit einer Abfrage, die die Posten je Plan, Art und Kategorie summiert;
  `HouseholdPlanApplicationService.getYearlyOverview` begrenzt auf die Jahre aus HP-1
- API: `GET /api/plans/years` mit `YearlyOverviewResponse`; Veränderungen sind Zahlen in Prozent oder `null` (JU-6)
- Frontend: `budget/yearly-overview-page.ts` unter `/` mit `budget/data-access/yearly-overview-store.ts` (JU-10,
  JU-12, JU-13), Anzeige der Veränderung in `budget/ui/year-change.ts` und `budget/year-change.ts` (JU-7, JU-8);
  `budget/budget-dashboard.ts` unter `/jahre/:year`, das Jahr kommt aus der Adresse (`parseYear` in `budget/year.ts`),
  die Pfeile ändern die Adresse, „Alle Jahre" führt zurück (JU-11)
- Tests: `PercentageTest`, `YearlyOverviewTest`, `HouseholdPlanApplicationServiceTest`, `HouseholdPlanOverviewControllerTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `year-change.spec.ts`, `yearly-overview-page.spec.ts`,
  `budget-dashboard.spec.ts`, `year.spec.ts`, `budget-api.spec.ts`
- E2E: `e2e/tests/jahresuebersicht.spec.ts`

## Offene Fragen
