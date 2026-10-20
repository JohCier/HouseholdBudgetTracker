# Plan als Vorlage übernehmen

Status: umgesetzt

## Zweck

Die meisten Einnahmen und Ausgaben ändern sich von Jahr zu Jahr kaum: Gehalt, Miete, Versicherungen, Sparrate. Heute
legt der Benutzer für jedes neue Jahr einen leeren Plan an (HP-5) und erfasst alle Posten erneut.

Mit dieser Spec übernimmt der Benutzer einen bestehenden Plan vollständig als Vorlage für ein anderes Jahr,
üblicherweise das Folgejahr, und passt dort nur noch an, was sich ändert. Er startet die Übernahme aus der Planansicht
eines Jahres, aus der Jahresübersicht aller Pläne ([jahresuebersicht.md](jahresuebersicht.md)) oder in einem Jahr, für
das es noch keinen Plan gibt.

Diese Spec ändert HP-2, HP-5 und das Nicht-Ziel „Plan aus dem Vorjahr kopieren" in [haushaltsplan.md](haushaltsplan.md)
sowie das Nicht-Ziel „Pläne aus der Jahresübersicht … kopieren" in [jahresuebersicht.md](jahresuebersicht.md): Ein
neuer Plan ist weiterhin leer, außer er entsteht durch Übernahme, und ein bestehender Plan kann durch eine Übernahme
überschrieben werden, wenn der Benutzer das bestätigt.

## Fachliche Regeln

### Quelle und Zieljahr

- **PU-1**: Der Benutzer kann jeden seiner Pläne (HP-4) als Vorlage übernehmen. Der übernommene Plan heißt Quelle,
  das Jahr, in das übernommen wird, Zieljahr.
- **PU-2**: Das Zieljahr wählt der Benutzer frei im erlaubten Bereich (HP-1), auch vor dem Jahr der Quelle.
  Vorgeschlagen wird das Folgejahr, also das Jahr direkt nach der Quelle; für eine Quelle 2200 das Vorjahr 2199.
  Das Jahr der Quelle selbst ist als Zieljahr nicht möglich.

### Was übernommen wird

- **PU-3**: Übernommen werden alle Posten der Quelle, jeweils mit Art, Beschreibung, Betrag, Kategorie,
  Unterkategorie und Notiz sowie der Angabe monatlich oder jährlich (MJ-1 in
  [monats-jahresbetrag.md](monats-jahresbetrag.md)), sobald es sie gibt.
- **PU-4**: Übernommen wird auch die Reihenfolge: die der Unterkategorie-Gruppen je Bereich (UK-14) und die der Posten
  innerhalb ihrer Gruppe. Der Plan im Zieljahr sieht in der Karte „Einträge" danach genauso aus wie die Quelle.
- **PU-5**: Die Quelle bleibt unverändert. Die übernommenen Posten sind eigenständig: Spätere Änderungen in einem der
  beiden Pläne wirken nicht auf den anderen.
- **PU-6**: Auch ein Plan ohne Posten lässt sich übernehmen; dann ist der Plan im Zieljahr danach leer.
- **PU-7**: Die Übernahme gelingt ganz oder gar nicht. Scheitert sie, ist im Zieljahr alles wie vorher.

### Zieljahr ohne und mit Plan

- **PU-8**: Gibt es im Zieljahr noch keinen Plan, legt die Übernahme ihn an; er gehört dem Benutzer (HP-3).
- **PU-9**: Gibt es im Zieljahr schon einen Plan, auch einen leeren, wird er nur nach ausdrücklicher Bestätigung
  überschrieben (PU-14). Überschreiben heißt: Alle Posten des Plans im Zieljahr werden entfernt und durch die
  übernommenen ersetzt, ebenso seine Reihenfolge der Gruppen. Der Plan selbst bleibt derselbe (gleiche ID, gleicher
  Besitzer). Ohne Bestätigung ändert sich nichts.
- **PU-10**: Überschriebene Posten lassen sich nicht wiederherstellen; „Rückgängig" (PO-9) gibt es dafür nicht.

### Protokoll

- **PU-11**: Die Übernahme wird als ein Event `HouseholdPlanCopied` mit Quelle, Ziel und der Angabe protokolliert, ob
  ein bestehender Plan überschrieben wurde (HP-7). Wird der Plan neu angelegt, kommt `HouseholdPlanCreated` hinzu. Die
  übernommenen und die überschriebenen Posten erzeugen keine eigenen Events `IncomeAdded`, `ExpenseAdded` oder
  `BudgetItemRemoved`.

### Anzeige

