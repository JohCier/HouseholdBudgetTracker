# Glossar

Die Fachsprache des Haushaltsbuchs. Code und Kommentare sind englisch, Oberfläche und Dokumentation deutsch. Die
Kategorien der 50/30/20-Regel heißen auch im Code deutsch (`LEBENSUNTERHALT` …), weil sie fachliche Eigennamen sind.
Neue Begriffe kommen hier hinein, bevor sie im Code verwendet werden.

## Haushaltspläne (`apps/backend`)

| Begriff | Im Code | Bedeutung |
|---|---|---|
| Haushaltsplan, Plan | `HouseholdPlan` (Aggregate Root) | Alle Einnahmen und Ausgaben, die ein Benutzer für ein Kalenderjahr plant. Höchstens ein aktiver pro Benutzer und Jahr |
| Besitzer | `OwnerId` | Der Benutzer, der den Plan angelegt hat. Nur er sieht und ändert ihn. Im Backend nur über seine ID bekannt |
| Jahr | `java.time.Year` | Das Kalenderjahr, für das ein Plan gilt (1900 bis 2200) |
| Als Vorlage übernehmen | `CopyHouseholdPlan` (Command) | Alle Posten eines Plans mit ihrer Reihenfolge in ein anderes Jahr kopieren; legt dort einen Plan an, ein vorhandener wird nach Bestätigung gelöscht und ersetzt; die Quelle bleibt unverändert |
| Plan löschen | `DeleteHouseholdPlan` (Command) | Einen Plan mit allen Posten nach Rückfrage unter „Gelöschte Pläne" ablegen; das Jahr hat danach keinen aktiven Plan mehr |
| Aktiver Plan | `household_plan.deleted_at IS NULL` | Ein Plan, der nicht gelöscht ist; nur aktive Pläne erscheinen in Jahresübersicht und Planansicht |
| Gelöschter Plan, Gelöschte Pläne | `DeletedPlans`, `DeletedPlanEntry` (Read Model) | Ein gelöschter Plan behält seine Posten und weiß, wann er gelöscht wurde; die Seite „Gelöschte Pläne" listet sie |
| Wiederherstellen | `RestoreHouseholdPlan` (Command) | Einen gelöschten Plan wieder aktiv machen; ein aktiver Plan desselben Jahres wird nach Bestätigung abgelöst und selbst gelöscht |
| Endgültig löschen | `PurgeHouseholdPlan` (Command) | Einen gelöschten Plan mit allen Posten nach Rückfrage für immer entfernen |
| Quelle | `sourcePlanId` | Der Plan, der als Vorlage übernommen wird |
| Zieljahr | `targetYear` | Das Jahr, in das ein Plan übernommen wird; vorgeschlagen ist das Folgejahr, das Jahr direkt nach der Quelle |
| Posten, Eintrag | `BudgetItem` (Entity) | Eine Einnahme oder Ausgabe in einem Plan, ohne eigenes Datum, als Monats- oder Jahresbetrag |
| Art des Postens | `ItemType` (`INCOME`, `EXPENSE`) | Einnahme oder Ausgabe |
| Einnahme | `ItemType.INCOME` | Posten ohne Kategorie, erhöht das Einkommen |
| Ausgabe | `ItemType.EXPENSE` | Posten mit genau einer Kategorie |
| Beschreibung | `BudgetItemDescription` | Wofür der Posten steht, z. B. „Miete". 1 bis 255 Zeichen, nicht nur Leerzeichen |
| Notiz | `BudgetItemNote` | Optionaler Freitext zum Posten, höchstens 1000 Zeichen; ohne Einfluss auf die Auswertung |
| Markierungen | `BudgetItem.isImportant()`, `BudgetItem.savingPotential()` | Wie der Benutzer einen Posten in den Einträgen markiert hat: wichtig und, bei Ausgaben, Sparpotential; ohne Einfluss auf die Auswertung |
| Wichtig | `BudgetItem.isImportant()`, `BudgetItemMarked.Mark.IMPORTANT` | Markierung eines Postens, den der Benutzer hervorgehoben sehen will; die Zeile ist farblich abgesetzt |
| Sparpotential | `SavingPotential`, `BudgetItemMarked.Mark.SAVING_POTENTIAL` | Der Teil einer Ausgabe, den der Benutzer für einsparbar hält; immer ein Monatsbetrag, höchstens der Monatsbetrag der Ausgabe. In der Bilanz als Summe je Monat und Jahr (`monthlySavingPotential`, `savingPotential`) |
| Sparpotential je Unterkategorie | `SubcategorySavingPotential`, `savingPotentialsBySubcategory`, `app-saving-potential-summary` | Summe der Sparpotentiale aller Ausgaben einer Unterkategorie über alle Kategorien, je Monat und Jahr; Kasten links neben der Bilanz. Je Gruppe als `OverviewItemGroup.monthlySavingPotential` in der Summenzeile der Einträge |
| Top 5 | `TopItem`, `topSavingPotentials`, `topExpenses`, `app-top-items` | Die bis zu fünf Ausgaben mit dem höchsten Sparpotential (alle Kategorien) bzw. dem höchsten Monatsbetrag (ohne Sparen & Investieren), nach Monatsbetrag geordnet; Karte unter der 50/30/20-Regel |
| Unterkategorie | `BudgetItemSubcategory` | Optionaler, frei wählbarer Name zur weiteren Gruppierung von Posten, z. B. „Auto"; 1 bis 50 Zeichen, unabhängig von der Kategorie und ohne Einfluss auf die Auswertung |
| Sonstiges | `BudgetItemSubcategory.OTHER` | Gruppe der Posten ohne Unterkategorie oder mit der Unterkategorie „Sonstiges"; steht am Ende, solange der Benutzer sie nicht verschiebt |
| Bereich | `ItemSection` | Die Einnahmen oder eine Kategorie der Ausgaben; darin werden die Posten nach Unterkategorie gruppiert |
| Gruppe | `OverviewItemGroup` | Die Posten eines Bereichs mit derselben Unterkategorie, mit ihrer Summe |
| Summenzeile | `.summary` (Oberfläche) | Zeile „Summe ‹…›" unter den Posten einer Gruppe, einer Kategorie, der Einnahmen oder der Ausgaben, je Monat und Jahr |
| Posten in einer Gruppe erfassen | `NewItemPreset`, `AddItemDialog` | Einen Posten aus einer Gruppe heraus anlegen; Art, Kategorie und Unterkategorie sind mit denen der Gruppe ausgefüllt |
| Reihenfolge der Gruppen | `SubcategoryOrder` | Wie der Benutzer die Gruppen eines Bereichs angeordnet hat; je Plan gespeichert |
| Betrag | `Money` | Geldbetrag in Euro mit höchstens zwei Nachkommastellen; im Posten immer positiv |
| Betrag eines Postens | `ItemAmount` | Der Betrag, wie der Benutzer ihn eingegeben hat, mit seiner Angabe |
| Angabe (monatlich, jährlich) | `AmountPeriod` (`MONTHLY`, `YEARLY`) | Ob der eingegebene Betrag ein Monats- oder ein Jahresbetrag ist |
| Jahresbetrag | `annual()`, `annualAmount` | Betrag eines Postens für das ganze Jahr: bei monatlicher Angabe der Betrag mal 12; Grundlage aller Summen und der Auswertung |
| Monatsbetrag | `monthly()`, `monthlyAmount` | Betrag eines Postens je Monat: bei jährlicher Angabe der Betrag durch 12, auf den Cent gerundet; nur zur Anzeige |
| Monatssumme | `monthlyTotal…`, `monthlyBalance` | Eine Jahressumme der Übersicht durch 12, auf den Cent gerundet |
| Kategorie | `ExpenseCategory` | Einer der drei Töpfe der 50/30/20-Regel |
| Lebensunterhalt | `LEBENSUNTERHALT` | Miete, Lebensmittel, Versicherungen; Ziel: höchstens 50 % des Einkommens |
| Freizeit & Spaß | `FREIZEIT_SPASS` | Hobbys, Ausgehen, Urlaub; Ziel: höchstens 30 % |
| Sparen & Investieren | `SPAREN_INVESTIEREN` | Rücklagen, ETFs, Altersvorsorge; Ziel: mindestens 20 % |
| Zielanteil | `targetPercentageOfIncome` | Der Prozentsatz der Kategorie (50, 30, 20) |
| Zielrichtung | `TargetDirection` (`AT_MOST`, `AT_LEAST`) | Ob der Zielanteil eine Obergrenze (höchstens) oder Untergrenze (mindestens) ist |
| Ziel erreicht | `targetMet` | Ob der Anteil der Ausgaben einer Kategorie am Einkommen ihren Zielanteil einhält |
| Anteil am Einkommen | `Percentage`, `shareOfIncome` | Ausgaben einer Kategorie geteilt durch Einkommen, in Prozent mit zwei Nachkommastellen |
| Einkommen | `totalIncome` | Summe aller Einnahmen eines Plans |
| Ausgaben | `totalExpense` | Summe aller Ausgaben eines Plans |
| Saldo | `balance` | Einkommen minus Ausgaben; negativ bei Defizit |
| Bilanz | `app-overview-summary`, nur im Frontend | Kasten oben in der Planansicht: Gesamteinnahmen, Gesamtausgaben, Saldo und Sparpotential je Monat und je Jahr |
| Defizit | `expensesExceedIncome` | Die Ausgaben sind höher als das Einkommen |
| Überschuss, nicht verplant | `surplus`, `unplanned` | Positiver Saldo; zählt in der Auswertung zu Freizeit & Spaß |
| Finanzielle Lage | `BudgetSummary` (Value Object) | Einkommen, Ausgaben je Kategorie und alles, was daraus folgt (Saldo, Anteile, Zielerreichung) |
| Übersicht | `HouseholdPlanOverview` (Read Model) | Was die Oberfläche für ein Jahr anzeigt: Posten, Summen, Aufteilung je Kategorie |
| Jahresübersicht | `YearlyOverview` (Read Model), `YearlyOverviewEntry` | Alle Pläne eines Benutzers mit Einnahmen, Ausgaben, Überschuss und Veränderung zum Vorjahr; Startseite nach der Anmeldung |
| Vorjahr | `YearlyOverviewEntry.of(…, previousYear)` | Der Plan des unmittelbar vorhergehenden Kalenderjahres, falls es ihn gibt |
| Veränderung zum Vorjahr | `Percentage.changeFrom`, `incomeChange`, `expenseChange`, `balanceChange` | (Wert − Wert des Vorjahres) ÷ \|Wert des Vorjahres\| × 100, in Prozent mit zwei Nachkommastellen; fehlt ohne Vorjahr oder bei 0,00 € im Vorjahr |
| Aufteilung je Kategorie | `CategoryBreakdown` | Teil der Übersicht: Summe, Anteil, Zielanteil und Zielerreichung einer Kategorie |
| Einmalposten | `OneTimeItem` (Aggregate Root) | Eine einzelne Einnahme oder Ausgabe mit Datum, z. B. eine Nachzahlung; gehört dem Benutzer, nicht zu einem Plan, und zählt weder zum Haushaltsplan noch zur 50/30/20-Regel |
| Datum eines Einmalpostens | `OneTimeItem.date` | Der Tag des Einmalpostens (01.01.1900 bis 31.12.2200); sein Jahr bestimmt, zu welchem Jahr der Einmalposten gehört. Nur Einmalposten haben ein Datum |
| Einmalposten eines Jahres | `OneTimeItemOverview` (Read Model) | Alle Einmalposten eines Benutzers in einem Jahr in der vom Benutzer festgelegten Reihenfolge, mit einmaligen Einnahmen, Ausgaben und Saldo |
| Platz eines Einmalpostens | `OneTimeItemRank` | Wo ein Einmalposten unter den einmaligen Einnahmen bzw. Ausgaben seines Jahres steht; ein neuer steht unten, Verschieben ändert nur den verschobenen |
| Einmalige Einnahmen, Ausgaben, Saldo | `oneTimeIncome`, `oneTimeExpense`, `oneTimeBalance` | Summen der Einmalposten eines Jahres; immer Jahreswerte, in der Jahresübersicht in eigenen Spalten „Einmalig (Jahr)"
| Tabs der Planansicht | `PLAN_TABS` (`items`, `oneTime`, `details`, `levers`), nur im Frontend | Die Bereiche unter Bilanz und 50/30/20-Regel: „Regelmäßige Posten" (die Einträge), „Einmalposten", „Detailansicht" (die Torte) und „Sparhebel" |
| Detailansicht | Tab `details`, `app-category-pie`, nur im Frontend | Der Tab mit der Torte, ihrem Ausschnitt und ihrer Sortierung |
| Sparhebel | Tab `levers`, nur im Frontend | Der Tab mit „Sparpotential je Unterkategorie", „Top 5 Sparpotential" und „Top 5 Ausgaben" (`TopItemsList`) |
| Posten erfassen | `AddItemDialog` ohne `NewItemPreset`, nur im Frontend | Der Dialog über den Einträgen, der einen neuen Posten anlegt; ersetzt das Formular „Neuer Eintrag" |
| Ausschnitt der Torte | `PieSelection` (`rule`, `category`, `group`), nur im Frontend | Was die Torte zeigt: die 50/30/20-Regel, eine Kategorie aufgeteilt nach Unterkategorien oder eine Unterkategorie einer Kategorie aufgeteilt nach Posten |
| Sortierung der Torte | `PieSort` (`entries`, `amount`, `name`, `descending`), nur im Frontend | Reihenfolge der Stücke einer Kategorie oder Unterkategorie in Torte und Legende: wie die Einträge, nach Betrag je Monat oder nach Name |
| Anteil am Ausschnitt | `percentageOfSection`, `percentageOfGroup`, `unplannedPercentageOfTotal` | Anteil einer Gruppe an ihrer Kategorie bzw. eines Postens an seiner Gruppe, aus den Monatsbeträgen; bei Sparen & Investieren einschließlich des Überschusses |
| Protokoll | `audit_log`, `AuditLogListener` | Jedes Domain Event wird nach erfolgreichem Speichern protokolliert |

