# Haushaltsplan löschen

Status: geplant

## Zweck

Ein Plan, der versehentlich, im falschen Jahr oder nur zum Ausprobieren angelegt oder übernommen wurde, bleibt heute
für immer in der Jahresübersicht stehen und verfälscht dort die Veränderung zum Vorjahr. Der Benutzer kann ihn nur
leeren, indem er jeden Posten einzeln löscht, und auch dann bleibt eine Zeile mit 0,00 €.

Mit dieser Spec löscht der Benutzer einen ganzen Plan samt allen Posten, aus der Jahresübersicht
([jahresuebersicht.md](jahresuebersicht.md)) oder aus der Planansicht des Jahres.

Diese Spec ändert das Nicht-Ziel „Plan löschen" in [haushaltsplan.md](haushaltsplan.md), das Nicht-Ziel „Pläne aus der
Jahresübersicht löschen" in [jahresuebersicht.md](jahresuebersicht.md) und das Nicht-Ziel „Pläne löschen" in
[plan-uebernehmen.md](plan-uebernehmen.md).

## Fachliche Regeln

### Was gelöscht wird

- **PL-1**: Der Benutzer kann jeden seiner Pläne (HP-4) löschen, auch einen leeren. Pläne anderer Benutzer werden
  behandelt, als gäbe es sie nicht.
- **PL-2**: Gelöscht werden der Plan mit allen Posten und seiner Reihenfolge der Gruppen (UK-14). Danach gibt es für
  dieses Jahr keinen Plan mehr; der Benutzer kann dort wieder einen anlegen (HP-5) oder einen übernehmen (PU-8).
- **PL-3**: Andere Pläne bleiben unverändert, auch solche, die aus dem gelöschten übernommen wurden oder als Vorlage
  für ihn dienten (PU-5).
- **PL-4**: Das Löschen gelingt ganz oder gar nicht.
- **PL-5**: Ein gelöschter Plan lässt sich nicht wiederherstellen; „Rückgängig" (PO-9) gibt es dafür nicht.

### Protokoll

- **PL-6**: Das Löschen wird als ein Event `HouseholdPlanDeleted` mit Plan, Jahr und Anzahl der gelöschten Posten
  protokolliert (HP-7). Die gelöschten Posten erzeugen keine eigenen Events `BudgetItemRemoved`. Frühere Einträge im
  Protokoll zu diesem Plan bleiben erhalten.

### Anzeige

- **PL-7**: Die Aktion „Plan löschen" gibt es
  - in jeder Zeile der Jahresübersicht neben „Als Vorlage übernehmen"; sie öffnet nicht den Plan der Zeile (JU-11),
  - in der Planansicht eines Jahres, solange ein Plan angezeigt wird, neben „Als Vorlage übernehmen".
- **PL-8**: Vor dem Löschen fragt ein Dialog nach: Titel „Plan 2026 löschen", Text „Der Haushaltsplan 2026 mit 12
  Posten wird gelöscht. Das lässt sich nicht rückgängig machen." (bei einem leeren Plan „… mit 0 Posten …"), die
  Schaltflächen „Löschen" und „Abbrechen". „Abbrechen" ändert nichts.
- **PL-9**: Nach dem Löschen bestätigt eine Meldung „Plan 2026 gelöscht.".
  - Aus der Jahresübersicht gestartet, bleibt die Jahresübersicht offen; die Zeile ist verschwunden, und die
    Veränderung zum Vorjahr der Zeile des Folgejahres entfällt (JU-4).
  - Aus der Planansicht gestartet, zeigt die Anwendung danach die Jahresübersicht.

## Akzeptanzkriterien

- **Gegeben** die Jahresübersicht mit den Zeilen 2026 (12 Posten) und 2025, **wenn** der Benutzer in der Zeile 2026
  „Plan löschen" wählt, **dann** nennt der Dialog „Plan 2026 löschen" und 12 Posten; nach „Löschen" bleibt die
  Jahresübersicht offen, zeigt nur noch 2025 und die Meldung „Plan 2026 gelöscht." (PL-1, PL-2, PL-7 bis PL-9).
