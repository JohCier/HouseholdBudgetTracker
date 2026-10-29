# Vermögen und Schulden

Status: umgesetzt

## Zweck

Der Haushaltsplan zeigt, was im Jahr hereinkommt und hinausgeht. Wo der Benutzer finanziell steht, zeigt er nicht:
Wie viel liegt auf Konten und im Depot, wie viel ist noch an Krediten offen, und ist das über die Jahre mehr oder
weniger geworden?

Mit dieser Spec erfasst der Benutzer je Jahr einfache **Vermögenswerte** (z. B. „Girokonto 3.200,00 €") und
**Schulden** (z. B. „Autokredit 7.800,00 €"). Die Differenz ist das **Nettovermögen**. Die Jahresübersicht zeigt in
einer eigenen Karte das Nettovermögen aller Jahre mit Wachstum oder Verlust zum Vorjahr und einem Verlaufsdiagramm.
Vermögen und Schulden haben keine Verbindung zum Haushaltsplan.

Vermögen und Schulden sind **Bestände**, keine Zahlungen. Die Regel „Maßgebliche Kenngröße ist der Monat" aus
[produkt.md](../produkt.md) gilt für sie nicht: Es gibt keinen Monatsbetrag und kein Zwölffaches.

Diese Spec ändert die Startseite [jahresuebersicht.md](jahresuebersicht.md) (neue Karte unter der Tabelle der Pläne)
und das Nicht-Ziel „Diagramme über mehrere Jahre" dort, aber nur für das Nettovermögen. JU-1 bis JU-13 und die Tabelle
der Pläne bleiben unverändert.

## Fachliche Regeln

### Werte

- **VS-1**: Ein Wert ist ein Vermögenswert oder eine Schuld mit Bezeichnung (1 bis 255 Zeichen, nicht nur
  Leerzeichen, wie die Beschreibung eines Postens) und Betrag (größer als 0,00 €, höchstens zwei Nachkommastellen).
  Auch Schulden werden mit positivem Betrag erfasst; „Autokredit 7.800,00 €" verringert das Nettovermögen um
  7.800,00 €.
- **VS-2**: Jeder Wert gehört zu genau einem Jahr von 1900 bis 2200 (wie HP-1). Er steht für den Stand am Ende dieses
  Jahres; für das laufende Jahr ist das der aktuelle Stand.
- **VS-3**: Ein Wert gehört dem Benutzer, der ihn erfasst hat. Nur dieser sieht, ändert und löscht ihn; fremde Werte
  werden behandelt, als gäbe es sie nicht (wie HP-4).
- **VS-4**: Werte sind unabhängig vom Haushaltsplan. Der Benutzer kann sie für jedes Jahr erfassen, auch ohne Plan.
  Plan löschen ([plan-loeschen.md](plan-loeschen.md)) und Plan übernehmen ([plan-uebernehmen.md](plan-uebernehmen.md))
  berühren keine Werte; Werte fließen in keine Summe des Plans ein.
- **VS-5**: Der Benutzer kann einen Wert erfassen, bearbeiten (Art, Bezeichnung, Betrag) und löschen. Vor dem Löschen
  fragt die Oberfläche nach („Wert „Girokonto" löschen?" mit „Löschen" und „Abbrechen").

### Summen eines Jahres

- **VS-6**: Für jedes Jahr gibt es drei Summen: **Vermögen** (Summe der Vermögenswerte), **Schulden** (Summe der
  Schulden) und **Nettovermögen** (Vermögen − Schulden). Das Nettovermögen darf negativ sein und wird dann in der
  Fehlerfarbe mit dem Hinweis „Überschuldet" angezeigt.
- **VS-7**: Ein Jahr hat einen **Vermögensstand**, sobald es mindestens einen Wert hat. Löscht der Benutzer den letzten
  Wert, hat das Jahr keinen Vermögensstand mehr.

### Seite „Vermögen"

- **VS-8**: Die Seite `/vermoegen/<Jahr>` mit der Überschrift „Vermögen <Jahr>" zeigt zwei Listen, „Vermögen" und
  „Schulden", je mit allen Werten des Jahres in der Reihenfolge des Erfassens und ihrer Summe darunter. Darüber steht
  ein Kasten mit Vermögen, Schulden und Nettovermögen (VS-6) und, wenn es einen Vergleich gibt, der Veränderung zum
  Vorjahr (VS-12).
- **VS-9**: Mit den Pfeilen wechselt der Benutzer zum vorherigen bzw. nächsten Jahr (wie in der Planansicht);
  „Alle Jahre" führt zur Jahresübersicht. Eine Adresse mit einem Jahr außerhalb von 1900 bis 2200 führt zur
  Jahresübersicht.
- **VS-10**: Ein Jahr ohne Vermögensstand zeigt „Für <Jahr> ist noch kein Vermögen erfasst." mit „Wert erfassen" und,
  wenn der Benutzer in einem anderen Jahr einen Vermögensstand hat, „Aus anderem Jahr übernehmen" (VS-15).
- **VS-11**: Beim Erfassen wählt der Benutzer Vermögen oder Schuld, Bezeichnung und Betrag. Die Liste, aus der er
  „Wert erfassen" startet, ist als Art vorbelegt.

### Veränderung zum Vorjahr

- **VS-12**: Verglichen wird das Nettovermögen mit dem des unmittelbar vorhergehenden Jahres, wenn dieses einen
  Vermögensstand hat (wie JU-4: 2026 wird nicht mit 2024 verglichen, wenn 2025 fehlt). Angezeigt werden die
  Veränderung in Euro und in Prozent, z. B. „+3.400,00 €" und „+32,38 %".
- **VS-13**: Die Prozente rechnen sich wie JU-5: (Nettovermögen des Jahres − des Vorjahres) ÷ |des Vorjahres| × 100,
  kaufmännisch auf zwei Nachkommastellen. Ist das Nettovermögen des Vorjahres 0,00 €, steht bei den Prozenten „–", die
  Veränderung in Euro bleibt. Vorzeichen und Farbe wie JU-7 und JU-8: Wachstum grün, Verlust rot, keine Veränderung
  neutral.
- **VS-14**: Alle Summen und Veränderungen rechnet das Backend (wie JU-9).

### In das Folgejahr übernehmen

- **VS-15**: Der Benutzer kann den Vermögensstand eines Jahres (Quelle) mit allen Werten in ein anderes Jahr
  (Zieljahr) übernehmen, um dort nur die Beträge anzupassen. Das Verhalten folgt [plan-uebernehmen.md](plan-uebernehmen.md):
  Zieljahr frei wählbar, vorgeschlagen das Folgejahr (PU-2); übernommen werden Art, Bezeichnung, Betrag und
  Reihenfolge (PU-3, PU-4); die Quelle bleibt unverändert (PU-5); ganz oder gar nicht (PU-7).
- **VS-16**: Hat das Zieljahr schon einen Vermögensstand, fragt der Dialog vorher nach: „Für 2027 ist bereits Vermögen
  mit 4 Werten erfasst. Es wird überschrieben, die Werte lassen sich nicht wiederherstellen." mit „Überschreiben"
  statt „Übernehmen" (wie PU-14). Ohne Bestätigung ändert sich nichts; die API antwortet dann mit 409.
- **VS-17**: „Vermögen übernehmen" gibt es auf der Seite „Vermögen" eines Jahres mit Vermögensstand und in jeder Zeile
  der Karte „Vermögen" der Jahresübersicht; „Aus anderem Jahr übernehmen" nach VS-10. Danach ist die Seite „Vermögen"
  des Zieljahres geöffnet mit der Meldung „Vermögen 2026 nach 2027 übernommen.".

### Karte „Vermögen" in der Jahresübersicht

- **VS-18**: Unter der Tabelle der Haushaltspläne steht die Karte „Vermögen". Sie enthält das Verlaufsdiagramm (VS-20)
  und darunter eine Tabelle mit allen Jahren mit Vermögensstand, das neueste oben, und den Spalten Jahr, Vermögen,
  Schulden, Nettovermögen und Veränderung (VS-12). Ein Klick auf eine Zeile öffnet `/vermoegen/<Jahr>`.
- **VS-19**: Die Karte zeigt immer Bestände; der Schalter „Monatlich"/„Jährlich" der Jahresübersicht (JM-1) wirkt auf
  sie nicht. Ohne Vermögensstand zeigt die Karte „Noch kein Vermögen erfasst." und „Vermögen erfassen", das
  `/vermoegen/<aktuelles Jahr>` öffnet.

### Verlaufsdiagramm

- **VS-20**: Das Diagramm zeigt die Jahre vom ältesten bis zum neuesten Vermögensstand von links nach rechts, jedes
  Kalenderjahr dazwischen mit eigenem Platz. Es enthält
  - eine Linie des Nettovermögens mit einem Punkt je Jahr mit Vermögensstand,
  - je Jahr mit Veränderung (VS-12) einen Balken in Höhe der Veränderung in Euro, nach oben grün für Wachstum, nach
    unten rot für Verlust,
  - eine waagerechte Nulllinie, sobald ein Wert negativ ist, und eine Achse mit Eurobeträgen.
- **VS-21**: Fehlt für ein Jahr der Vermögensstand, ist die Linie dort unterbrochen, und das Jahr hat keinen Punkt und
  keinen Balken; die Jahreszahl steht trotzdem an der Achse.
- **VS-22**: Zeigt der Benutzer auf einen Punkt oder Balken (Maus oder Tippen), erscheinen Jahr, Nettovermögen und
  Veränderung, z. B. „2026: 13.900,00 € · +3.400,00 € (+32,38 %)". Die Farbe ist nie die einzige Information (wie
  JU-8): Die Tabelle unter dem Diagramm enthält alle Zahlen als Text.
- **VS-23**: Mit nur einem Vermögensstand zeigt das Diagramm einen Punkt und den Hinweis „Ab zwei Jahren zeigt das
  Diagramm den Verlauf.".
- **VS-24**: Auf schmalen Bildschirmen ([mobile-ansicht.md](mobile-ansicht.md)) passt das Diagramm in die Breite,
  zeigt höchstens die letzten acht Jahre (ältere über waagerechtes Wischen im Diagramm, nicht der Seite), und die
  Tabelle zeigt je Jahr Nettovermögen und Veränderung in der ersten, Vermögen und Schulden klein in der zweiten Zeile.

## Akzeptanzkriterien

Beispielbestand für die Kriterien:

| Jahr | Vermögen | Schulden | Nettovermögen | Veränderung |
|---|---|---|---|---|
| 2021 | 5.000,00 € | 3.000,00 € | 2.000,00 € | – |
| 2022 | 7.000,00 € | 2.500,00 € | 4.500,00 € | +2.500,00 € (+125,00 %) |
| 2023 | 9.000,00 € | 3.000,00 € | 6.000,00 € | +1.500,00 € (+33,33 %) |
| 2024 | 15.200,00 € | 11.400,00 € | 3.800,00 € | −2.200,00 € (−36,67 %) |
| 2025 | 20.100,00 € | 9.600,00 € | 10.500,00 € | +6.700,00 € (+176,32 %) |
| 2026 | 21.700,00 € | 7.800,00 € | 13.900,00 € | +3.400,00 € (+32,38 %) |

2026 besteht aus „Girokonto" 3.200,00 €, „Tagesgeld" 6.000,00 €, „ETF-Depot" 12.500,00 € (Vermögen) sowie
„Autokredit" 6.300,00 € und „Ratenkauf Küche" 1.500,00 € (Schulden).

- **Gegeben** kein Vermögensstand 2026, **wenn** der Benutzer auf `/vermoegen/2026` die fünf Werte des Beispielbestands
  erfasst, **dann** zeigt der Kasten
  Vermögen 21.700,00 €, Schulden 7.800,00 € und Nettovermögen 13.900,00 € (VS-1, VS-6, VS-8).
- **Gegeben** der Beispielbestand, **dann** zeigt die Karte „Vermögen" der Jahresübersicht die Zeilen 2026 bis 2021
  mit den Werten der Tabelle; 2024 rot, 2022, 2023, 2025 und 2026 grün, 2021 ohne Veränderung (VS-12, VS-13, VS-18).
- **Gegeben** der Beispielbestand, **dann** zeigt das Diagramm sechs Punkte auf einer durchgehenden Linie, einen roten
  Balken nach unten bei 2024 und grüne Balken nach oben bei 2022, 2023, 2025 und 2026 (VS-20).
- **Gegeben** der Beispielbestand ohne 2024, **dann** ist die Linie zwischen 2023 und 2025 unterbrochen, 2024 steht
  ohne Punkt an der Achse, und 2025 hat keine Veränderung und keinen Balken (VS-12, VS-21).
- **Gegeben** die Jahresübersicht mit „Monatlich", **dann** zeigt die Karte „Vermögen" weiter 13.900,00 € für 2026
  (VS-19).
- **Gegeben** Vermögen 2.000,00 € und Schulden 5.000,00 € in 2026, **dann** steht das Nettovermögen −3.000,00 € in
  der Fehlerfarbe mit „Überschuldet" (VS-6).
- **Gegeben** 2025 mit Nettovermögen −1.000,00 € und 2026 mit −500,00 €, **dann** zeigt 2026 +500,00 € (+50,00 %) in
  Grün (VS-13).
- **Gegeben** 2025 mit Nettovermögen 0,00 € und 2026 mit 1.000,00 €, **dann** zeigt 2026 +1.000,00 € und „–" bei den
  Prozenten (VS-13).
- **Gegeben** der Vermögensstand 2026 und keiner für 2027, **wenn** der Benutzer „Vermögen übernehmen" mit dem
  vorgeschlagenen Zieljahr 2027 wählt, **dann** ist `/vermoegen/2027` mit denselben fünf Werten geöffnet, die Meldung
  „Vermögen 2026 nach 2027 übernommen." erscheint, und das Ändern eines Betrags in 2027 lässt 2026 unverändert (VS-15,
  VS-17).