## Vermögen und Schulden (`apps/backend`)

| Begriff | Im Code | Bedeutung |
|---|---|---|
| Vermögensstand | `NetWorthStatement` (Aggregate Root) | Alle Vermögenswerte und Schulden eines Benutzers am Ende eines Jahres; höchstens einer pro Benutzer und Jahr, besteht, solange er Werte hat; unabhängig vom Haushaltsplan |
| Wert | `NetWorthEntry` (Entity) | Ein Vermögenswert oder eine Schuld mit Bezeichnung und Betrag |
| Vermögenswert | `NetWorthEntryType.ASSET` | Was dem Benutzer gehört, z. B. „Girokonto"; erhöht das Nettovermögen |
| Schuld | `NetWorthEntryType.LIABILITY` | Was der Benutzer schuldet, z. B. „Autokredit"; mit positivem Betrag erfasst, verringert das Nettovermögen |
| Bezeichnung | `NetWorthEntryName` | Wofür der Wert steht; 1 bis 255 Zeichen, nicht nur Leerzeichen |
| Vermögen, Schulden | `totalAssets`, `totalLiabilities` | Summe der Vermögenswerte bzw. der Schulden eines Jahres |
| Nettovermögen | `netWorth`, `NetWorthTotals` | Vermögen minus Schulden; negativ heißt überschuldet (`overindebted`) |
| Veränderung des Nettovermögens | `NetWorthChange` | Unterschied zum Nettovermögen des unmittelbar vorhergehenden Jahres in Euro und in Prozent wie `Percentage.changeFrom` |
| Verlauf | `NetWorthHistory` (Read Model), `app-net-worth-chart` | Alle Jahre mit Vermögensstand mit Nettovermögen und Veränderung; Karte „Vermögen" der Jahresübersicht mit Verlaufsdiagramm |