- **Gegeben** derselbe Dialog, **wenn** der Benutzer „Abbrechen" wählt, **dann** bleibt der Plan 2026 mit allen Posten
  erhalten (PL-8).
- **Gegeben** die Planansicht 2026 mit Plan, **wenn** der Benutzer „Plan löschen" und im Dialog „Löschen" wählt,
  **dann** zeigt die Anwendung die Jahresübersicht ohne die Zeile 2026 und die Meldung „Plan 2026 gelöscht."
  (PL-7, PL-9).
- **Gegeben** ein gelöschter Plan 2026, **wenn** der Benutzer die Planansicht 2026 öffnet, **dann** bietet sie
  „Haushaltsplan anlegen" an; ein neu angelegter Plan 2026 ist leer (PL-2).
- **Gegeben** Pläne für 2025, 2026 und 2027, **wenn** der Benutzer 2026 löscht, **dann** zeigt die Zeile 2027 keine
  Veränderung zum Vorjahr mehr, und 2025 und 2027 sind mit allen Posten unverändert (PL-3, JU-4).
- **Gegeben** ein leerer Plan, **wenn** der Benutzer ihn löscht, **dann** nennt der Dialog 0 Posten, und der Plan ist
  danach gelöscht (PL-1, PL-8).
- **Gegeben** Benutzer A mit einem Plan, **wenn** Benutzer B diesen Plan über die API löschen will, **dann** antwortet
  die API mit 404, und der Plan von A bleibt erhalten (PL-1, HP-4).
- **Gegeben** ein bereits gelöschter Plan, **wenn** er erneut über die API gelöscht werden soll, **dann** antwortet die
  API mit 404 (PL-2).
- **Gegeben** ein Plan mit Posten wird gelöscht, **dann** enthält das Protokoll genau ein `HouseholdPlanDeleted` und
  kein `BudgetItemRemoved` (PL-6).

## Nicht im Umfang

- Gelöschte Pläne wiederherstellen oder einen Papierkorb (PL-5)
- Mehrere Pläne auf einmal löschen
- Plan auf ein anderes Jahr verschieben
- Benutzerkonto löschen

## Umsetzung

Noch nicht umgesetzt. Geplant:

- Domäne: `HouseholdPlan.delete` zeichnet `HouseholdPlanDeleted` auf (PL-6); `HouseholdPlanRepository.delete`
- Ablauf: `HouseholdPlanApplicationService.deletePlan` mit `DeleteHouseholdPlanCommand` lädt den Plan gesperrt und
  nur für den Besitzer (HP-4, HP-6) und löscht ihn in einer Transaktion (PL-4); Posten und Reihenfolge fallen über
  `ON DELETE CASCADE` weg
- API: `DELETE /api/plans/{planId}`, antwortet mit 204, für fremde oder unbekannte Pläne mit 404
- Frontend: Bestätigungsdialog wie `budget/ui/copy-plan-dialog.ts`, geöffnet aus `budget/yearly-overview-page.ts` und
  `budget/budget-dashboard.ts`
- E2E: `e2e/tests/plan-loeschen.spec.ts`

## Offene Fragen

1. Reicht ein einfacher Bestätigungsdialog (PL-8), oder soll der Benutzer zur Sicherheit das Jahr eintippen?
   Vorschlag: einfacher Dialog, weil er Jahr und Anzahl der Posten nennt.
2. Wie sieht „Plan löschen" aus? Vorschlag: in der Planansicht eine Textschaltfläche mit Papierkorb-Symbol neben
   „Als Vorlage übernehmen", in der Jahresübersicht ein Papierkorb-Symbol mit Tooltip neben dem Symbol fürs
   Übernehmen, beide in der Warnfarbe.
3. Nach dem Löschen in der Planansicht: zur Jahresübersicht (Vorschlag, PL-9) oder im selben Jahr bleiben und dort
   „Haushaltsplan anlegen" anbieten?
