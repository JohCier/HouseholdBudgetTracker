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
- **AW-9**: Alle Zahlen der Auswertung rechnet die Domäne (`BudgetSummary`). Read Model und Oberfläche rechnen nichts
  selbst nach.

## Akzeptanzkriterien

- **Gegeben** Einnahmen von 3.000,00 € und Ausgaben Lebensunterhalt 1.500,00 €, Freizeit & Spaß 600,00 €, Sparen &
  Investieren 900,00 €, **dann** zeigt die Auswertung Saldo 0,00 €, kein Defizit, Anteile 50 %, 20 %, 30 % und alle drei
  Ziele als erreicht.
- **Gegeben** Einnahmen von 2.000,00 € und Ausgaben Lebensunterhalt 1.200,00 €, **dann** ist das Ziel für
  Lebensunterhalt überschritten (60 % > 50 %), und das Ziel für Sparen & Investieren ist nicht erreicht (0 % < 20 %).
- **Gegeben** Ausgaben von 100,00 € und keine Einnahmen, **dann** zeigt die Oberfläche die Warnung „Deine Ausgaben
  übersteigen deine Einnahmen.", und der Saldo ist −100,00 €.
- **Gegeben** ein leerer Plan, **dann** sind alle Summen 0,00 €, alle Anteile 0 %, und alle Ziele gelten als erreicht.

## Nicht im Umfang

- Andere Budgetregeln oder vom Benutzer einstellbare Zielanteile
- Vergleich mehrerer Jahre, Diagramme über die Zeit
- Prognosen oder Empfehlungen, wie ein Ziel erreicht werden kann

## Umsetzung

- Domäne: `BudgetSummary` (Summen, Saldo, Defizit, Anteile, Zielerreichung), `ExpenseCategory.isTargetMet`,
  `TargetDirection.isMet`, `Percentage.shareOf`
- Read Model: `HouseholdPlanOverview`, `CategoryBreakdown`, `OverviewItem` (in `domain/householdplan/overview`), geladen
  über `JdbcHouseholdPlanOverviewQueryHandler`
- API: `GET /api/plans?year=` liefert `HouseholdPlanOverviewResponse`; Beträge als Dezimal-Strings (z. B. `"850.00"`)
- Frontend: `budget/ui/overview-summary.ts` (Summen, Saldo, Warnung), `budget/ui/category-breakdown.ts` (Balken je
  Kategorie)
- Tests: `BudgetSummaryTest`, `ExpenseCategoryTest`, `PercentageTest`, `HouseholdPlanOverviewTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `overview-summary.spec.ts`, `category-breakdown.spec.ts`

## Offene Fragen

- Soll ein Überschuss (Einkommen, das keiner Kategorie zugeordnet ist) als Sparen gewertet werden? Heute zählt nur, was
  ausdrücklich als „Sparen & Investieren" erfasst ist.
