# Gelöschte Pläne

Status: umgesetzt

## Zweck

Heute löscht „Plan löschen" einen Haushaltsplan mit allen Posten endgültig (PL-2, PL-5), und „Als Vorlage übernehmen"
überschreibt einen vorhandenen Plan ebenso endgültig (PU-9, PU-10). Ein Fehlklick in der Jahresübersicht oder das
falsche Zieljahr kostet so einen ganzen Plan, der sich nur durch erneutes Erfassen aller Posten zurückholen lässt.

Mit dieser Spec legt das Löschen einen Plan nur noch unter **Gelöschte Pläne** ab. Dort sieht der Benutzer alle
gelöschten Pläne, stellt sie wieder her oder löscht sie endgültig. Gibt es im Jahr eines wiederhergestellten Plans
schon einen anderen Plan, wird dieser erst nach Rückfrage abgelöst und landet seinerseits unter „Gelöschte Pläne".

Diese Spec ändert HP-2 in [haushaltsplan.md](haushaltsplan.md), in [plan-loeschen.md](plan-loeschen.md) PL-2, PL-5,
PL-6, PL-8, PL-9 und das Nicht-Ziel
„Gelöschte Pläne wiederherstellen oder einen Papierkorb", in [plan-uebernehmen.md](plan-uebernehmen.md) PU-9, PU-10,
PU-11, PU-14 und das Nicht-Ziel „Überschreiben rückgängig machen" sowie in [jahresuebersicht.md](jahresuebersicht.md)
JU-1. Im Glossar ändern sich „Haushaltsplan" (höchstens ein **aktiver** Plan je Benutzer und Jahr) und „Plan löschen".

## Fachliche Regeln

### Aktive und gelöschte Pläne

- **GP-1**: Ein Plan ist entweder **aktiv** oder **gelöscht**. Je Benutzer und Jahr gibt es höchstens einen aktiven
  Plan, aber beliebig viele gelöschte (ändert
  HP-2: „höchstens einen Plan pro Jahr" gilt nur für aktive Pläne).
- **GP-2**: Ein gelöschter Plan behält alle Posten, ihre Reihenfolge und die Reihenfolge der Gruppen (UK-14)
  unverändert. Er merkt sich, wann er gelöscht wurde (Datum und Uhrzeit).
- **GP-3**: Gelöschte Pläne erscheinen nirgends außer unter „Gelöschte Pläne": nicht in der Jahresübersicht (JU-1),
  nicht in der Planansicht, nicht beim Wechsel des Jahres mit den Pfeilen, nicht als Quelle bei „Aus anderem Jahr
  übernehmen" (PU-13) und nicht als Vorjahr für die Veränderung (JU-4). Ein Jahr, dessen Plan gelöscht ist, gilt
  überall als Jahr ohne Plan.
- **GP-4**: Ein gelöschter Plan lässt sich weder öffnen noch ändern: keine Posten erfassen, ändern oder löschen, nicht
  als Vorlage übernehmen. Die API antwortet darauf mit 404 wie für einen unbekannten Plan.
- **GP-5**: Gelöschte Pläne bleiben, bis der Benutzer sie wiederherstellt oder endgültig löscht; eine Frist, nach der
  sie automatisch verschwinden, gibt es nicht.
- **GP-6**: Einmalposten ([EP-5](einmalposten.md)) und das geplante Vermögen hängen nicht am Plan. Löschen,
  Wiederherstellen und endgültiges Löschen eines Plans ändern sie nicht.

### Löschen (ändert PL-2, PL-5, PL-8, PL-9)

- **GP-7**: „Plan löschen" (PL-7) macht einen aktiven Plan zu einem gelöschten (ändert PL-2). Danach hat das Jahr
  keinen aktiven Plan mehr; der Benutzer kann dort wieder einen anlegen (HP-5) oder übernehmen (PU-8).
- **GP-8**: Der Dialog vor dem Löschen (PL-8) lautet: Titel „Plan 2026 löschen", Text „Der Haushaltsplan 2026 mit
  12 Posten wird unter „Gelöschte Pläne" abgelegt und lässt sich dort wiederherstellen.", Schaltflächen „Löschen" und
  „Abbrechen".
- **GP-9**: Die Meldung nach dem Löschen „Plan 2026 gelöscht." (PL-9) bietet „Rückgängig" an. „Rückgängig" stellt
  den Plan wieder her wie GP-12; die Anwendung bleibt dabei auf der aktuellen Seite, und die Zeile erscheint wieder.
  Hat der Benutzer inzwischen einen anderen Plan für dasselbe Jahr angelegt, fragt sie nach wie GP-13.

### Wiederherstellen

- **GP-10**: Der Benutzer kann jeden seiner gelöschten Pläne wiederherstellen. Gelöschte Pläne anderer Benutzer werden
  behandelt, als gäbe es sie nicht (HP-4).
- **GP-11**: Wiederherstellen macht den gelöschten Plan mit allen Posten und Reihenfolgen wieder aktiv, unter
  seinem bisherigen Jahr und seiner bisherigen ID. Er verschwindet aus „Gelöschte Pläne".
- **GP-12**: Hat das Jahr keinen aktiven Plan, stellt die Anwendung ohne Rückfrage wieder her und meldet „Plan 2026
  wiederhergestellt.". Die Seite „Gelöschte Pläne" bleibt offen.
- **GP-13**: Hat das Jahr schon einen aktiven Plan, fragt ein Dialog nach: Titel „Plan 2026 wiederherstellen", Text
  „Für 2026 gibt es bereits einen Haushaltsplan mit 3 Posten. Er wird durch den gelöschten Plan mit 12 Posten ersetzt
  und unter „Gelöschte Pläne" abgelegt.", Schaltflächen „Überschreiben" und „Abbrechen". „Abbrechen" ändert nichts.
- **GP-14**: „Überschreiben" tauscht die beiden Pläne: Der bisher aktive Plan wird gelöscht (wie GP-7, mit dem
  aktuellen Zeitpunkt) und erscheint unter „Gelöschte Pläne", der wiederhergestellte ist aktiv. Meldung: „Plan 2026
  wiederhergestellt.". Ohne Bestätigung antwortet die API mit 409 und ändert nichts.
