# Auswertung nach der 50/30/20-Regel

Status: umgesetzt

## Zweck

Die Auswertung zeigt, ob der Jahresplan eines Benutzers tragfähig ist: wie viel er einnimmt und ausgibt, ob ein
Defizit entsteht und ob jede Kategorie ihren Zielanteil nach der 50/30/20-Regel einhält. Sie ist der eigentliche Nutzen
der Anwendung.

## Fachliche Regeln

- **AW-1**: Einkommen ist die Summe aller Einnahmen, Ausgaben die Summe aller Ausgaben des Plans.
- **AW-2**: Saldo = Einkommen − Ausgaben. Der Saldo darf negativ sein.
- **AW-3**: Ein Defizit liegt vor, wenn die Ausgaben höher sind als das Einkommen. Gleich hohe Ausgaben sind kein
  Defizit.
- **AW-4**: Die Zielanteile sind fest:

  | Kategorie | Zielanteil | Zielrichtung |
  |---|---|---|
  | Lebensunterhalt | 50 % | höchstens |
  | Freizeit & Spaß | 30 % | höchstens |
  | Sparen & Investieren | 20 % | mindestens |

- **AW-5**: Anteil einer Kategorie = Ausgaben der Kategorie / Einkommen × 100, kaufmännisch gerundet auf zwei
  Nachkommastellen.
- **AW-6**: Ein Ziel ist erreicht, wenn der Anteil bei „höchstens" kleiner oder gleich, bei „mindestens" größer oder
  gleich dem Zielanteil ist. Genau auf der Grenze gilt das Ziel als erreicht.
- **AW-7**: Ohne Einkommen ist jeder Anteil 0 %. Dann gilt eine Obergrenze nur als eingehalten, wenn in der Kategorie
  nichts ausgegeben wird; eine Untergrenze gilt immer als erreicht.
- **AW-8**: Die Auswertung enthält immer alle drei Kategorien, auch solche ohne Ausgaben (mit 0,00 €).
- **AW-10**: Ein Überschuss (positiver Saldo, also Einkommen, das keiner Ausgabe zugeordnet ist)
  zählt in der Auswertung zu Sparen & Investieren: Anteil und Zielerreichung von Sparen & Investieren werden aus den
  Ausgaben der Kategorie plus dem Überschuss berechnet. Zu Freizeit & Spaß zählt nur, was ausdrücklich so erfasst ist.
  Einkommen, Ausgaben und Saldo (AW-1, AW-2) ändern sich dadurch nicht. Bei einem Defizit gibt es keinen Überschuss.
  Die Oberfläche weist den Überschuss in der Aufteilung von Sparen & Investieren getrennt aus („davon nicht verplant").
  Damit ist das Sparziel immer erreicht, wenn Lebensunterhalt und Freizeit & Spaß ihre Obergrenzen einhalten und kein
  Defizit vorliegt; das ist so gewollt.
- **AW-9**: Alle Zahlen der Auswertung rechnet die Domäne (`BudgetSummary`). Read Model und Oberfläche rechnen nichts
  selbst nach.

## Akzeptanzkriterien

- **Gegeben** Einnahmen von 3.000,00 € und Ausgaben Lebensunterhalt 1.500,00 €, Freizeit & Spaß 600,00 €, Sparen &
  Investieren 900,00 €, **dann** zeigt die Auswertung Saldo 0,00 €, kein Defizit, Anteile 50 %, 20 %, 30 % und alle drei
  Ziele als erreicht.
- **Gegeben** Einnahmen von 2.000,00 € und Ausgaben Lebensunterhalt 1.200,00 €, **dann** ist das Ziel für
  Lebensunterhalt überschritten (60 % > 50 %). Die übrigen 800,00 € zählen zu Sparen & Investieren, deren Ziel damit
  erreicht ist (40 % ≥ 20 %, AW-10); Freizeit & Spaß bleibt bei 0 % und gilt als erreicht.
- **Gegeben** Einnahmen von 2.000,00 € und Ausgaben Lebensunterhalt 1.000,00 €, Freizeit & Spaß 600,00 €, Sparen &
  Investieren 100,00 €, **dann** beträgt der Überschuss 300,00 €, Sparen & Investieren zeigt 400,00 € bzw. 20 % und
  gilt als erreicht, Freizeit & Spaß bleibt bei 600,00 € bzw. 30 % (AW-10). Die Oberfläche zeigt bei Sparen &
  Investieren „davon nicht verplant: 300,00 €" und bei Freizeit & Spaß keinen solchen Hinweis.
- **Gegeben** Ausgaben von 100,00 € und keine Einnahmen, **dann** zeigt die Oberfläche die Warnung „Deine Ausgaben
  übersteigen deine Einnahmen.", und der Saldo ist −100,00 €.
- **Gegeben** ein leerer Plan, **dann** sind alle Summen 0,00 €, alle Anteile 0 %, und alle Ziele gelten als erreicht.

## Nicht im Umfang

- Andere Budgetregeln oder vom Benutzer einstellbare Zielanteile
- Vergleich mehrerer Jahre, Diagramme über die Zeit
- Prognosen oder Empfehlungen, wie ein Ziel erreicht werden kann

## Umsetzung

- Domäne: `BudgetSummary` (Summen, Saldo, Defizit, Anteile, Zielerreichung; AW-10: `surplus`, `unplannedIn`,
  `assessedIn`, `SURPLUS_CATEGORY`), `ExpenseCategory.isTargetMet`,
  `TargetDirection.isMet`, `Percentage.shareOf`
- Read Model: `HouseholdPlanOverview`, `CategoryBreakdown`, `OverviewItem` (in `domain/householdplan/overview`), geladen
  über `JdbcHouseholdPlanOverviewQueryHandler`
- API: `GET /api/plans?year=` liefert `HouseholdPlanOverviewResponse`; Beträge als Dezimal-Strings (z. B. `"850.00"`).
  `CategoryBreakdownResponse.total` enthält den Überschuss, `unplanned` weist ihn getrennt aus (AW-10)
- Frontend: `budget/ui/overview-summary.ts` (Summen, Saldo, Warnung), `budget/ui/category-breakdown.ts` (Balken je
  Kategorie, „davon nicht verplant" bei Sparen & Investieren)
- Tests: `BudgetSummaryTest`, `ExpenseCategoryTest`, `PercentageTest`, `HouseholdPlanOverviewTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `overview-summary.spec.ts`, `category-breakdown.spec.ts`
- E2E: `e2e/tests/auswertung.spec.ts`

## Offene Fragen