- **PU-12**: Die Aktion „Als Vorlage übernehmen" gibt es
  - in der Planansicht eines Jahres, solange ein Plan angezeigt wird (Quelle ist dieser Plan),
  - in jeder Zeile der Jahresübersicht (Quelle ist der Plan der Zeile); sie öffnet nicht den Plan der Zeile (JU-11),
  - in der Planansicht eines Jahres ohne Plan neben „Haushaltsplan anlegen" als „Aus anderem Jahr übernehmen", sofern
    der Benutzer mindestens einen Plan hat (Zieljahr ist dieses Jahr).
- **PU-13**: Vor der Übernahme fragt ein Dialog nach, z. B. „Plan 2026 als Vorlage übernehmen". Darin wählt der
  Benutzer das Zieljahr (vorbelegt nach PU-2) bzw. bei „Aus anderem Jahr übernehmen" die Quelle unter seinen Plänen
  (vorbelegt mit dem neuesten Plan vor dem Zieljahr, sonst dem ältesten danach). Der Dialog nennt die Anzahl der Posten,
  die übernommen werden, und hat die Schaltflächen „Übernehmen" und „Abbrechen". „Abbrechen" ändert nichts.
- **PU-14**: Gibt es im gewählten Zieljahr schon einen Plan, zeigt der Dialog vor dem Übernehmen den Hinweis „Für 2027
  gibt es bereits einen Haushaltsplan mit 12 Posten. Er wird überschrieben, die Posten lassen sich nicht
  wiederherstellen." Die Schaltfläche heißt dann „Überschreiben" statt „Übernehmen".
- **PU-15**: Nach der Übernahme ist der Plan im Zieljahr geöffnet, egal von wo sie gestartet wurde, und eine Meldung
  bestätigt „Plan 2026 nach 2027 übernommen.". Zurück in der Jahresübersicht steht das Zieljahr mit den neuen Summen.

## Akzeptanzkriterien

