# Einmalposten

Status: umgesetzt

## Zweck

Manche Einnahmen und Ausgaben kommen nur einmal im Jahr vor, etwa eine Nachzahlung der Nebenkosten oder eine
Steuererstattung. Erfasst der Benutzer sie heute als Posten im Haushaltsplan, gelten sie als Jahresbetrag, der sich
gleichmäßig auf alle Monate verteilt: Eine Nachzahlung von 1.200,00 € erscheint als 100,00 € im Monat. Damit verfälscht
sie die Monatssummen, die Bilanz, das Sparpotential und die 50/30/20-Auswertung, und beim Übernehmen des Plans
wandert sie ins Folgejahr.

Diese Spec führt **Einmalposten** ein: einzelne Einnahmen und Ausgaben mit genauem Datum, getrennt vom Haushaltsplan.
Sie erscheinen in der Planansicht in einem eigenen Abschnitt und in der Jahresübersicht in einer eigenen Spalte, ohne
den Plan oder die 50/30/20-Auswertung zu berühren.

Diese Spec ändert das Nicht-Ziel „Keine einzelnen Buchungen mit Datum" in [produkt.md](../produkt.md): Ein Datum gibt
es ausschließlich bei Einmalposten, Posten des Haushaltsplans bleiben ohne Datum. Sie ändert außerdem JU-1 und JU-2 in
[jahresuebersicht.md](jahresuebersicht.md).

## Fachliche Regeln

### Einmalposten

- **EP-1**: Ein Einmalposten ist eine Einnahme oder eine Ausgabe mit Beschreibung (wie Posten, 1 bis 255 Zeichen),
  Betrag (größer als 0,00 €), Datum (Tag, Monat, Jahr, ohne Uhrzeit) und optional einer Notiz (wie bei Posten,
  höchstens 1000 Zeichen, [PN-…](posten-notiz.md)). Eine Kategorie, Unterkategorie, Markierung oder ein
  Sparpotential hat er nicht.
- **EP-2**: Der Betrag eines Einmalpostens ist immer der Betrag dieses einen Vorgangs. Es gibt keine Wahl zwischen
  Monats- und Jahresbetrag, und er wird nie auf Monate verteilt.
- **EP-3**: Das Jahr eines Einmalpostens ist das Jahr seines Datums. Erlaubt sind Daten vom 01.01.1900 bis zum
  31.12.2200 (wie HP-1).
- **EP-4**: Ein Einmalposten gehört dem Benutzer, der ihn erfasst hat. Nur dieser sieht, ändert und löscht ihn; fremde
  Einmalposten werden behandelt, als gäbe es sie nicht (wie HP-4).
- **EP-5**: Einmalposten sind unabhängig vom Haushaltsplan. Der Benutzer kann sie auch für ein Jahr ohne Plan
  erfassen. Plan löschen ([plan-loeschen.md](plan-loeschen.md)) löscht keine Einmalposten, Plan übernehmen
  ([plan-uebernehmen.md](plan-uebernehmen.md)) kopiert keine.
- **EP-6**: Einmalposten fließen in keine Summe des Haushaltsplans ein: nicht in die Bilanz (BI-1), die Einträge, das
  Sparpotential und nicht in die 50/30/20-Auswertung (AW-1 bis AW-9) oder das Tortendiagramm.
- **EP-7**: Der Benutzer kann einen Einmalposten erfassen, bearbeiten (Art, Beschreibung, Betrag, Datum, Notiz) und
  löschen. Ändert er das Datum in ein anderes Jahr, gehört der Einmalposten danach zu diesem Jahr.
- **EP-17**: Gelöscht wird sofort, ohne Rückfrage; die Meldung bietet „Rückgängig“ wie bei Posten (ES-10, ES-11 in
  [einmalposten-sortierung.md](einmalposten-sortierung.md)).

### Abschnitt in der Planansicht