## Benutzer (`apps/users`)

| Begriff | Im Code | Bedeutung |
|---|---|---|
| Benutzer | `User` (Aggregate Root) | Eine Person, die sich anmelden kann |
| E-Mail-Adresse | `EmailAddress` | Anmeldename; kleingeschrieben gespeichert und eindeutig |
| Passwort | `Password` | Im Klartext nur bei Eingabe; 8 Zeichen bis 72 Bytes; wird nie gespeichert |
| Passwort-Hash | `PasswordHash` | Das gespeicherte BCrypt-Ergebnis des Passworts |
| Registrieren | `User.register`, `/api/auth/register` | Benutzer anlegen und direkt anmelden |
| Anmelden | `authenticate`, `/api/auth/login` | Prüfen, ob E-Mail-Adresse und Passwort zusammenpassen, und eine Sitzung starten |
| Sitzung | Cookie `HBT_SESSION` mit JWT | Weist den Benutzer gegenüber beiden Diensten aus; 8 Stunden gültig, nicht serverseitig gespeichert |
| Anmeldesperre | `LoginThrottle` (Application-Port, kein Aggregate) | Nach 5 fehlgeschlagenen Anmeldungen in 15 Minuten ist die Anmeldung mit dieser Adresse 15 Minuten lang gesperrt; falsche aktuelle Passwörter beim Passwortändern zählen mit |
| Passwort ändern | `User.changePassword`, `ChangePasswordCommand`, `/api/auth/password` | Ein angemeldeter Benutzer ersetzt sein Passwort; dafür gibt er das aktuelle an, das neue muss sich davon unterscheiden |

