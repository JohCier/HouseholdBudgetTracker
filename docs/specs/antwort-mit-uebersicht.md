# Übersicht als Antwort auf Änderungen

Status: geplant

## Zweck

Nach jeder Änderung an einem Plan (Plan anlegen, Posten erfassen, ändern oder löschen) lädt die Oberfläche heute die
komplette Übersicht mit einer zweiten Anfrage neu. Jeder Klick kostet so zwei Anfragen und die doppelte Wartezeit.
Mit dieser Spec liefert die API die Übersicht nach der Änderung direkt in ihrer Antwort mit, und die Oberfläche zeigt
sie ohne weitere Anfrage an. Für den Benutzer ändert sich nur, dass die Anzeige schneller aktualisiert wird.

## Fachliche Regeln

- **AU-1**: Jede erfolgreiche Änderung an einem Plan beantwortet die API mit der Übersicht dieses Plans nach der
  Änderung. Die Übersicht hat dieselbe Form wie bei `GET /api/plans?year=` (Posten, Summen, Aufteilung je Kategorie).
  Das gilt für: Plan anlegen (HP-5), Einnahme oder Ausgabe erfassen (PO-1), Posten ändern (PB-1) und Posten löschen
  (PO-6).
- **AU-2**: Die mitgelieferte Übersicht enthält genau den Stand nach dieser Änderung, einschließlich der Änderung
  selbst. Sie ist mit dem identisch, was ein anschließender Abruf der Übersicht liefern würde, solange niemand sonst
  den Plan ändert.
- **AU-3**: Antworten auf das Erfassen eines Postens und das Anlegen eines Plans nennen weiterhin die ID des neuen
  Postens bzw. Plans, neben der Übersicht.
- **AU-4**: Statuscodes: Plan anlegen und Posten erfassen antworten weiter mit 201, Posten ändern und löschen mit 200
  statt bisher 204, weil die Antwort jetzt einen Inhalt hat. Fehlerantworten (400, 404, 409) bleiben unverändert.
- **AU-5**: Die Oberfläche zeigt nach einer Änderung die mitgelieferte Übersicht an und fragt sie nicht erneut ab.
  Gehört die Antwort zu einem anderen Jahr als dem gerade gewählten (z. B. Rückgängig nach einem Jahreswechsel, PO-9),
  zeigt sie die Übersicht nicht an; das gewählte Jahr bleibt wie es ist (HP-1).
- **AU-6**: Wird gerade die Übersicht des gewählten Jahres geladen, während eine Änderung antwortet, gewinnt die
  jüngere Antwort, damit keine veralteten Daten stehen bleiben.

## Akzeptanzkriterien

- **Gegeben** ein angemeldeter Benutzer ohne Plan für 2026, **wenn** er einen Plan für 2026 anlegt, **dann** antwortet
  die API mit 201, der ID des Plans und der leeren Übersicht für 2026 (AU-1, AU-3).
- **Gegeben** ein Plan, **wenn** der Benutzer die Ausgabe „Miete", 850,00 €, Lebensunterhalt erfasst, **dann**
  antwortet die API mit 201, der ID des Postens und einer Übersicht, die „Miete" enthält und Ausgaben von 850,00 € zeigt
  (AU-1, AU-2, AU-3).
- **Gegeben** ein Posten, **wenn** er geändert wird, **dann** antwortet die API mit 200 und der Übersicht mit den neuen
  Werten (AU-1, AU-4).
- **Gegeben** ein Posten, **wenn** er gelöscht wird, **dann** antwortet die API mit 200 und der Übersicht ohne diesen
  Posten (AU-1, AU-4).
- **Gegeben** die Oberfläche zeigt einen Plan, **wenn** der Benutzer einen Posten erfasst, ändert oder löscht, **dann**
  zeigt sie die neue Übersicht an, ohne `GET /api/plans` erneut aufzurufen (AU-5).
- **Gegeben** ein gelöschter Posten aus 2026 und die Oberfläche zeigt inzwischen 2027, **wenn** der Benutzer
  „Rückgängig" wählt, **dann** bleibt 2027 angezeigt, und der Posten ist in 2026 wieder vorhanden (AU-5, PO-9).

## Nicht im Umfang

- Optimistische Anzeige vor der Antwort des Servers
- Änderungen anderer Sitzungen oder Tabs live übernehmen (der Stand kommt weiter erst mit der nächsten eigenen Änderung
  oder dem nächsten Abruf)
- Änderungen an `GET /api/plans?year=` selbst

## Umsetzung

Noch nicht umgesetzt. Bei der Umsetzung werden im selben PR auch die Akzeptanzkriterien in
[haushaltsplan.md](haushaltsplan.md), [posten.md](posten.md) und [posten-bearbeiten.md](posten-bearbeiten.md)
angepasst, die heute 204 bzw. 201 nur mit ID nennen, außerdem API-Vertrag und Frontend.

## Offene Fragen

1. **Ändern und Löschen mit 200 statt 204 (AU-4)?** Vorschlag: ja, weil die Antwort jetzt die Übersicht enthält.
   Alternative: 204 behalten und die Oberfläche lädt nach Ändern/Löschen weiter neu, dann gilt die Spec nur fürs
   Erfassen und Anlegen.
2. **Form der Antwort beim Erfassen und Anlegen (AU-3)?** Vorschlag: `{ "id": …, "overview": { … } }`, damit API-Clients
   die ID wie bisher unter `id` finden. Alternative: nur die Übersicht, die ID steht dort ja in der Postenliste bzw. als
   `id` des Plans.