- **EP-8**: Die Planansicht eines Jahres (`/jahre/<Jahr>`) zeigt im Tab „Einmalposten“ (AP-8) einen eigenen Abschnitt
  „Einmalposten" mit allen Einmalposten dieses Jahres, oben die Einnahmen, darunter die Ausgaben, je in einem eigenen
  Bereich und in der Reihenfolge, die der Benutzer festlegt (ES-1, ES-4 bis ES-8). Je Zeile stehen Datum,
  Beschreibung und Betrag (ES-2); eine Notiz steht wie bei Posten unter der Beschreibung.
- **EP-9**: Jeder Bereich endet mit der Summe seiner Einmalposten („Summe Einnahmen“, „Summe Ausgaben“); unter beiden
  steht der einmalige Saldo (Einnahmen − Ausgaben) des Jahres (ES-1). Ein negativer Saldo steht in der Fehlerfarbe.
- **EP-10**: Der Abschnitt ist auch in einem Jahr ohne Haushaltsplan da, im Tab „Einmalposten“ neben dem Hinweis „Für
  <Jahr> gibt es noch keinen Haushaltsplan.“ (AP-13). Ohne Einmalposten zeigt er „Keine Einmalposten." und „Einmalposten erfassen".
- **EP-11**: Beim Erfassen ist das Datum mit dem heutigen Tag vorbelegt, wenn er im angezeigten Jahr liegt, sonst mit
  dem 01.01. des angezeigten Jahres.

### Jahresübersicht

- **EP-12**: Die Jahresübersicht enthält jedes Jahr, für das der Benutzer einen Plan oder mindestens einen
  Einmalposten hat (ändert JU-1). Ein Jahr nur mit Einmalposten zeigt in Einnahmen, Ausgaben und Überschuss des Plans
  „–"; ein Klick auf die Zeile öffnet `/jahre/<Jahr>` wie bei JU-11. Sie hat keine Knöpfe zum Übernehmen oder
  Löschen eines Plans (PU-12, PL-7), weil es keinen Plan gibt.
- **EP-13**: Die Spalten Einnahmen, Ausgaben und Überschuss stehen unter der gemeinsamen Überschrift
  „Haushaltsplan". Rechts daneben stehen unter der gemeinsamen Überschrift „Einmalig" drei Spalten:
  „Einnahmen", „Ausgaben" und „Saldo" der Einmalposten des Jahres (EP-9). Ein Jahr ohne Einmalposten zeigt dreimal
  0,00 €. Ein negativer einmaliger Saldo ist wie ein Defizit gekennzeichnet (AW-3). Einnahmen, Ausgaben und Überschuss
  des Plans bleiben unverändert (JU-2); eine Spalte „Gesamt" gibt es nicht.
- **EP-14**: Die Spalten „Einmalig" zeigen immer Jahreswerte, auch wenn „Monatlich" gewählt ist (JM-3). Die
  Überschrift sagt das: „Einmalig (Jahr)".
- **EP-15**: Für die Spalten „Einmalig" gibt es keine Veränderung zum Vorjahr.
- **EP-16**: Alle Summen rechnet das Backend (wie JU-9).
- **EP-18**: Auf schmalen Bildschirmen ([MA-…](mobile-ansicht.md)) stehen die Werte „Einmalig" einer Zeile in einer
  eigenen Zeile „Einmalig (Jahr)" unter den Werten des Plans, ohne waagerechtes Scrollen.

## Akzeptanzkriterien

- **Gegeben** der Plan 2026 mit Einnahmen „Gehalt" 3.000,00 € monatlich, **wenn** der Benutzer die Einmalposten
  „Nebenkostennachzahlung" (Ausgabe, 1.200,00 €, 15.03.2026) und „Steuererstattung" (Einnahme, 800,00 €, 10.07.2026)
  erfasst, **dann** zeigt der Abschnitt „Einmalposten" beide in dieser Reihenfolge, Einnahmen 800,00 €, Ausgaben
  1.200,00 € und Saldo −400,00 € in der Fehlerfarbe; die Bilanz zeigt weiter Gesamteinnahmen 3.000,00 € im Monat und
  36.000,00 € im Jahr (EP-1, EP-6, EP-8, EP-9).