## Domain Events

| Event | Wann |
|---|---|
| `HouseholdPlanCreated` | Ein Plan wurde angelegt |
| `HouseholdPlanCopied` | Ein Plan wurde als Vorlage in ein anderes Jahr übernommen, ggf. unter Überschreiben des dortigen Plans |
| `HouseholdPlanDeleted` | Ein Plan wurde mit allen Posten unter „Gelöschte Pläne" abgelegt, auch beim Ablösen durch Wiederherstellen oder Übernehmen |
| `HouseholdPlanRestored` | Ein gelöschter Plan wurde wiederhergestellt |
| `HouseholdPlanPurged` | Ein gelöschter Plan wurde mit allen Posten endgültig gelöscht |
| `IncomeAdded` | Eine Einnahme wurde erfasst |
| `ExpenseAdded` | Eine Ausgabe wurde erfasst |
| `BudgetItemRemoved` | Ein Posten wurde gelöscht |
| `BudgetItemChanged` | Beschreibung, Betrag, Angabe, Kategorie, Unterkategorie oder Notiz eines Postens wurden geändert |
| `BudgetItemMarked` | Ein Posten wurde als wichtig markiert oder bekam ein Sparpotential, oder die Markierung wurde entfernt |
| `BudgetItemsReordered` | Die Posten einer Unterkategorie-Gruppe wurden umsortiert |
| `SubcategoriesReordered` | Die Unterkategorie-Gruppen der Einnahmen oder einer Kategorie wurden umsortiert |
| `NetWorthStatementCreated` | Für ein Jahr wurde der erste Vermögenswert oder die erste Schuld erfasst |
| `NetWorthEntryAdded` | Ein Vermögenswert oder eine Schuld wurde erfasst |
| `NetWorthEntryChanged` | Art, Bezeichnung oder Betrag eines Werts wurden geändert |
| `NetWorthEntryRemoved` | Ein Wert wurde gelöscht |
| `NetWorthStatementCopied` | Der Vermögensstand eines Jahres wurde in ein anderes übernommen, ggf. unter Überschreiben |
| `UserRegistered` | Ein Benutzer hat sich registriert |
| `PasswordChanged` | Ein Benutzer hat sein Passwort geändert |