- **GP-15**: Wiederherstellen und Überschreiben gelingen ganz oder gar nicht.

### Endgültig löschen

- **GP-16**: Unter „Gelöschte Pläne" kann der Benutzer einen gelöschten Plan endgültig löschen. Ein Dialog fragt
  nach: Titel „Plan 2026 endgültig löschen", Text „Der gelöschte Haushaltsplan 2026 mit 12 Posten wird endgültig
  gelöscht. Das lässt sich nicht rückgängig machen.", Schaltflächen „Endgültig löschen" und „Abbrechen".
- **GP-17**: Endgültig gelöscht werden der Plan, alle Posten und die Reihenfolge der Gruppen (wie bisher PL-2). Danach
  lässt er sich nicht wiederherstellen. Meldung: „Plan 2026 endgültig gelöscht.".
- **GP-18**: Nur gelöschte Pläne lassen sich endgültig löschen; ein aktiver Plan muss erst gelöscht werden. Die API
  antwortet für einen aktiven, fremden oder unbekannten Plan mit 404.

### Als Vorlage übernehmen (ändert PU-9, PU-10, PU-14)

- **GP-19**: Überschreibt eine Übernahme den Plan im Zieljahr (PU-9), wird dieser nicht mehr geleert, sondern
  gelöscht wie GP-7; für das Zieljahr entsteht ein neuer aktiver Plan mit den übernommenen Posten. Der überschriebene
  Plan lässt sich unter „Gelöschte Pläne" wiederherstellen (ersetzt PU-10).
- **GP-20**: Der Hinweis im Dialog (PU-14) lautet: „Für 2027 gibt es bereits einen Haushaltsplan mit 12 Posten. Er
  wird überschrieben und unter „Gelöschte Pläne" abgelegt."

### Seite „Gelöschte Pläne"

- **GP-21**: Unter der Tabelle der Jahresübersicht steht der Link „Gelöschte Pläne (2)" mit der Anzahl der gelöschten
  Pläne, nur wenn es mindestens einen gibt, auch wenn der Benutzer keinen aktiven Plan hat (JU-13). Er öffnet die
  Seite `/geloeschte-plaene`.