- **Gegeben** Vermögensstände 2026 und 2027 (2027 mit 4 Werten), **wenn** der Benutzer 2026 nach 2027 übernehmen will,
  **dann** zeigt der Dialog den Hinweis nach VS-16 und „Überschreiben"; nach „Abbrechen" ist 2027 unverändert (VS-16).
- **Gegeben** ein Plan 2026 und ein Vermögensstand 2026, **wenn** der Benutzer den Plan löscht, **dann** bleibt der
  Vermögensstand 2026 erhalten (VS-4).
- **Gegeben** nur ein Vermögensstand 2026, **dann** zeigt das Diagramm einen Punkt und „Ab zwei Jahren zeigt das
  Diagramm den Verlauf." (VS-23).
- **Gegeben** ein neuer Benutzer, **dann** zeigt die Karte „Vermögen" „Noch kein Vermögen erfasst."; „Vermögen
  erfassen" öffnet `/vermoegen/<aktuelles Jahr>` mit „Für <Jahr> ist noch kein Vermögen erfasst." (VS-10, VS-19).
- **Gegeben** der Wert „Girokonto", **wenn** der Benutzer ihn löschen will und „Abbrechen" wählt, **dann** bleibt er;
  nach „Löschen" sind die Summen angepasst (VS-5).
