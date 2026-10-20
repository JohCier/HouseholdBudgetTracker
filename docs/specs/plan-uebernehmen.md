# Plan ins Folgejahr übernehmen

Status: geplant

## Zweck

Die meisten Einnahmen und Ausgaben ändern sich von Jahr zu Jahr kaum: Gehalt, Miete, Versicherungen, Sparrate. Heute
legt der Benutzer für jedes neue Jahr einen leeren Plan an (HP-5) und erfasst alle Posten erneut.

Mit dieser Spec übernimmt der Benutzer einen bestehenden Plan vollständig als Vorlage für das Folgejahr und passt dort
nur noch an, was sich ändert. Er startet die Übernahme aus der Planansicht eines Jahres oder aus der Jahresübersicht
aller Pläne ([jahresuebersicht.md](jahresuebersicht.md)).

Diese Spec ändert HP-5 und das Nicht-Ziel „Plan aus dem Vorjahr kopieren" in [haushaltsplan.md](haushaltsplan.md)
sowie das Nicht-Ziel „Pläne aus der Jahresübersicht … kopieren" in [jahresuebersicht.md](jahresuebersicht.md): Ein
neuer Plan ist weiterhin leer, außer er entsteht durch Übernahme.

## Fachliche Regeln

### Übernahme

- **PU-1**: Der Benutzer kann jeden seiner Pläne (HP-4) als Vorlage ins Folgejahr übernehmen. Folgejahr ist immer das
  Kalenderjahr direkt nach dem Jahr des Plans: aus 2026 wird 2027.
- **PU-2**: Die Übernahme legt einen neuen Plan für das Folgejahr an, der dem Benutzer gehört (HP-3). Der
  übernommene Plan (Quelle) bleibt unverändert.
- **PU-3**: Übernommen werden alle Posten der Quelle, jeweils mit Art, Beschreibung, Betrag, Kategorie,
  Unterkategorie und Notiz sowie der Angabe monatlich oder jährlich (MJ-1 in
  [monats-jahresbetrag.md](monats-jahresbetrag.md)), sobald es sie gibt.
- **PU-4**: Übernommen wird auch die Reihenfolge: die der Unterkategorie-Gruppen je Bereich (UK-14) und die der Posten
  innerhalb ihrer Gruppe. Der neue Plan sieht in der Karte „Einträge" also genauso aus wie die Quelle.
- **PU-5**: Die übernommenen Posten sind eigenständig. Spätere Änderungen in einem der beiden Pläne wirken nicht auf
  den anderen; ein Löschen in einem Plan lässt den anderen unberührt.
- **PU-6**: Auch ein Plan ohne Posten lässt sich übernehmen; dann entsteht ein leerer Plan für das Folgejahr.
- **PU-7**: Die Übernahme gelingt ganz oder gar nicht. Scheitert sie, gibt es keinen Plan für das Folgejahr.

### Wenn die Übernahme nicht möglich ist

- **PU-8**: Gibt es für das Folgejahr schon einen Plan, auch einen leeren, wird nichts übernommen und nichts
  überschrieben (HP-2). Die Oberfläche zeigt den Hinweis „Für 2027 gibt es bereits einen Haushaltsplan. Er wird nicht
  überschrieben." und bietet „Plan 2027 öffnen" an.
- **PU-9**: Liegt das Folgejahr außerhalb des erlaubten Bereichs (HP-1), also für einen Plan 2200, ist die Übernahme
  nicht möglich und die Aktion deaktiviert.

### Protokoll

- **PU-10**: Die Übernahme wird als ein Event `HouseholdPlanCopied` mit Quelle und neuem Plan protokolliert (HP-7),
  zusätzlich zu `HouseholdPlanCreated`. Die übernommenen Posten erzeugen keine eigenen Events `IncomeAdded` bzw.
  `ExpenseAdded`.

### Anzeige

- **PU-11**: Die Planansicht eines Jahres bietet die Aktion „Ins Folgejahr übernehmen" an, solange ein Plan angezeigt
  wird.
- **PU-12**: Die Jahresübersicht bietet dieselbe Aktion in jeder Zeile an. Sie öffnet nicht den Plan der Zeile (JU-11),
  sondern nur die Übernahme.
- **PU-13**: Vor der Übernahme fragt ein Dialog nach: „Plan 2026 als Vorlage für 2027 übernehmen?" mit der Anzahl der
  Posten, die übernommen werden, und den Schaltflächen „Übernehmen" und „Abbrechen". „Abbrechen" ändert nichts.
- **PU-14**: Nach der Übernahme ist der neue Plan geöffnet, aus der Planansicht wie aus der Jahresübersicht, und eine
  Meldung bestätigt „Plan 2026 nach 2027 übernommen.". Zurück in der Jahresübersicht steht 2027 als neue Zeile.

## Akzeptanzkriterien