- **GP-22**: Die Seite heißt „Gelöschte Pläne" und hat oben „Alle Jahre" zurück zur Jahresübersicht. Je gelöschtem
  Plan zeigt sie eine Zeile mit Jahr, „Gelöscht am" (z. B. „29.10.2026, 20:15"), Anzahl der Posten, Einnahmen,
  Ausgaben und Überschuss (mit Kennzeichnung als Defizit, AW-3) sowie den Aktionen „Wiederherstellen" und „Endgültig
  löschen". Eine Veränderung zum Vorjahr gibt es dort nicht.
- **GP-23**: Die Beträge sind Monats- oder Jahresbeträge nach der Wahl in der Jahresübersicht (JM-7); die Seite hat
  keinen eigenen Umschalter. Die Zahlen rechnet das Backend (JU-9).
- **GP-24**: Sortiert ist nach Jahr, das neueste oben; mehrere gelöschte Pläne desselben Jahres stehen nach
  „Gelöscht am", der zuletzt gelöschte oben.
- **GP-25**: Ohne gelöschte Pläne zeigt die Seite „Keine gelöschten Pläne.". Das gilt auch, nachdem der Benutzer den
  letzten wiederhergestellt oder endgültig gelöscht hat.
- **GP-26**: Auf schmalen Bildschirmen (MA-1) steht jeder gelöschte Plan ohne waagerechtes Scrollen als Block: oben
  Jahr und „Gelöscht am", darunter Posten, Einnahmen, Ausgaben und Überschuss je mit Beschriftung, unten die Aktionen.

### Protokoll (ändert PL-6, PU-11)

- **GP-27**: Löschen wird weiter als `HouseholdPlanDeleted` protokolliert (PL-6), auch beim Ablösen nach GP-14 und
  GP-19. Wiederherstellen wird als `HouseholdPlanRestored` mit Plan und Jahr protokolliert, endgültiges Löschen als
  `HouseholdPlanPurged` mit Plan, Jahr und Anzahl der Posten. Keines davon erzeugt Events je Posten.
- **GP-28**: Eine Übernahme mit Überschreiben protokolliert `HouseholdPlanDeleted` für den abgelösten Plan,
  `HouseholdPlanCreated` für den neuen und `HouseholdPlanCopied` (ändert das Akzeptanzkriterium zu PU-11).

## Akzeptanzkriterien

- **Gegeben** die Jahresübersicht mit 2026 (12 Posten) und 2025, **wenn** der Benutzer in der Zeile 2026 „Plan
  löschen" und „Löschen" wählt, **dann** zeigt die Jahresübersicht nur 2025, die Meldung „Plan 2026 gelöscht." mit
  „Rückgängig" und unter der Tabelle „Gelöschte Pläne (1)" (GP-7 bis GP-9, GP-21).
- **Gegeben** dieselbe Meldung, **wenn** der Benutzer „Rückgängig" wählt, **dann** steht 2026 wieder mit 12 Posten
  in der Jahresübersicht, und der Link „Gelöschte Pläne" verschwindet (GP-9, GP-11).
- **Gegeben** der gelöschte Plan 2026, **wenn** der Benutzer `/jahre/2026` öffnet, **dann** bietet die Planansicht
  „Haushaltsplan anlegen" an, und ein neuer Plan 2026 ist leer (GP-3, GP-7).
- **Gegeben** Pläne 2025, 2026 und 2027 und 2026 wird gelöscht, **dann** zeigt 2027 keine Veränderung zum Vorjahr;
  nach dem Wiederherstellen von 2026 zeigt 2027 sie wieder (GP-3, JU-4).
- **Gegeben** der gelöschte Plan 2026 (12 Posten) und kein aktiver Plan 2026, **wenn** der Benutzer unter „Gelöschte
  Pläne" „Wiederherstellen" wählt, **dann** erscheint ohne Rückfrage „Plan 2026 wiederhergestellt.", die Seite zeigt
  „Keine gelöschten Pläne.", und die Planansicht 2026 enthält die 12 Posten in ihrer alten Reihenfolge (GP-11,
  GP-12, GP-25).