- **Gegeben** die Jahresübersicht auf einem 390 px breiten Bildschirm, **dann** ist die Seite nicht breiter als der
  Bildschirm, und das Diagramm passt in die Karte (VS-24).
- **Gegeben** ein Wert mit Betrag 0,00 € oder für das Jahr 2201, **dann** wird er abgelehnt (VS-1, VS-2).
- **Gegeben** Benutzer A mit einem Wert, **wenn** Benutzer B ihn über seine ID ändern oder löschen will, **dann**
  antwortet die API mit 404 (VS-3).

## Nicht im Umfang

- Gruppen oder Arten von Werten (Konto, Depot, Immobilie, Kredit) und Diagramme je Gruppe
- Stände unterjährig oder je Monat, Stichtage außer dem Jahresende
- Automatische Fortschreibung aus dem Haushaltsplan (etwa Überschuss → Vermögen) oder Zinsen und Tilgungspläne
- Ein Verweis zwischen Planansicht und Seite „Vermögen"
- Vermögen in der Planansicht, der Bilanz oder der 50/30/20-Auswertung
- Rückgängig machen von Löschen oder Überschreiben

## Umsetzung

- Domäne: Aggregate `NetWorthStatement` (Vermögensstand) in `domain/networth/` (Value Objects in `valueobject/`) je Besitzer und Jahr, mit Entities
  `NetWorthEntry` aus `NetWorthEntryType` (`ASSET`, `LIABILITY`), `NetWorthEntryName` und `Money` (VS-1, VS-2);
  `addEntry`, `changeEntry`, `removeEntry` und `isEmpty` (VS-5, VS-7); `createFrom`, `overwriteWith` und
  `requireCopyableTo` (VS-15, VS-16); Events `NetWorthStatementCreated`, `NetWorthEntryAdded`, `NetWorthEntryChanged`,
  `NetWorthEntryRemoved`, `NetWorthStatementCopied`. Kein Verweis auf den Haushaltsplan (VS-4)