- **Gegeben** derselbe Benutzer, **wenn** er „Jährlich" in der Jahresübersicht wählt, **dann** zeigt die Zeile 2026
  Einnahmen 36.000,00 €, Überschuss 36.000,00 € und unter „Einmalig (Jahr)" Einnahmen 800,00 €, Ausgaben 1.200,00 € und
  Saldo −400,00 € als Defizit; bei „Monatlich" Einnahmen 3.000,00 € und unter „Einmalig (Jahr)" weiterhin 800,00 €,
  1.200,00 € und −400,00 € (EP-13, EP-14).
- **Gegeben** kein Plan für 2027, **wenn** der Benutzer in `/jahre/2027` den Einmalposten „Bonus" (Einnahme,
  2.000,00 €, 01.12.2027) erfasst, **dann** zeigt die Jahresübersicht eine Zeile 2027 mit „–" in Einnahmen, Ausgaben
  und Überschuss und unter „Einmalig (Jahr)" Einnahmen 2.000,00 €, Ausgaben 0,00 € und Saldo 2.000,00 € (EP-5, EP-10,
  EP-12, EP-13).
- **Gegeben** der Plan 2026 mit Einmalposten in 2026, **wenn** der Benutzer den Plan löscht, **dann** bleiben die
  Einmalposten erhalten, und `/jahre/2026` zeigt sie unter „Für 2026 gibt es noch keinen Haushaltsplan." (EP-5,
  EP-10).
- **Gegeben** der Plan 2026 mit Einmalposten, **wenn** der Benutzer ihn nach 2027 übernimmt, **dann** hat 2027 keine
  Einmalposten (EP-5).
- **Gegeben** der Einmalposten „Steuererstattung" am 10.07.2026, **wenn** der Benutzer das Datum auf 05.01.2027
  ändert, **dann** steht er nicht mehr im Abschnitt von 2026, sondern in dem von 2027 (EP-7).
- **Gegeben** der Einmalposten „Bonus" mit der Notiz „Jahresprämie", **dann** steht die Notiz unter der Beschreibung
  (EP-1, EP-8).
- **Gegeben** der Einmalposten „Bonus", **wenn** der Benutzer „Löschen" wählt, **dann** ist er sofort entfernt, die
  Summen sind angepasst, und die Meldung bietet „Rückgängig“ (EP-17, ES-10).
- **Gegeben** die Jahresübersicht auf einem 390 px breiten Bildschirm, **dann** stehen die Werte „Einmalig" unter
  denen des Plans, und die Seite ist nicht breiter als der Bildschirm (EP-18).
- **Gegeben** ein Einmalposten mit Betrag 0,00 € oder Datum 31.12.1899, **dann** wird er abgelehnt (EP-1, EP-3).
- **Gegeben** Benutzer A mit einem Einmalposten, **wenn** Benutzer B ihn über seine ID ändern oder löschen will,
  **dann** antwortet die API mit 404 (EP-4).

## Nicht im Umfang

