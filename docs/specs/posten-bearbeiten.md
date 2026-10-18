# Posten bearbeiten

Status: geplant

## Zweck

Heute lässt sich ein Posten nur löschen und neu anlegen (PO-7). Wer sich bei Betrag, Beschreibung oder Kategorie
vertippt hat oder einen Betrag anpassen will, muss den Posten erneut vollständig erfassen, und der Posten rutscht in der
Liste nach oben. Mit dieser Spec kann der Besitzer eines Plans einen bestehenden Posten direkt ändern.

Mit der Umsetzung entfallen PO-7, „Posten bearbeiten" unter „Nicht im Umfang" in [posten.md](posten.md) und das
Nicht-Ziel „Keine Bearbeitung eines Postens" in [produkt.md](../produkt.md).

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

Noch nicht umgesetzt. Vorgesehen:

- Domäne: `HouseholdPlan.changeItem`, `BudgetItem` erhält eine Methode, die eine geänderte Kopie mit gleicher ID
  liefert; Command `ChangeBudgetItemCommand`; Event `BudgetItemChanged`
- API: `PUT /api/plans/{planId}/items/{itemId}` mit Beschreibung, Betrag und Kategorie, Antwort 204; Fehler als
  RFC 9457 Problem Details; OpenAPI-Vertrag und TypeScript-Typen neu erzeugt
- Persistenz: `UPDATE` der Zeile, `created_at` bleibt unverändert
- Frontend: Schaltfläche „Bearbeiten" in `budget-item-table.ts`, Dialog mit den Feldern und Prüfungen aus
  `add-item-form.ts`, `BudgetStore.changeItem`
- Tests: `BudgetItemTest`, `HouseholdPlanTest`, `HouseholdPlanApplicationServiceTest`, `HouseholdPlanControllerTest`,
  `HouseholdPlanIntegrationTest`, `JdbcHouseholdPlanRepositoryTest`, `budget-item-table.spec.ts`,
  `budget-store.spec.ts`, Spec für den Dialog

## Offene Fragen

- Soll sich die Art des Postens ändern lassen (PB-2)? Vorschlag: nein, weil sich damit auch die Pflicht zur Kategorie
  ändert und Löschen plus Neuanlegen dafür reicht.
- Soll der geänderte Posten seinen Platz behalten (PB-4) oder wie ein neuer Posten nach oben wandern? Vorschlag: Platz
  behalten.
- Bearbeiten im Dialog oder direkt in der Tabellenzeile? Vorschlag: Dialog, weil er die Prüfungen des Formulars
  wiederverwendet und auf schmalen Bildschirmen besser funktioniert.
- Soll es wie beim Löschen ein „Rückgängig" geben (PB-8)? Vorschlag: nein.
