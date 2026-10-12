# Projektregeln

## Aggregates, Entities, Value Objects

### Regeln für Aggregates

- Eindeutige Identität: Jede Entity und somit auch jede Aggregate Root muss über eine unveränderliche, typisierte ID verfügen. Der Objektvergleich (equals/hashCode) erfolgt ausschließlich über diese ID

- Konsistenz: Aggregates schützen Invarianten zwingend durch Guard Clauses

- Kapselung: Keine öffentlichen Setter; keine direkte Herausgabe veränderbarer interner Listen (nur defensive Kopien/Unmodifiable)

- Validität: Keine „leeren“ Objekte; Konstruktoren müssen die initiale Gültigkeit erzwingen

- Reinheit: Keine Infrastrukturabhängigkeiten oder Repository-Aufrufe innerhalb des Aggregates

- _Keep Aggregates_ Small: Ein Aggregate ist keine fachliche Sammelmappe, sondern eine minimale Konsistenzgrenze, es umfasst nur die Entities und Value Objects, die zwingend atomar und synchron konsistent sein müssen

- Referenzierung: Andere Aggregates werden niemals als Objektreferenz eingebettet, sondern ausschließlich über ihre typisierte ID referenziert; das erzwingt die Entkopplung und verhindert riesige Objektgraphen

- Tell, Don’t Ask-Prinzip: Trage nach Möglichkeit keine Daten aus dem Aggregate heraus, um extern Entscheidungen zu treffen; sende stattdessen einen Befehl (Command/Methode) an das Aggregate, damit es die Entscheidung intern, basierend auf seinem eigenen Zustand trifft

### Regeln für Value Objects

- Unveränderlichkeit: Value Objects sind nach der Erzeugung nicht mehr veränderbar, jede Änderung erzeugt eine neue Instanz des Objekts

- Gleichheit durch Wert: Zwei Value Objects sind identisch, wenn alle ihre Attribute übereinstimmen (implementiere equals und hashCode basierend auf allen Feldern)

- Selbstvalidierung: Ein Value Object darf niemals in einem ungültigen Zustand existieren; prüfe alle fachlichen Regeln direkt im Konstruktor

- Keine Primitive Obsession: Nutze keine einfachen Datentypen (String, Integer) für fachliche Konzepte; erstelle stattdessen Value Objects, die ihre eigene Logik mitbringen

- Nebenwirkungsfreie Methoden: Methoden in Value Objects dürfen niemals den Zustand verändern, sondern geben immer ein neues Value Object zurück

### Regeln für Entities

- Global eindeutige Identität: Jede Entity besitzt eine eigene, global eindeutige Identität (typisierte ID), der Vergleich von Entities erfolgt ausschließlich über diese ID

- Kapselung durch die Root: Interne Entities dürfen ausschließlich über die Aggregate Root verändert werden; kein externer Dienst darf eine interne Entity direkt manipulieren. Die Entity-Klasse selbst darf public sein (z. B. damit Repositorys sie lesen und rekonstituieren können), ihre verändernden fachlichen Methoden sind aber in der Sichtbarkeit zu reduzieren (package-private), sodass nur die Aggregate Root sie aufrufen kann

- Lebenszyklusabhängigkeit: Die Existenz einer internen Entity ist an die Aggregate Root gebunden; wird die Root gelöscht, verschwinden auch ihre Entities

- Fachliche Methoden: Entities sind keine reinen Datenhüllen; sie enthalten Logik, die ihren eigenen Zustand betrifft, werden aber von der Root orchestriert

- Validierung bei Zustandsübergängen: Auch interne Entities müssen ihre eigenen Invarianten schützen; eine Entity darf durch einen Methodenaufruf niemals in einen fachlich ungültigen Zustand geraten

## Repositorys

### Regeln für Repositorys

- Schnittstellenprinzip: Repositorys werden im Domänen-Layer als Interfaces definiert; die technische Implementierung erfolgt strikt getrennt im Infrastruktur-Layer

- Sammlungsmetapher: Ein Repository verhält sich wie eine fachliche Sammlung (z. B. ein Set)

- Aggregate-Fokus: Repositorys verwalten ausschließlich Aggregate Roots; erstelle niemals Repositorys für interne Entities oder Value Objects.

- Ganzheitlichkeit (Integrität): Ein Repository gibt immer das vollständige Aggregate zurück; gib niemals Fragmente oder Teildaten (Projektionen) heraus

- Keine Infrastrukturlecks: Die Schnittstelle darf keine technischen Details wie JPA-Criteria, SQL-Fragmente enthalten

### Selektion vs. Präsentation

- Erlaubt: Fachliche Filter, um Aggregates für Operationen zu laden (z. B. findeÜberfälligeStornierungen()).

- Vermeiden: Abfragen für Dashboards oder komplexe UI-Tabellen, die Informationen aus mehreren Aggregates umfassen; nutze hierfür Read Models.

## Domain Commands

### Regeln für Domain Commands

- Strikte Logiktrennung: Ein Domain Command darf keinerlei Businesslogik enthalten. Seine Verantwortung beschränkt sich ausschließlich auf die formale (syntaktische) Validierung.