- **Gegeben** der gelöschte Plan 2026 (12 Posten, gelöscht am 20.10.2026) und ein neuer aktiver Plan 2026 (3 Posten),
  **wenn** der Benutzer „Wiederherstellen" wählt, **dann** nennt der Dialog 3 und 12 Posten; nach „Überschreiben" hat
  2026 die 12 Posten, und „Gelöschte Pläne" zeigt den Plan mit 3 Posten mit dem heutigen Datum (GP-13, GP-14).
- **Gegeben** derselbe Dialog, **wenn** der Benutzer „Abbrechen" wählt, **dann** bleibt 2026 mit 3 Posten aktiv, und
  der Plan mit 12 Posten bleibt gelöscht (GP-13).
- **Gegeben** ein aktiver Plan 2026, **wenn** das Wiederherstellen eines gelöschten Plans 2026 über die API ohne
  Bestätigung angefordert wird, **dann** antwortet die API mit 409, und beide Pläne bleiben, wie sie sind (GP-14).
- **Gegeben** zwei gelöschte Pläne 2026 (gelöscht am 20.10. und am 29.10.2026) und einer für 2024, **dann** zeigt die
  Seite zuerst 2026 vom 29.10., dann 2026 vom 20.10., dann 2024 (GP-24).
- **Gegeben** der gelöschte Plan 2026, **wenn** der Benutzer „Endgültig löschen" und im Dialog „Endgültig löschen"
  wählt, **dann** verschwindet er mit der Meldung „Plan 2026 endgültig gelöscht.", und er lässt sich über die API
  nicht mehr wiederherstellen (404) (GP-16, GP-17).
- **Gegeben** ein aktiver Plan, **wenn** er über die API endgültig gelöscht werden soll, **dann** antwortet die API mit
  404, und der Plan bleibt aktiv (GP-18).
- **Gegeben** ein gelöschter Plan, **wenn** über die API ein Posten darin erfasst oder er als Vorlage übernommen werden
  soll, **dann** antwortet die API mit 404 (GP-4).
- **Gegeben** Pläne 2026 und 2027 (2027 mit 12 Posten), **wenn** der Benutzer 2026 nach 2027 übernimmt und
  „Überschreiben" wählt, **dann** nennt der Hinweis „Gelöschte Pläne", 2027 enthält die Posten von 2026, und unter
  „Gelöschte Pläne" steht 2027 mit den alten 12 Posten, der sich wiederherstellen lässt (GP-19, GP-20).
- **Gegeben** Benutzer A mit einem gelöschten Plan, **wenn** Benutzer B ihn über die API wiederherstellen oder
  endgültig löschen will, **dann** antwortet die API mit 404, und B sieht ihn nicht unter „Gelöschte Pläne" (GP-10,
  GP-18).
- **Gegeben** der Plan 2026 mit Einmalposten in 2026, **wenn** der Benutzer den Plan löscht, wiederherstellt und
  endgültig löscht, **dann** bleiben die Einmalposten jedes Mal unverändert (GP-6).
- **Gegeben** ein Benutzer ohne aktive, aber mit einem gelöschten Plan, **dann** zeigt die Jahresübersicht „Noch keine
  Haushaltspläne." und den Link „Gelöschte Pläne (1)" (GP-21, JU-13).
- **Gegeben** die Seite „Gelöschte Pläne" auf einem 360 px breiten Bildschirm, **dann** steht jeder Plan als Block
  ohne waagerechtes Scrollen (GP-26).
- **Gegeben** ein Plan wird gelöscht, wiederhergestellt und endgültig gelöscht, **dann** enthält das Protokoll
  nacheinander `HouseholdPlanDeleted`, `HouseholdPlanRestored` und `HouseholdPlanPurged` und kein
  `BudgetItemRemoved` (GP-27).

## Nicht im Umfang

- Gelöschte Pläne öffnen, ansehen oder einzelne Posten daraus übernehmen
- Einen gelöschten Plan in ein anderes Jahr wiederherstellen
- Automatisches endgültiges Löschen nach einer Frist (GP-5), „Alle endgültig löschen"
- Mehrere Pläne auf einmal löschen oder wiederherstellen
- Gelöschte Posten (PO-9) oder Einmalposten und Vermögen unter „Gelöschte Pläne"

## Umsetzung

- Datenbank: `V11__household_plan_deleted.sql` ergänzt `household_plan.deleted_at`; die Eindeutigkeit von Besitzer und
  Jahr gilt nur noch für Zeilen ohne `deleted_at` (Teilindex `household_plan_owner_year_active_unique`, GP-1)