- **Gegeben** ein Plan 2026 mit Einnahme „Gehalt" und Ausgabe „Wartung" (Lebensunterhalt, Unterkategorie „Auto",
  Notiz „Inspektion im Mai") und kein Plan 2027, **wenn** der Benutzer in der Planansicht 2026 „Als Vorlage
  übernehmen" wählt, das vorgeschlagene Zieljahr 2027 lässt und „Übernehmen" wählt, **dann** ist der Plan 2027
  geöffnet und enthält beide Posten mit denselben Angaben, und die Auswertung 2027 gleicht der von 2026
  (PU-1 bis PU-3, PU-8, PU-12, PU-13, PU-15).
- **Gegeben** ein Plan 2026, **wenn** der Benutzer im Dialog das Zieljahr 2030 wählt, **dann** ist danach der Plan
  2030 geöffnet und enthält die Posten von 2026 (PU-2).
- **Gegeben** der Dialog für die Quelle 2026, **dann** lässt sich 2026 nicht als Zieljahr wählen, ebenso wenig 1899
  oder 2201 (PU-2).
- **Gegeben** ein Plan 2026, in dem der Benutzer die Gruppen und Posten umsortiert hat, **wenn** er ihn übernimmt,
  **dann** zeigt die Karte „Einträge" im Zieljahr dieselbe Reihenfolge (PU-4).
- **Gegeben** ein übernommener Plan 2027, **wenn** der Benutzer dort den Betrag eines Postens ändert und einen anderen
  löscht, **dann** ist der Plan 2026 unverändert (PU-5).
- **Gegeben** die Jahresübersicht mit den Zeilen 2026 und 2025, **wenn** der Benutzer in der Zeile 2026 „Als Vorlage
  übernehmen" und „Übernehmen" wählt, **dann** ist der Plan 2027 geöffnet; zurück über „Alle Jahre" zeigt die
  Jahresübersicht die Zeile 2027 oben (PU-12, PU-15).
- **Gegeben** ein Plan 2026 und die Planansicht 2027 ohne Plan, **wenn** der Benutzer „Aus anderem Jahr übernehmen"
  wählt, **dann** ist 2026 als Quelle vorbelegt; nach „Übernehmen" ist der Plan 2027 geöffnet (PU-12, PU-13).
- **Gegeben** ein Benutzer ohne Plan, **dann** bietet die Planansicht nur „Haushaltsplan anlegen" an (PU-12).
- **Gegeben** Pläne für 2026 und 2027 (2027 mit 12 Posten), **wenn** der Benutzer 2026 nach 2027 übernehmen will,
  **dann** zeigt der Dialog den Hinweis nach PU-14 und „Überschreiben"; nach „Überschreiben" enthält 2027 genau die
  Posten von 2026 in ihrer Reihenfolge (PU-9, PU-14).
- **Gegeben** derselbe Dialog mit Hinweis, **wenn** der Benutzer „Abbrechen" wählt, **dann** ist der Plan 2027
  unverändert (PU-9, PU-13).
- **Gegeben** ein Plan 2027 existiert schon, **wenn** die Übernahme von 2026 nach 2027 über die API ohne Bestätigung
  des Überschreibens angefordert wird, **dann** antwortet die API mit 409, und 2027 ist unverändert (PU-9).
- **Gegeben** ein leerer Plan 2026, **wenn** er nach 2027 übernommen wird, **dann** gibt es einen leeren Plan 2027
  (PU-6).
- **Gegeben** ein Zieljahr außerhalb von 1900 bis 2200 oder gleich dem Jahr der Quelle, **wenn** die Übernahme über die
  API angefordert wird, **dann** antwortet die API mit 400 (PU-2).
- **Gegeben** Benutzer A mit einem Plan, **wenn** Benutzer B diesen Plan übernehmen will, **dann** antwortet die API
  mit 404, und für B entsteht oder ändert sich kein Plan (HP-4).
- **Gegeben** eine Übernahme in ein Jahr ohne Plan, **dann** enthält das Protokoll `HouseholdPlanCreated` und
  `HouseholdPlanCopied`; bei einer Übernahme mit Überschreiben nur `HouseholdPlanCopied`; in beiden Fällen kein
  `IncomeAdded`, `ExpenseAdded` oder `BudgetItemRemoved` (PU-11).

## Nicht im Umfang

- Mehrere Pläne zusammenführen oder Posten zu einem bestehenden Plan hinzufügen, ohne ihn zu überschreiben
- Nur einen Teil der Posten übernehmen oder Beträge bei der Übernahme pauschal anpassen (z. B. +3 %)
- Überschreiben rückgängig machen (PU-10)
- Pläne löschen, siehe [plan-loeschen.md](plan-loeschen.md)

## Umsetzung

- Domäne: `HouseholdPlan.createFrom` (PU-8) und `HouseholdPlan.overwriteWith` (PU-9) kopieren über
  `BudgetItem.copy` alle Posten mit neuer ID und alle Werte des Postens (PU-3, PU-5) sowie die Reihenfolge der Posten
  und `SubcategoryOrder` (PU-4); beide lehnen das Jahr der Quelle mit `DomainValidationException` ab (PU-2) und
  zeichnen `HouseholdPlanCopied` auf (PU-11), `createFrom` über `create` zusätzlich `HouseholdPlanCreated`. Die
  Angabe monatlich/jährlich aus [monats-jahresbetrag.md](monats-jahresbetrag.md) übernimmt `BudgetItem.copy`, sobald
  `BudgetItem` sie trägt
- Ablauf: `HouseholdPlanApplicationService.copyPlan` mit `CopyHouseholdPlanCommand` lädt die Quelle (HP-4), sucht
  den Plan des Zieljahres über `HouseholdPlanRepository.findFor` (gesperrt wie `findById`) und legt ihn an oder
  überschreibt ihn nur mit `overwrite`, sonst `HouseholdPlanAlreadyExistsException` (409); alles in einer
  Transaktion (PU-7)
- API: `POST /api/plans/{planId}/copies` mit `CopyHouseholdPlanRequest` (`targetYear` 1900 bis 2200, `overwrite`),
  antwortet mit `CopiedHouseholdPlanResponse`: 201 für einen neuen Plan, 200 nach dem Überschreiben
- Frontend: `budget/ui/copy-plan-dialog.ts` fragt Zieljahr bzw. Vorlage ab, prüft über `GET /api/plans?year=`, ob
  das Zieljahr einen Plan hat, und warnt dann (PU-13, PU-14); geöffnet aus `budget/budget-dashboard.ts`
  („Als Vorlage übernehmen", in Jahren ohne Plan „Aus anderem Jahr übernehmen" mit den Jahren aus
  `GET /api/plans/years`) und je Zeile aus `budget/yearly-overview-page.ts` (PU-12); danach ist das Zieljahr
  geöffnet (PU-15)
- Tests: `HouseholdPlanCopyTest`, `HouseholdPlanApplicationServiceTest`, `HouseholdPlanControllerTest`,
  `JdbcHouseholdPlanRepositoryTest`, `HouseholdPlanIntegrationTest`, `copy-plan-dialog.spec.ts`,
  `budget-dashboard.spec.ts`, `yearly-overview-page.spec.ts`, `budget-api.spec.ts`
- E2E: `e2e/tests/plan-uebernehmen.spec.ts`

## Offene Fragen

Keine. Entschieden am 20.10.2026: Zieljahr frei wählbar (PU-2), Überschreiben nach Rückfrage (PU-9, PU-14),
„Aus anderem Jahr übernehmen" in Jahren ohne Plan (PU-12), danach den Plan im Zieljahr öffnen (PU-15), vor der
Übernahme nachfragen (PU-13).
