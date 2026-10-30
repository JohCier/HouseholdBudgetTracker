# Einmalposten getrennt und frei sortiert

Status: umgesetzt

## Zweck

Im Tab „Einmalposten“ ([planansicht-aufbau.md](planansicht-aufbau.md), AP-8) stehen einmalige Einnahmen und Ausgaben
heute gemischt in einer Liste, fest nach Datum sortiert (EP-8). Gelöscht wird nach einer Rückfrage (EP-17). Die
regelmäßigen Posten verhalten sich anders: Einnahmen und Ausgaben stehen getrennt, die Reihenfolge legt der Benutzer
per Ziehen fest (UK-14), und Löschen geht sofort mit „Rückgängig“ (PO-9).

Diese Spec gleicht die Einmalposten daran an: Einnahmen stehen oben, Ausgaben darunter, jeweils in einem eigenen
Bereich mit Summe. Innerhalb eines Bereichs sortiert der Benutzer die Einmalposten per Ziehen, und Löschen geht wie bei
Posten mit „Rückgängig“.

Diese Spec ändert EP-8, EP-9 und EP-17 in [einmalposten.md](einmalposten.md).

## Fachliche Regeln

### Einnahmen und Ausgaben getrennt

- **ES-1**: Der Abschnitt „Einmalposten“ hat zwei Bereiche: oben „Einnahmen“ mit den einmaligen Einnahmen, darunter
  „Ausgaben“ mit den einmaligen Ausgaben. Jeder Bereich ist farblich abgesetzt wie in den Einträgen (Einnahmen grün,
  Ausgaben rot) und endet mit der Zeile „Summe Einnahmen“ bzw. „Summe Ausgaben“. Unter beiden Bereichen steht der
  einmalige Saldo (EP-9), negativ in der Fehlerfarbe. Ändert EP-8 und EP-9.
- **ES-2**: Je Zeile stehen Datum, Beschreibung mit Notiz darunter und Betrag (EP-8). Die Spalte „Art“ entfällt, weil
  der Bereich sie zeigt.
- **ES-3**: Hat das Jahr keine Einmalposten, zeigt der Abschnitt wie bisher „Keine Einmalposten.“ ohne Bereiche
  (EP-10). Fehlt nur eine Art, zeigt ihr Bereich „Keine einmaligen Einnahmen.“ bzw. „Keine einmaligen Ausgaben.“ und
  die Summe 0,00 €.

### Reihenfolge

- **ES-4**: Der Benutzer kann die Einmalposten innerhalb eines Bereichs frei umsortieren, per Ziehen und Ablegen an
  einem Griff oder mit den Pfeiltasten auf dem Griff, wie Posten (UK-14). Einen Griff gibt es nur, wenn der Bereich
  mindestens zwei Einmalposten hat. Ein Einmalposten wechselt durch Verschieben nicht den Bereich; dafür ändert der
  Benutzer seine Art.
- **ES-5**: Die Reihenfolge wird gespeichert und gilt je Benutzer und Jahr: nach dem Neuladen, nach erneuter Anmeldung
  und auf anderen Geräten (wie UK-15).
- **ES-6**: Bereits erfasste Einmalposten stehen zunächst wie bisher nach Datum, bei gleichem Datum in der Reihenfolge
  des Erfassens. Danach bestimmen Verschieben (ES-4), Erfassen (ES-7) und Ändern (ES-8) die Reihenfolge.
- **ES-7**: Ein neu erfasster Einmalposten steht unten in seinem Bereich (wie EG-1).
- **ES-8**: Ändert der Benutzer Beschreibung, Betrag, Notiz oder das Datum innerhalb desselben Jahres, behält der
  Einmalposten seinen Platz. Ändert er die Art, steht der Einmalposten unten im anderen Bereich (wie EG-2); ändert er
  das Datum in ein anderes Jahr, steht er dort unten in seinem Bereich.
- **ES-9**: Schlägt das Speichern der Reihenfolge fehl, springt die Liste auf die gespeicherte Reihenfolge zurück und
  eine Meldung nennt den Fehler (wie beim Umsortieren der Posten).

### Löschen

- **ES-10**: „Löschen“ entfernt den Einmalposten sofort, ohne Rückfrage. Die Meldung „„‹Beschreibung›“ gelöscht“ bietet
  „Rückgängig“ (wie PO-9). Ersetzt die Rückfrage aus EP-17.
- **ES-11**: „Rückgängig“ legt den Einmalposten mit allen Angaben (Art, Beschreibung, Betrag, Datum, Notiz) wieder an;
  er steht wie ein neuer unten in seinem Bereich (wie EG-3).

## Akzeptanzkriterien

- **Gegeben** die Einmalposten „Nebenkostennachzahlung“ (Ausgabe, 1.200,00 €, 15.03.), „Steuererstattung“ (Einnahme,
  800,00 €, 10.07.) und „Autoreparatur“ (Ausgabe, 650,00 €, 22.09.), **wenn** der Benutzer den Tab „Einmalposten“
  öffnet, **dann** steht oben „Einnahmen“ mit „Steuererstattung“ und „Summe Einnahmen“ +800,00 €, darunter „Ausgaben“
  mit „Nebenkostennachzahlung“, „Autoreparatur“ und „Summe Ausgaben“ −1.850,00 €, darunter „Saldo“ −1.050,00 € in der
  Fehlerfarbe (ES-1, ES-2, ES-6).
