# Notiz zum Posten

Status: geplant

## Zweck

Die Beschreibung eines Postens sagt, wofür er steht („Miete"), aber nicht, was dahintersteckt: wie sich ein Betrag
zusammensetzt, wann ein Vertrag ausläuft oder warum ein Betrag geschätzt ist. Mit dieser Spec kann der Besitzer eines
Plans zu jedem Posten eine optionale Notiz festhalten und später wiederfinden.

## Fachliche Regeln

- **PN-1**: Jeder Posten, Einnahme wie Ausgabe, kann eine Notiz haben. Die Notiz ist optional.
- **PN-2**: Eine Notiz ist Freitext mit höchstens 1000 Zeichen; Zeilenumbrüche sind erlaubt. Eine längere Notiz wird
  abgelehnt, nicht gekürzt.
- **PN-3**: Eine Notiz, die leer ist oder nur aus Leerzeichen besteht, gilt als „keine Notiz". Führende und
  abschließende Leerzeichen werden entfernt.
- **PN-4**: Die Notiz wird beim Erfassen eines Postens angegeben und beim Bearbeiten geändert oder entfernt (siehe
  [PB-1](posten-bearbeiten.md)). Wie die übrigen Werte eines Postens ändert sie nur der Besitzer des Plans (PO-6).
- **PN-5**: Eine neue, geänderte oder entfernte Notiz ist eine Änderung des Postens im Sinne von PB-5 und wird als
  `BudgetItemChanged` protokolliert. Der Text der Notiz steht nicht im Protokoll.
- **PN-6**: Die Notiz hat keinen Einfluss auf die Auswertung nach der 50/30/20-Regel.
- **PN-7**: Die Tabelle der Posten zeigt bei Posten mit Notiz die Notiz unter der Beschreibung an.
- **PN-8**: Wird das Löschen eines Postens rückgängig gemacht (PO-9), wird auch seine Notiz wiederhergestellt.

## Akzeptanzkriterien

- **Gegeben** ein Plan, **wenn** der Benutzer eine Ausgabe „Miete", 850,00 €, Lebensunterhalt mit der Notiz
  „Warmmiete inkl. Nebenkosten" erfasst, **dann** zeigt die Tabelle die Notiz unter „Miete" an.
- **Gegeben** ein Plan, **wenn** ein Posten ohne Notiz erfasst wird, **dann** antwortet die API mit 201, und die Tabelle
  zeigt beim Posten keine Notiz.
- **Gegeben** ein Posten, **wenn** der Benutzer „Bearbeiten" wählt, **dann** zeigt der Dialog die bisherige Notiz; nach
  dem Ändern und „Speichern" zeigt die Tabelle die neue Notiz, nach dem Leeren des Feldes keine Notiz mehr.
- **Gegeben** ein Posten, **wenn** eine Notiz mit mehr als 1000 Zeichen erfasst oder gespeichert werden soll, **dann**
  antwortet die API mit 400, und die Oberfläche zeigt „Höchstens 1000 Zeichen.", ohne die Anfrage zu senden.
- **Gegeben** ein Posten, **wenn** nur die Notiz geändert wird, **dann** wird ein Event `BudgetItemChanged`
  protokolliert, ohne den Text der Notiz.
- **Gegeben** ein gelöschter Posten mit Notiz, **wenn** der Benutzer „Rückgängig" wählt, **dann** erscheint der Posten
  wieder mit seiner Notiz.

## Nicht im Umfang

- Mehrere Notizen pro Posten, Notizen zum ganzen Plan
- Formatierung (Fettschrift, Links, Listen), Anhänge
- Suche oder Filter nach Notizen
- Verlauf früherer Notizen

## Umsetzung

Folgt nach der Umsetzung.

## Offene Fragen

- Reichen 1000 Zeichen als Obergrenze (PN-2)?
- Soll die Tabelle die Notiz vollständig unter der Beschreibung zeigen (PN-7) oder nur ein Symbol, das die Notiz beim
  Daraufzeigen einblendet? Vorschlag: vollständig, in kleiner grauer Schrift.
