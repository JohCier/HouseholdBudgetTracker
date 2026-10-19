# Posten bearbeiten

Status: umgesetzt

## Zweck

Heute lässt sich ein Posten nur löschen und neu anlegen (PO-7). Wer sich bei Betrag, Beschreibung oder Kategorie
vertippt hat oder einen Betrag anpassen will, muss den Posten erneut vollständig erfassen, und der Posten rutscht in der
Liste nach oben. Mit dieser Spec kann der Besitzer eines Plans einen bestehenden Posten direkt ändern.

Diese Spec ersetzt die frühere Regel PO-7 („Ein Posten kann nicht bearbeitet werden") in [posten.md](posten.md) und
das frühere Nicht-Ziel „Keine Bearbeitung eines Postens" in [produkt.md](../produkt.md).

## Fachliche Regeln

- **PB-1**: Der Besitzer eines Plans kann Beschreibung, Betrag und, bei einer Ausgabe, die Kategorie eines Postens
  ändern (siehe [HP-4](haushaltsplan.md)).
- **PB-2**: Die Art des Postens (Einnahme oder Ausgabe) lässt sich nicht ändern. Dafür wird der Posten weiterhin
  gelöscht und neu angelegt.
- **PB-3**: Für die geänderten Werte gelten dieselben Regeln wie beim Erfassen (PO-3, PO-4, PO-5). Verletzt ein Wert
  eine Regel, bleibt der Posten unverändert.
- **PB-4**: Ein geänderter Posten behält seine ID und seinen Platz in der Übersicht; maßgeblich für die Reihenfolge
  (PO-10) bleibt der Zeitpunkt der ursprünglichen Erfassung.
- **PB-5**: Eine Änderung wird als Domain Event `BudgetItemChanged` protokolliert (HP-7). Bleiben alle Werte gleich,
  wird die Anfrage angenommen, aber nichts protokolliert.
- **PB-6**: Das Ändern eines Postens, der nicht (mehr) zum Plan gehört, wird abgelehnt (wie PO-8).
- **PB-7**: Die Auswertung (Summen, Saldo, Anteile, Zielerreichung) zeigt nach dem Speichern sofort die geänderten
  Werte.
- **PB-8**: Eine Änderung kann nicht rückgängig gemacht werden; der Benutzer ändert den Posten erneut.

## Akzeptanzkriterien

- **Gegeben** eine Ausgabe „Miete", 850,00 €, Lebensunterhalt, **wenn** der Benutzer den Betrag auf 900,00 € ändert,
  **dann** antwortet die API mit 204, die Übersicht zeigt „Miete", 900,00 €, Lebensunterhalt mit derselben ID an
  derselben Stelle, und die Summe für Lebensunterhalt steigt um 50,00 €.
- **Gegeben** eine Ausgabe in Freizeit & Spaß, **wenn** der Benutzer die Kategorie auf Sparen & Investieren ändert,
  **dann** verschiebt sich ihr Betrag in der Aufteilung je Kategorie entsprechend.
- **Gegeben** eine Einnahme, **wenn** beim Ändern eine Kategorie mitgeschickt wird, **dann** antwortet die API mit 400.
- **Gegeben** eine Ausgabe, **wenn** sie ohne Kategorie geändert werden soll, **dann** antwortet die API mit 400.
- **Gegeben** ein Posten, **wenn** ein Betrag von 0, ein negativer Betrag, 10,005 oder eine leere Beschreibung gesendet
  wird, **dann** antwortet die API mit 400, und die Oberfläche zeigt dieselben Meldungen wie beim Erfassen, ohne die
  Anfrage zu senden.
- **Gegeben** eine Posten-ID, die nicht zum Plan gehört, **wenn** sie geändert werden soll, **dann** antwortet die API
  mit 404.
- **Gegeben** der Plan eines anderen Benutzers, **wenn** ein Posten darin geändert werden soll, **dann** antwortet die
  API mit 404 (HP-4).
- **Gegeben** die Tabelle der Posten, **wenn** der Benutzer bei einem Posten „Bearbeiten" wählt, **dann** öffnet sich
  ein Dialog mit den bisherigen Werten; „Speichern" übernimmt die Änderung, „Abbrechen" lässt den Posten unverändert.
- **Gegeben** eine erfolgreiche Änderung, **dann** wird ein Event `BudgetItemChanged` protokolliert; bei unveränderten
  Werten nicht.

## Nicht im Umfang

- Art des Postens ändern (Einnahme ↔ Ausgabe)
- Posten in einen anderen Plan bzw. ein anderes Jahr verschieben
- Änderungsverlauf eines Postens anzeigen
- Änderung rückgängig machen

## Umsetzung

- Domäne: `HouseholdPlan.changeItem` (PB-1, PB-4 bis PB-6), `BudgetItem.changedTo` behält ID und Art und prüft die
  Regeln wie beim Erfassen (PB-2, PB-3), `BudgetItem.hasSameValuesAs` (PB-5)
- Command `ChangeBudgetItemCommand`, Event `BudgetItemChanged`, `HouseholdPlanApplicationService.changeItem`
- Persistenz: `JdbcHouseholdPlanRepository.save` vergleicht mit den gespeicherten Posten und schreibt nur neue,
  geänderte und entfernte; ein geänderter Posten bekommt ein `UPDATE`, `created_at` bleibt unverändert (PB-4)
- API: `PUT /api/plans/{planId}/items/{itemId}` mit `ChangeBudgetItemRequest` (Beschreibung, Betrag, Kategorie nur bei
  Ausgaben), Antwort 204; 400 bei ungültigen Werten, 404 bei unbekanntem Plan oder Posten
- Frontend: Schaltfläche „Bearbeiten" in `budget/ui/budget-item-table.ts`, Dialog `budget/ui/edit-item-dialog.ts` mit
  den Prüfungen aus `budget/ui/item-validators.ts`, `BudgetStore.changeItem`, `BudgetApi.changeItem`; geöffnet aus
  `budget-dashboard.ts`
- Tests: `BudgetItemTest`, `HouseholdPlanTest`, `HouseholdPlanApplicationServiceTest`, `HouseholdPlanControllerTest`,
  `JdbcHouseholdPlanRepositoryTest`, `HouseholdPlanIntegrationTest`, `edit-item-dialog.spec.ts`,
  `budget-item-table.spec.ts`, `budget-store.spec.ts`, `budget-api.spec.ts`, `budget-dashboard.spec.ts`
- E2E: `e2e/tests/posten-bearbeiten.spec.ts` (PB-1 bis PB-4, PB-7, PB-8; PB-5, PB-6 und die 404-Fälle decken
  die Backend-Tests ab)

## Offene Fragen