- Lesen: Read Models `NetWorthOverview` (Seite eines Jahres) und `NetWorthHistory` mit `NetWorthHistoryEntry` (Karte
  und Diagramm), beide aus `NetWorthTotals` (VS-6) und `NetWorthChange` über `Percentage.changeFrom` (VS-12, VS-13);
  `NetWorthQueryHandler`, umgesetzt in `JdbcNetWorthQueryHandler`
- Ablauf: `NetWorthApplicationService` legt den Vermögensstand mit dem ersten Wert an und entfernt ihn mit dem letzten
  (VS-7); `copyStatement` legt an oder überschreibt nur mit `overwrite`, sonst
  `NetWorthStatementAlreadyExistsException` (409)
- Persistenz: Migration `V12__net_worth.sql` (`net_worth_statement`, `net_worth_entry`), `JdbcNetWorthStatementRepository`
- API: `NetWorthController` unter `/api/net-worth`: `GET /years`, `GET /{year}`, `POST /{year}/entries`,
  `PUT`/`DELETE /{year}/entries/{entryId}`, `POST /{year}/copies` (201 neu, 200 überschrieben, 409); Fehler
  `urn:hbt:net-worth-not-found`, `urn:hbt:net-worth-entry-not-found`, `urn:hbt:net-worth-already-exists`