- Datum für Posten des Haushaltsplans; Uhrzeit für Einmalposten
- Wiederkehrende Einmalposten (etwa „jedes Jahr im März") und Vorlagen
- Kategorie, Unterkategorie, Markierungen für Einmalposten
- Eine Spalte „Gesamt" (Überschuss + einmaliger Saldo) in der Jahresübersicht
- Einmalposten in der Bilanz, der 50/30/20-Auswertung oder dem Tortendiagramm
- Eine Ansicht je Kalendermonat

## Umsetzung

- Domäne: Aggregate `OneTimeItem` in `domain/onetimeitem/` mit `OneTimeItemRepository` und `valueobject/OneTimeItemId`;
  nutzt `OwnerId`, `ItemType`, `BudgetItemDescription`, `BudgetItemNote` und `Money` aus `domain/householdplan/valueobject/`, kennt aber
  keinen Plan (EP-5). `create`/`change` prüfen Betrag > 0 (EP-1) und Datum 01.01.1900 bis 31.12.2200 (EP-3); `change`
  liefert den geänderten Einmalposten, `year()` ist das Jahr des Datums (EP-7). Commands `Create…`, `Change…`,
  `DeleteOneTimeItemCommand`, Events `OneTimeItemCreated`, `…Changed`, `…Deleted` (im Audit-Log ohne Plan),
  `OneTimeItemNotFoundException` (404, `urn:hbt:one-time-item-not-found`, EP-4)
- Lesen: Read Model `OneTimeItemOverview` mit `OneTimeOverviewItem` und Summen (EP-8, EP-9), geladen von
  `JdbcOneTimeItemOverviewQueryHandler` nach Datum und Erfassungszeit. Die Jahresübersicht nimmt die Summen je Jahr
  als `YearlyOverview.OneTimeSummary` dazu; `YearlyOverviewEntry.withoutPlan` für Jahre nur mit Einmalposten (EP-12 bis
  EP-15)
- Persistenz: Tabelle `one_time_item` (`V10__one_time_item.sql`), `JdbcOneTimeItemRepository` mit `FOR UPDATE`
- Application: `OneTimeItemApplicationService`
- API: `GET/POST /api/one-time-items`, `PUT/DELETE /api/one-time-items/{id}` (`OneTimeItemController`); Änderungen
  antworten ohne Inhalt, die Oberfläche lädt die Einmalposten des Jahres neu. `YearlyOverviewEntryResponse` hat
  `oneTimeIncome`, `oneTimeExpense`, `oneTimeBalance`, `oneTimeExpensesExceedIncome`; `planId` und die Beträge des Plans
  sind ohne Plan `null`
- Frontend: `data-access/one-time-item-api.ts`, `data-access/one-time-item-store.ts`; Abschnitt `ui/one-time-items.ts`
  in `budget-dashboard.ts` unter den Einträgen bzw. unter „Für <Jahr> gibt es noch keinen Haushaltsplan." (EP-8 bis
  EP-10); Dialog `ui/one-time-item-dialog.ts` (Datum vorbelegt mit `defaultDateIn`, EP-11); Bereiche, Reihenfolge
  und Löschen mit „Rückgängig“ siehe [einmalposten-sortierung.md](einmalposten-sortierung.md). `yearly-overview-page.ts` mit Spaltengruppen „Haushaltsplan" und
  „Einmalig (Jahr)", „–" und ohne Knöpfe für Jahre ohne Plan, unter 600 px eine eigene Zeile `tr.one-time-row` (EP-12
  bis EP-18); „Haushaltsplan anlegen" und der Umschalter zählen nur Jahre mit Plan
- Tests: `OneTimeItemTest`, `OneTimeItemOverviewTest`, `YearlyOverviewTest`, `OneTimeItemApplicationServiceTest`,
  `OneTimeItemControllerTest`, `HouseholdPlanOverviewControllerTest`, `JdbcOneTimeItemRepositoryTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `OneTimeItemIntegrationTest`; `one-time-item-store.spec.ts`,
  `one-time-items.spec.ts`, `one-time-item-dialog.spec.ts`, `yearly-overview-page.spec.ts`, `year.spec.ts`
- E2E: `e2e/tests/einmalposten.spec.ts` (EP-1 bis EP-3, EP-5 bis EP-15, EP-18), EP-17 in
  `einmalposten-sortierung.spec.ts`; EP-4 und EP-16 decken die Backend-Tests ab

## Offene Fragen

Keine. Entschieden mit JC: einmalige Einnahmen, Ausgaben und Saldo als eigene Spalten ohne „Gesamt" (EP-13); Jahre nur
mit Einmalposten erscheinen in der Jahresübersicht (EP-12); Datum als Tag ohne Uhrzeit (EP-1); optionale Notiz (EP-1);
Löschen zunächst mit Rückfrage (EP-17, inzwischen durch ES-10 ersetzt); Überschrift „Haushaltsplan" über den Spalten
des Plans (EP-13); eine Zeile ohne Plan hat keine Knöpfe für den Plan (EP-12).