- Erlaubt (Validierung): Prüfung auf null, leere Strings, korrekte Formate (Regex), Wertebereiche (z. B. „Alter muss eine positive Zahl sein“).

- Verboten (Businesslogik): fachliche Entscheidungen, Berechnungen oder Prüfungen gegen den Systemzustand (z. B. „Ist das Zimmer frei?“ oder „Darf dieser Gast einchecken?“). Diese Logik gehört zwingend in Aggregates oder Domain Services.

- Fachliche Absicht (Intent): Domain Commands sind nach der fachlichen Handlung benannt (CheckeGastEin), nicht nach technischen Datenänderungen (UpdateZimmerRequest).

- Unveränderlichkeit: Domain Commands müssen strikt unveränderlich sein.

- Eindeutigkeit: Ein Domain Command repräsentiert genau eine fachliche Absicht und führt zu genau einer Transaktion.

- Teil der Domäne: Domain Commands sind Teil der Domäne, sie können außerhalb bspw. aus einem Application Service erstellt werden. Die Commands werden nicht direkt auf Aggregates angewandt, sondern die fachlichen Daten aus dem Command entnommen und auf die Fachmethode im Aggregate angewandt.

## Application Services und Domain Services

### Regeln für Application Services

- Koordination statt Logik: Ein Application Service delegiert die Fachlogik an Aggregates oder Domain Services; er enthält nur „Wenn-Dann“-Abläufe für den Prozess (z. B. „Lade Buchung, führe Check-In aus, speichere Buchung“)

- Transaktionsgrenze: Hier beginnt und endet die Datenbanktransaktion; der Application Service stellt sicher, dass eine Operation atomar ist

- Infrastruktur-Brücke: Koordiniert technische Belange; Versand von Bestätigungs-E-Mails, Aufruf externer APIs oder Veröffentlichen von Events

- Eingabe/Ausgabe: Nimmt Commands oder einfache Datentypen (DTOs) entgegen und gibt idealerweise Read Models oder einfache Bestätigungen zurück; er schirmt die Domäne vor der Außenwelt ab

- Sicherheit und Zugriff: Authentifizierung und Autorisierung („Darf dieser Nutzer diesen Command ausführen?“) werden typischerweise hier oder in vorgeschalteten Interceptors geprüft

### Regeln für Domain Services

- Zustandslosigkeit: Domain Services sind zustandslos, sie halten keine Daten, sondern führen Berechnungen oder Operationen auf übergebenen Aggregates aus

- Fachliche Sprache: Die Methodennamen spiegeln die Ubiquitous Language wider (z. B. ZimmerZuweisungsService.findeBestesZimmer())

- Keine Infrastruktur: Ein Domain Service darf keine technischen Details (E-Mail-Versand, Datenbanktransaktionen) kennen; er bleibt im Domain Layer

- Logikfokus: Nutze Domain Services nur, wenn die Logik wirklich nicht in ein Aggregate passt. Das Ziel bleibt ein Rich Domain Model, kein prozeduraler Code.

## Domain Events

### Regeln für Domain Events

- Benennung: Domain Events dokumentieren fachlich relevante Fakten der Vergangenheit und werden konsequent in der Vergangenheitsform benannt (z. B. GastAusgecheckt).

- Inhalt: Ein Domain Event sollte nur die Identitäten (IDs) und die minimal notwendigen Daten enthalten, um den Empfängern die Arbeit zu ermöglichen

- Unveränderlichkeit: Domain Events müssen strikt unveränderlich sein

## Read Model und Query Handler
### Regeln für Read Model und Query Handler

- Immutability: Read Models müssen strikt unveränderlich sein; es gibt keine öffentlichen Setter

- Clientzentrierung: Die Struktur eines Read Models richtet sich ausschließlich nach den Bedürfnissen des Konsumenten (z. B. UI oder API-Client); sie darf flach sein, kann aber auch hierarchisch oder verschachtelt aufgebaut sein, wenn das die Verarbeitung im Client vereinfacht

- Grenzüberschreitung: Im Gegensatz zu Aggregates dürfen Read Models Daten aus verschiedenen Aggregates kombinieren (Denormalisierung), um Joins auf Client-Seite zu vermeiden

- Bereitstellung via Query-Handler: Read Models werden niemals über Repositorys bezogen, sondern über dedizierte Query-Handler

- Wiederverwendung von Value Objects oder typisierten IDs: Es ist ausdrücklich erlaubt und erwünscht, Value Objects oder typisierte IDs aus der Domäne im Read Model wiederzuverwenden; das stellt sicher, dass fachliche Formatierungen und einfache Berechnungen (z. B. Geldwert.toString() oder Zeitraum.dauerInTagen()) konsistent bleiben

- Keine neue Domänenlogik: Ein Read Model darf existierende Logik von Value Objects nutzen, aber keine eigene neue Geschäftslogik oder Validierungen einführen; es bleibt ein reiner Informationsträger.

- Performancefokus: Query-Handler dürfen den „schweren“ Weg über das Domänenmodell umgehen und optimierte Abfragen (z. B. natives SQL oder spezialisierte Views) nutzen