- Frontend: Feature `net-worth/` mit `net-worth-page.ts` unter `/vermoegen/:year` und `data-access/net-worth-store.ts`
  (VS-8 bis VS-11), Dialoge `ui/net-worth-entry-dialog.ts`, `ui/remove-net-worth-entry-dialog.ts`,
  `ui/copy-net-worth-dialog.ts` (VS-5, VS-15 bis VS-17), Karte `ui/net-worth-card.ts` in `yearly-overview-page.ts`
  (VS-18, VS-19) mit Diagramm `ui/net-worth-chart.ts` als eigenes SVG (VS-20 bis VS-24); Anzeige der Veränderung in
  `net-worth-change.ts`
- Tests: `NetWorthStatementTest`, `NetWorthHistoryTest`, `NetWorthControllerTest`, `NetWorthIntegrationTest`,
  `net-worth-change.spec.ts`, `net-worth-chart.spec.ts`, `net-worth-card.spec.ts`, `net-worth-store.spec.ts`,
  `net-worth-api.spec.ts`
- E2E: `e2e/tests/vermoegen.spec.ts`

## Abstimmung mit „Einmalposten"

Der Entwurf [einmalposten.md](einmalposten.md) erweitert die Tabelle der Pläne in der Jahresübersicht (EP-12 bis
EP-18). Diese Spec fasst die Tabelle nicht an, sondern ergänzt eine eigene Karte darunter. Beide ändern
`yearly-overview-page.ts`; wer zuerst gemergt wird, gibt die Struktur vor, der andere PR passt sich an.

## Offene Fragen

Keine. Entschieden mit JC am 25.10.2026: Werte nur mit Bezeichnung und Betrag (VS-1), Stand am Jahresende (VS-2),
eigene Seite je Jahr (VS-8), Übernahme mit Überschreiben nach Rückfrage (VS-15, VS-16), eigene Karte in der
Jahresübersicht (VS-18), Linie des Nettovermögens mit Balken für Wachstum und Verlust (VS-20).
