# Glossar

Die Fachsprache des Haushaltsbuchs. Code und Kommentare sind englisch, Oberfläche und Dokumentation deutsch. Die
Kategorien der 50/30/20-Regel heißen auch im Code deutsch (`LEBENSUNTERHALT` …), weil sie fachliche Eigennamen sind.
Neue Begriffe kommen hier hinein, bevor sie im Code verwendet werden.

## Haushaltspläne (`apps/backend`)

| Begriff | Im Code | Bedeutung |
|---|---|---|
| Haushaltsplan, Plan | `HouseholdPlan` (Aggregate Root) | Alle Einnahmen und Ausgaben, die ein Benutzer für ein Kalenderjahr plant. Höchstens einer pro Benutzer und Jahr |
| Besitzer | `OwnerId` | Der Benutzer, der den Plan angelegt hat. Nur er sieht und ändert ihn. Im Backend nur über seine ID bekannt |
| Jahr | `java.time.Year` | Das Kalenderjahr, für das ein Plan gilt (1900 bis 2200) |
| Posten, Eintrag | `BudgetItem` (Entity) | Eine Einnahme oder Ausgabe in einem Plan, als Jahresbetrag ohne eigenes Datum |
| Art des Postens | `ItemType` (`INCOME`, `EXPENSE`) | Einnahme oder Ausgabe |
| Einnahme | `ItemType.INCOME` | Posten ohne Kategorie, erhöht das Einkommen |
| Ausgabe | `ItemType.EXPENSE` | Posten mit genau einer Kategorie |
| Beschreibung | `BudgetItemDescription` | Wofür der Posten steht, z. B. „Miete". 1 bis 255 Zeichen, nicht nur Leerzeichen |
| Notiz | `BudgetItemNote` | Optionaler Freitext zum Posten, höchstens 1000 Zeichen; ohne Einfluss auf die Auswertung |
| Unterkategorie | `BudgetItemSubcategory` | Optionaler, frei wählbarer Name zur weiteren Gruppierung von Posten, z. B. „Auto"; 1 bis 50 Zeichen, unabhängig von der Kategorie und ohne Einfluss auf die Auswertung |
| Betrag | `Money` | Geldbetrag in Euro mit höchstens zwei Nachkommastellen; im Posten immer positiv |
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
| Defizit | `expensesExceedIncome` | Die Ausgaben sind höher als das Einkommen |
| Überschuss, nicht verplant | `surplus`, `unplanned` | Positiver Saldo; zählt in der Auswertung zu Sparen & Investieren |
| Finanzielle Lage | `BudgetSummary` (Value Object) | Einkommen, Ausgaben je Kategorie und alles, was daraus folgt (Saldo, Anteile, Zielerreichung) |
| Übersicht | `HouseholdPlanOverview` (Read Model) | Was die Oberfläche für ein Jahr anzeigt: Posten, Summen, Aufteilung je Kategorie |
| Aufteilung je Kategorie | `CategoryBreakdown` | Teil der Übersicht: Summe, Anteil, Zielanteil und Zielerreichung einer Kategorie |
| Protokoll | `audit_log`, `AuditLogListener` | Jedes Domain Event wird nach erfolgreichem Speichern protokolliert |

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
| Anmeldesperre | `LoginThrottle` (Application-Port, kein Aggregate) | Nach 5 fehlgeschlagenen Anmeldungen in 15 Minuten ist die Anmeldung mit dieser Adresse 15 Minuten lang gesperrt |

## Domain Events

| Event | Wann |
|---|---|
| `HouseholdPlanCreated` | Ein Plan wurde angelegt |
| `IncomeAdded` | Eine Einnahme wurde erfasst |
| `ExpenseAdded` | Eine Ausgabe wurde erfasst |
| `BudgetItemRemoved` | Ein Posten wurde gelöscht |
| `BudgetItemChanged` | Beschreibung, Betrag, Kategorie oder Notiz eines Postens wurden geändert |
| `UserRegistered` | Ein Benutzer hat sich registriert |