- **Gegeben** ein Plan 2026 mit Einnahme „Gehalt" und Ausgabe „Wartung" (Lebensunterhalt, Unterkategorie „Auto",
  Notiz „Inspektion im Mai") und kein Plan 2027, **wenn** der Benutzer in der Planansicht 2026 „Ins Folgejahr
  übernehmen" und „Übernehmen" wählt, **dann** ist der Plan 2027 geöffnet und enthält beide Posten mit denselben
  Angaben, und die Auswertung 2027 gleicht der von 2026 (PU-1 bis PU-3, PU-11, PU-14).
- **Gegeben** ein Plan 2026, in dem der Benutzer die Gruppen und Posten umsortiert hat, **wenn** er ihn ins Folgejahr
  übernimmt, **dann** zeigt die Karte „Einträge" 2027 dieselbe Reihenfolge (PU-4).
- **Gegeben** ein übernommener Plan 2027, **wenn** der Benutzer dort den Betrag eines Postens ändert und einen anderen
  löscht, **dann** ist der Plan 2026 unverändert (PU-2, PU-5).
- **Gegeben** die Jahresübersicht mit den Zeilen 2026 und 2025, **wenn** der Benutzer in der Zeile 2026 „Ins
  Folgejahr übernehmen" und „Übernehmen" wählt, **dann** ist der Plan 2027 geöffnet; zurück über „Alle Jahre" zeigt
  die Jahresübersicht die Zeile 2027 oben (PU-12, PU-14).
- **Gegeben** Pläne für 2026 und 2027, **wenn** der Benutzer 2026 ins Folgejahr übernehmen will, **dann** zeigt die
  Oberfläche den Hinweis nach PU-8, der Plan 2027 bleibt unverändert, und „Plan 2027 öffnen" öffnet ihn (PU-8).
- **Gegeben** der Dialog „Plan 2026 als Vorlage für 2027 übernehmen?", **wenn** der Benutzer „Abbrechen" wählt,
  **dann** gibt es weiterhin keinen Plan 2027 (PU-13).
- **Gegeben** ein leerer Plan 2026, **wenn** er übernommen wird, **dann** gibt es einen leeren Plan 2027 (PU-6).
- **Gegeben** ein Plan 2200, **dann** ist „Ins Folgejahr übernehmen" deaktiviert; über die API antwortet die
  Übernahme mit 400 (PU-9).
- **Gegeben** ein Plan 2027 existiert schon, **wenn** die Übernahme von 2026 über die API angefordert wird, **dann**
  antwortet die API mit 409 (PU-8).
- **Gegeben** Benutzer A mit einem Plan, **wenn** Benutzer B diesen Plan übernehmen will, **dann** antwortet die API
  mit 404, und für B entsteht kein Plan (HP-4).
- **Gegeben** eine erfolgreiche Übernahme, **dann** enthält das Protokoll `HouseholdPlanCreated` und
  `HouseholdPlanCopied`, aber kein `IncomeAdded` oder `ExpenseAdded` für die übernommenen Posten (PU-10).

## Nicht im Umfang

- Übernahme in ein frei gewähltes Jahr, etwa ins Vorjahr oder zwei Jahre weiter
- Übernahme in einen bestehenden Plan, Zusammenführen oder Überschreiben von Plänen
- Nur einen Teil der Posten übernehmen oder Beträge bei der Übernahme pauschal anpassen (z. B. +3 %)
- „Aus Vorjahr übernehmen" in der Planansicht eines Jahres ohne Plan; dort bleibt „Haushaltsplan anlegen" (siehe
  Offene Fragen)
- Pläne löschen

## Umsetzung

Wird nach der Umsetzung ergänzt. Die Aktion in der Jahresübersicht (PU-12) setzt
[jahresuebersicht.md](jahresuebersicht.md) voraus, die Übernahme der Angabe monatlich/jährlich (PU-3)
[monats-jahresbetrag.md](monats-jahresbetrag.md); umgesetzt wird diese Spec nach den beiden.

## Offene Fragen

1. Ziel der Übernahme (PU-1): immer das Folgejahr (Vorschlag), oder soll der Benutzer das Zieljahr wählen können?
2. Folgejahr hat schon einen Plan (PU-8): nur Hinweis, nichts überschreiben (Vorschlag), oder auf Nachfrage
   überschreiben?
3. Gegenrichtung: Soll die Planansicht eines Jahres ohne Plan neben „Haushaltsplan anlegen" auch „Aus 2026
   übernehmen" anbieten, wenn es einen Plan für das Vorjahr gibt? Vorschlag: zunächst nicht, ggf. eigene Spec.
4. Nach der Übernahme aus der Jahresübersicht (PU-14): den neuen Plan öffnen (Vorschlag), oder in der
   Jahresübersicht bleiben und die neue Zeile zeigen?
5. Nachfrage vor der Übernahme (PU-13): mit Dialog (Vorschlag), oder sofort übernehmen, weil nichts überschrieben
   wird?