- **Gegeben** nur die Einnahme „Bonus“, **dann** zeigt der Bereich „Ausgaben“ „Keine einmaligen Ausgaben.“ und
  „Summe Ausgaben“ 0,00 € (ES-3).
- **Gegeben** die Ausgaben „Nebenkostennachzahlung“ und „Autoreparatur“, **wenn** der Benutzer „Autoreparatur“ am Griff
  nach oben zieht, **dann** steht sie oben, auch nach dem Neuladen (ES-4, ES-5).
- **Gegeben** derselbe Plan, **wenn** der Benutzer den Griff von „Autoreparatur“ fokussiert und die Pfeiltaste nach
  oben drückt, **dann** steht sie oben (ES-4).
- **Gegeben** nur eine einmalige Einnahme, **dann** hat sie keinen Griff (ES-4).
- **Gegeben** „Autoreparatur“ oben vor „Nebenkostennachzahlung“, **wenn** der Benutzer „Kaution“ (Ausgabe, 01.01.)
  erfasst, **dann** steht „Kaution“ unten bei den Ausgaben (ES-7).
- **Gegeben** „Autoreparatur“ oben, **wenn** der Benutzer ihren Betrag und ihr Datum im selben Jahr ändert, **dann**
  bleibt sie oben; **wenn** er ihre Art auf „Einnahme“ ändert, **dann** steht sie unten bei den Einnahmen (ES-8).
- **Gegeben** die Ausgabe „Autoreparatur“, **wenn** der Benutzer „Löschen“ wählt, **dann** ist sie sofort weg, und die
  Meldung „„Autoreparatur“ gelöscht“ bietet „Rückgängig“; nach „Rückgängig“ steht sie mit Datum und Notiz wieder
  unten bei den Ausgaben (ES-10, ES-11).
- **Gegeben** die Einmalposten eines anderen Benutzers, **wenn** jemand ihre Reihenfolge ändern will, **dann**
  antwortet die API mit 404 (EP-4).

## Nicht im Umfang

- Einmalposten zwischen Einnahmen und Ausgaben verschieben (dafür die Art ändern)
- Einmalposten nach Betrag oder Beschreibung per Knopfdruck sortieren
- Unterkategorien, Gruppen oder Markierungen für Einmalposten (Nicht-Ziel in EP)
- Die Spalten „Einmalig“ der Jahresübersicht

## Umsetzung

- Domäne: Value Object `OneTimeItemRank` (Platz im Bereich; `last` für einen neuen oder in einen anderen Bereich
  gewechselten Einmalposten, `between` für einen verschobenen) als Feld von `OneTimeItem` (ES-4, ES-7, ES-8).
  `OneTimeItem.moveBetween(previous, next)` prüft, dass die Nachbarn zu Art und Jahr passen, und zeichnet
  `OneTimeItemMoved` auf; `change` behält den Rang im selben Bereich. Verschieben ändert nur den verschobenen
  Einmalposten, ein Aggregate je Command (`MoveOneTimeItemCommand`)
- Persistenz: Migration `V14__one_time_item_rank.sql` mit Spalte `item_rank` (NUMERIC), vorhandene Einmalposten je
  Benutzer, Jahr und Art nach Datum und Erfassen durchnummeriert (ES-6); `JdbcOneTimeItemOverviewQueryHandler`
  sortiert nach `item_rank`
- API: `PUT /api/one-time-items/{id}/position` mit `previousItemId` und/oder `nextItemId` (`MoveOneTimeItemRequest`):
  204, 400 ohne Nachbarn oder mit Nachbar anderer Art bzw. anderen Jahres, 404 für fremde Einmalposten (EP-4)
- Frontend: `ui/one-time-items.ts` mit den Bereichen `areas` (ES-1 bis ES-3), `cdkDropList` je Bereich, Griff mit
  Pfeiltasten und Ausgabe `move` (ES-4); `OneTimeItemStore.moveItem` zeigt die neue Reihenfolge sofort und lädt bei
  einem Fehler neu (ES-9), `restoreItem` legt einen gelöschten Einmalposten wieder an (ES-11);
  `budget-dashboard.ts` löscht sofort mit „Rückgängig“ (ES-10), `ui/delete-one-time-item-dialog.ts` entfällt
- Tests: `OneTimeItemTest`, `OneTimeItemApplicationServiceTest`, `JdbcOneTimeItemRepositoryTest`,
  `OneTimeItemControllerTest`, `OneTimeItemIntegrationTest`; `one-time-items.spec.ts`, `one-time-item-store.spec.ts`
- E2E: `e2e/tests/einmalposten-sortierung.spec.ts` (ES-1 bis ES-8, ES-10, ES-11); ES-9 im
  `one-time-item-store.spec.ts`, der 404-Fall in `OneTimeItemIntegrationTest`

## Offene Fragen

Keine. Entschieden mit JC: dasselbe Ziehen und Ablegen wie bei den Posten (ES-4), Einnahmen oben und Ausgaben unten in
eigenen Bereichen (ES-1), ein neuer Einmalposten steht unten (ES-7), ein geändertes Datum behält den Platz (ES-8),
Löschen wie bei Posten mit „Rückgängig“ (ES-10).