- Domäne: `HouseholdPlan.delete`, `restore` und `purge` zeichnen `HouseholdPlanDeleted`, `HouseholdPlanRestored` und
  `HouseholdPlanPurged` auf (GP-27); `HouseholdPlan.createFrom(…, replacesPlan)` für die Übernahme (GP-19).
  `HouseholdPlanRepository` sieht nur aktive Pläne, außer `findDeletedById`; `markDeleted`, `restore` (409 bei
  aktivem Plan des Jahres) und `remove` verwalten den Zustand (GP-2 bis GP-4). Read Models `DeletedPlans` (Sortierung
  GP-24) und `DeletedPlanEntry`; `YearlyOverview.deletedPlanCount` (GP-21)
- Ablauf: `HouseholdPlanApplicationService.deletePlan`, `restorePlan` (`RestoreHouseholdPlanCommand` mit `overwrite`,
  löst einen aktiven Plan des Jahres über `delete` ab, GP-13 bis GP-15), `purgePlan` (`PurgeHouseholdPlanCommand`,
  GP-16 bis GP-18), `getDeletedPlans` und `copyPlan` (GP-19). Ersetzen ändert zwei Pläne in einer Transaktion, damit es
  ganz oder gar nicht gelingt (GP-15, PU-7)
- Lesen: `JdbcHouseholdPlanOverviewQueryHandler` lässt gelöschte Pläne in Planansicht und Jahresübersicht aus (GP-3),
  zählt sie (GP-21) und listet sie mit Summen über `findDeletedPlansByOwner` (GP-22)
- API: `GET /api/plans/deleted` (`DeletedPlansResponse`), `POST /api/plans/{planId}/restore`
  (`RestoreHouseholdPlanRequest`, 204, 409 ohne Bestätigung, 404), `DELETE /api/plans/deleted/{planId}` (204, 404 für
  aktive, fremde und unbekannte Pläne); `YearlyOverviewResponse.deletedPlanCount`
- Frontend: `budget/deleted-plans-page.ts` unter `/geloeschte-plaene` mit `data-access/deleted-plans-store.ts`
  (GP-22 bis GP-26); `ui/restore-plan-dialog.ts` mit `restorePlan` (erst ohne `overwrite`, bei 409 die Rückfrage,
  GP-12 bis GP-14); `ui/purge-plan-dialog.ts` (GP-16, GP-17); `yearly-overview-page.ts` mit Link und „Rückgängig"
  (GP-9, GP-21), die Planansicht übergibt den gelöschten Plan im Navigationszustand; Texte in
  `ui/delete-plan-dialog.ts` (GP-8) und `ui/copy-plan-dialog.ts` (GP-20)
- Tests: `HouseholdPlanTest`, `HouseholdPlanCopyTest`, `DeletedPlansTest`, `HouseholdPlanApplicationServiceTest`,
  `HouseholdPlanControllerTest`, `HouseholdPlanOverviewControllerTest`, `JdbcHouseholdPlanRepositoryTest`,
  `JdbcHouseholdPlanOverviewQueryHandlerTest`, `HouseholdPlanIntegrationTest`, `deleted-plans-page.spec.ts`,
  `restore-plan-dialog.spec.ts`, `purge-plan-dialog.spec.ts`, `delete-plan-dialog.spec.ts`,
  `yearly-overview-page.spec.ts`, `budget-dashboard.spec.ts`, `budget-api.spec.ts`
- E2E: `e2e/tests/geloeschte-plaene.spec.ts`; angepasst `plan-loeschen.spec.ts` und `plan-uebernehmen.spec.ts`

## Offene Fragen

Keine. Entschieden am 29.10.2026: überschriebener Plan beim Wiederherstellen und beim Übernehmen landet unter
„Gelöschte Pläne" (GP-14, GP-19); endgültiges Löschen je Plan mit Rückfrage, keine Frist (GP-5, GP-16); mehrere
gelöschte Pläne je Jahr mit Datum (GP-24); Link unter der Jahresübersicht und eigene Seite (GP-21, GP-22);
„Rückgängig" in der Meldung nach dem Löschen (GP-9).
