---
paths:
  - "apps/backend/src/**/*.java"
  - "apps/users/src/**/*.java"
---

# DDD-Regeln für die Java-Apps

Der Abschnitt „Konventionen in diesem Projekt“ beschreibt, wie die Bausteine hier konkret gebaut werden; die Abschnitte
danach die allgemeinen DDD-Regeln dahinter. Die mit „(ArchUnit)“ markierten Regeln prüft `ArchitectureTest` (ArchUnit,
jMolecules) in beiden Apps maschinell. Schlägt `ArchitectureTest` fehl, wird der Code angepasst, nicht der Test; eine
Änderung am Test nur nach Rückfrage.

## Konventionen in diesem Projekt

### Annotationen und Pakete

- Annotationen sind Pflicht: Jeder Baustein trägt seine jMolecules-Annotation (`@AggregateRoot`, `@Entity`,
  `@ValueObject`, `@Identity` am ID-Feld, `@Command`, `@QueryModel`) bzw. die eigene `@QueryHandler`-Annotation;
  Domain Events implementieren `TimestampedDomainEvent`. Ohne Annotation greift keine ArchUnit-Prüfung, Verstöße fallen
  dann nicht auf

- Pakete (Beispiel Backend, `users` entsprechend unter `de.jcss.hbt.users`):
  - `domain/<aggregat>/`: Aggregate Root, Entities, Value Objects, typisierte IDs, Repository-Interface, Domain-Ports
  - `domain/<aggregat>/command/`: Domain Commands (ArchUnit)
  - `domain/<aggregat>/event/`: Domain Events
  - `domain/<aggregat>/overview/`: Read Models und Query-Handler-Interfaces (ArchUnit)
  - `domain/exception/`: fachliche Exceptions
  - `application/<aggregat>/`: Application Services und Ports für technische Mechanismen
  - `infrastructure/persistence|web|security|…`: Repository- und Query-Handler-Implementierungen, Controller, DTOs

- Schichten: Die Ringe sind per `package-info.java` als jMolecules-Onion-Ringe deklariert; Abhängigkeiten zeigen nur
  nach innen (infrastructure → application → domain) (ArchUnit)

- Frameworkfreie Domäne: Klassen unter `domain` hängen nur von `domain`, `java..` und `org.jmolecules..` ab, also kein
  Spring, kein JDBC, kein Jackson (ArchUnit)

### Bausteine

- Typisierte IDs: Records mit einer `UUID` und einer statischen Fabrik `generate()` (z. B. `HouseholdPlanId`,
  `BudgetItemId`, `UserId`)

- Value Objects sind Records oder Enums (ArchUnit); fachliche Regeln stehen im kompakten Konstruktor, enthaltene
  Collections werden mit `List.copyOf`/`Map.copyOf` kopiert

- Aggregates und Entities sind finale Klassen; ihre Felder sind final (ArchUnit), equals/hashCode
  vergleichen nur die ID (ArchUnit)

- Erzeugen und Rekonstituieren: Der Konstruktor ist privat und prüft alle Invarianten. Eine fachlich benannte Fabrik
  (`create`, `register`) erzeugt ein neues Aggregate und zeichnet ein Event auf; `reconstitute` baut ein gespeichertes
  Aggregate im Repository wieder auf, prüft ebenfalls, zeichnet aber keine Events auf. Fabriken interner Entities, die
  nur die Root benutzt, sind package-private (z. B. `BudgetItem.income/expense`)

- Domain Events veröffentlichen: Das Aggregate sammelt seine Events intern; der Application Service holt sie nach
  `add`/`save` über `pullDomainEvents()` ab und veröffentlicht sie über Springs `ApplicationEventPublisher`. Events
  sind Records (ArchUnit) und tragen einen `Instant timestamp` (ArchUnit)

- Nur fachliche Konzepte sind Aggregates: Eine Aggregate Root modelliert ein Konzept, das Benutzer kennen und dessen
  Regeln die Specs beschreiben (`HouseholdPlan`, `User`). Technische Schutz- und Hilfsmechanismen wie Anmeldesperre,
  Rate-Limits, Caches oder Sitzungen sind keine Aggregates, Entities oder Value Objects und bekommen weder Repository
  noch Tabelle. Sie werden als Interface im Application Layer deklariert und in der Infrastruktur umgesetzt, im
  einfachsten Fall im Speicher (z. B. `LoginThrottle`, umgesetzt von `InMemoryLoginThrottle`); ihre Exceptions liegen
  beim Interface (`LoginLockedException`). Eine eigene
  Datenbanktransaktion oder Persistenz nur, wenn die Spec verlangt, dass der Zustand einen Neustart übersteht

- Domain-Ports: Technische Fähigkeiten, die die Domäne braucht, werden als Interface in der Domäne deklariert und in der
  Infrastruktur implementiert (z. B. `PasswordHasher`, umgesetzt von `BCryptPasswordHasher`). Ein Port darf einer
  Methode des Aggregates übergeben werden; das ist keine Infrastrukturabhängigkeit, weil das Aggregate nur das
  Interface kennt

- Fachliche Exceptions: Eigene Exceptions in `domain/exception/`; Verletzungen der Regeln von Value Objects und Entities
  werfen `DomainValidationException` mit einer Meldung, die die verletzte Regel nennt. Eine eigene Exception-Klasse
  gibt es nur, wenn sie einen eigenen HTTP-Status bzw. eine eigene `type`-Kennung bekommt oder gezielt gefangen wird
  (z. B. `…NotFoundException`, `InvalidCredentialsException`). Die Übersetzung in HTTP-Status geschieht ausschließlich
  im `GlobalExceptionHandler`

### Abläufe

- Ein Aggregate pro Transaktion: Ein Command ändert genau ein Aggregate. Regeln über mehrere Aggregates (z. B. „höchstens
  ein Plan pro Benutzer und Jahr“, „eine E-Mail-Adresse gehört höchstens einem Benutzer“) prüft der Application
  Service vorab, um früh einen klaren Fehler zu liefern; die eigentliche Absicherung gegen gleichzeitige Anfragen ist
  ein Unique Constraint hinter `Repository.add`

- Autorisierung über Besitz: Der Application Service lädt das Aggregate und fragt das Aggregate, ob es dem Benutzer
  gehört (`HouseholdPlan.isOwnedBy`). Ein fremdes Aggregate wird genau wie ein nicht vorhandenes behandelt
  (`…NotFoundException`), damit seine Existenz nicht verraten wird

- Getrennte Lese- und Schreibseite: Read Models kennen weder Aggregates noch Entities noch Repositorys, und die
  Schreibseite kennt keine Read Models (ArchUnit). Query-Handler-Interfaces werden nur vom Application Layer benutzt und
  in `infrastructure.persistence` implementiert (ArchUnit)

## Aggregates, Entities, Value Objects

### Regeln für Aggregates

- Eindeutige Identität: Jede Entity und somit auch jede Aggregate Root muss über eine unveränderliche, typisierte ID verfügen. Der Objektvergleich (equals/hashCode) erfolgt ausschließlich über diese ID

- Konsistenz: Aggregates schützen Invarianten zwingend durch Guard Clauses

- Kapselung: Keine öffentlichen Setter (ArchUnit); keine direkte Herausgabe veränderbarer interner Listen (nur defensive Kopien/Unmodifiable)

- Validität: Keine „leeren“ Objekte; Konstruktoren müssen die initiale Gültigkeit erzwingen

- Reinheit: Keine Infrastrukturabhängigkeiten oder Repository-Aufrufe innerhalb des Aggregates; Domain-Ports als Methodenparameter sind erlaubt (siehe oben)

- _Keep Aggregates_ Small: Ein Aggregate ist keine fachliche Sammelmappe, sondern eine minimale Konsistenzgrenze, es umfasst nur die Entities und Value Objects, die zwingend atomar und synchron konsistent sein müssen

- Referenzierung: Andere Aggregates werden niemals als Objektreferenz eingebettet, sondern ausschließlich über ihre typisierte ID referenziert (ArchUnit); das erzwingt die Entkopplung und verhindert riesige Objektgraphen

- Tell, Don’t Ask-Prinzip: Trage nach Möglichkeit keine Daten aus dem Aggregate heraus, um extern Entscheidungen zu treffen; sende stattdessen einen Befehl (Command/Methode) an das Aggregate, damit es die Entscheidung intern, basierend auf seinem eigenen Zustand trifft (z. B. `plan.isOwnedBy(owner)` statt `plan.owner().equals(owner)` im Service)

### Regeln für Value Objects

- Unveränderlichkeit: Value Objects sind nach der Erzeugung nicht mehr veränderbar, jede Änderung erzeugt eine neue Instanz des Objekts

- Gleichheit durch Wert: Zwei Value Objects sind gleich, wenn alle ihre Attribute übereinstimmen; als Records bekommen sie equals und hashCode automatisch, beides wird nicht von Hand geschrieben

- Selbstvalidierung: Ein Value Object darf niemals in einem ungültigen Zustand existieren; prüfe alle fachlichen Regeln direkt im Konstruktor

- Keine Primitive Obsession: Nutze keine einfachen Datentypen (String, Integer) für fachliche Konzepte; erstelle stattdessen Value Objects, die ihre eigene Logik mitbringen (z. B. `Money`, `Percentage`, `BudgetItemDescription`). Nicht jedes Datenfeld in einem Aggregate muss ein Value Object sein, nur dort wo sich auch fachliche Regeln mit verbinden lassen, unnötigen Overhead gilt es hier zu vermeiden

- Nebenwirkungsfreie Methoden: Methoden in Value Objects dürfen niemals den Zustand verändern, sondern geben immer ein neues Value Object zurück (z. B. `BudgetSummary.withExpense(...)`)

### Regeln für Entities

- Identität: Jede Entity besitzt eine eigene typisierte ID, der Vergleich von Entities erfolgt ausschließlich über diese ID

- Kapselung durch die Root: Interne Entities dürfen ausschließlich über die Aggregate Root verändert werden; kein externer Dienst darf eine interne Entity direkt manipulieren. Die Entity-Klasse selbst darf public sein (z. B. damit Repositorys sie lesen und rekonstituieren können), ihre verändernden fachlichen Methoden sind aber in der Sichtbarkeit zu reduzieren (package-private), sodass nur die Aggregate Root sie aufrufen kann

- Lebenszyklusabhängigkeit: Die Existenz einer internen Entity ist an die Aggregate Root gebunden; wird die Root gelöscht, verschwinden auch ihre Entities

- Fachliche Methoden: Entities sind keine reinen Datenhüllen; sie enthalten Logik, die ihren eigenen Zustand betrifft, werden aber von der Root orchestriert

- Validierung bei Zustandsübergängen: Auch interne Entities müssen ihre eigenen Invarianten schützen; eine Entity darf durch einen Methodenaufruf niemals in einen fachlich ungültigen Zustand geraten

## Repositorys

### Regeln für Repositorys

- Schnittstellenprinzip: Repositorys werden im Domänen-Layer als Interfaces definiert; die technische Implementierung erfolgt strikt getrennt im Infrastruktur-Layer

- Sammlungsmetapher: Ein Repository verhält sich wie eine fachliche Sammlung: `add` nimmt ein neues Aggregate auf, `findById` holt eines heraus, `save` übernimmt die Änderungen an einem geholten Aggregate. `save` wird nach jeder Änderung explizit aufgerufen

- Aggregate-Fokus: Repositorys verwalten ausschließlich Aggregate Roots; erstelle niemals Repositorys für interne Entities oder Value Objects.

- Ganzheitlichkeit (Integrität): Ein Repository gibt immer das vollständige Aggregate zurück; gib niemals Fragmente oder Teildaten (Projektionen) heraus

- Keine Infrastrukturlecks: Die Schnittstelle darf keine technischen Details wie SQL-Fragmente oder JDBC-Typen enthalten

### Selektion vs. Präsentation

- Erlaubt: Fachliche Filter und Prüfungen, um Aggregates für Operationen zu laden oder einen Ablauf zu steuern (z. B. `findByEmail(email)`, `existsFor(owner, year)`).

- Vermeiden: Abfragen für Dashboards oder komplexe UI-Tabellen, die Informationen aus mehreren Aggregates umfassen; nutze hierfür Read Models.

## Domain Commands

### Regeln für Domain Commands

- Strikte Logiktrennung: Ein Domain Command darf keinerlei Businesslogik enthalten. Seine Verantwortung beschränkt sich ausschließlich auf die formale (syntaktische) Validierung.

- Erlaubt (Validierung): Prüfung auf null, leere Strings, korrekte Formate (Regex), Wertebereiche. Meist genügen `Objects.requireNonNull`, weil die Felder schon Value Objects sind, die sich selbst validieren.

- Verboten (Businesslogik): fachliche Entscheidungen, Berechnungen oder Prüfungen gegen den Systemzustand (z. B. „Hat der Benutzer für das Jahr schon einen Plan?“ oder „Gehört der Plan diesem Benutzer?“). Diese Logik gehört zwingend in Aggregates, Domain Services oder den Ablauf des Application Service.

- Fachliche Absicht (Intent): Domain Commands sind nach der fachlichen Handlung benannt (`AddExpenseCommand`, `RegisterUserCommand`), nicht nach technischen Datenänderungen (`UpdatePlanRequest`).

- Unveränderlichkeit: Domain Commands sind Records (ArchUnit).

- Eindeutigkeit: Ein Domain Command repräsentiert genau eine fachliche Absicht und führt zu genau einer Transaktion, die genau ein Aggregate ändert.

- Teil der Domäne: Domain Commands sind Teil der Domäne (ArchUnit), sie können außerhalb bspw. aus einem Application Service erstellt werden. Die Commands werden nicht direkt auf Aggregates angewandt, sondern die fachlichen Daten aus dem Command entnommen und auf die Fachmethode im Aggregate angewandt.

## Application Services und Domain Services

### Regeln für Application Services

- Koordination statt Logik: Ein Application Service delegiert die Fachlogik an Aggregates oder Domain Services; er enthält nur „Wenn-Dann“-Abläufe für den Prozess (z. B. „Lade Plan, füge Ausgabe hinzu, speichere Plan, veröffentliche Events“)

- Transaktionsgrenze: Hier beginnt und endet die Datenbanktransaktion (`@Transactional`); der Application Service stellt sicher, dass eine Operation atomar ist. Ausnahme: Ein Anwendungsfall, der höchstens eine Zeile schreibt und dabei etwas Langsames ohne Datenbank tut (z. B. BCrypt in `apps/users`), läuft ohne Transaktion, damit er keine Datenbankverbindung blockiert; der Javadoc des Service begründet das

- Infrastruktur-Brücke: Koordiniert technische Belange; Versand von Bestätigungs-E-Mails, Aufruf externer APIs oder Veröffentlichen von Events

- Eingabe/Ausgabe: Nimmt Commands oder einfache Datentypen entgegen und gibt Read Models, IDs oder einfache Bestätigungen zurück, niemals Aggregates; er schirmt die Domäne vor der Außenwelt ab. Web-DTOs bleiben in `infrastructure/web/dto` und werden im Controller in Commands übersetzt

- Sicherheit und Zugriff: Authentifizierung geschieht vorgeschaltet (Spring Security); die Autorisierung („Darf dieser Benutzer diesen Plan ändern?“) im Application Service, wie unter „Autorisierung über Besitz“ beschrieben

### Regeln für Domain Services

- Zustandslosigkeit: Domain Services sind zustandslos, sie halten keine Daten, sondern führen Berechnungen oder Operationen auf übergebenen Aggregates aus

- Fachliche Sprache: Die Methodennamen spiegeln die Ubiquitous Language wider (siehe `docs/glossar.md`), auf Englisch wie aller Code

- Keine Infrastruktur: Ein Domain Service darf keine technischen Details (E-Mail-Versand, Datenbanktransaktionen) kennen; er bleibt im Domain Layer

- Logikfokus: Nutze Domain Services nur, wenn die Logik wirklich nicht in ein Aggregate oder Value Object passt. Das Ziel bleibt ein Rich Domain Model, kein prozeduraler Code.

## Domain Events

### Regeln für Domain Events

- Benennung: Domain Events dokumentieren fachlich relevante Fakten der Vergangenheit und werden konsequent in der Vergangenheitsform benannt (z. B. `ExpenseAdded`, `UserRegistered`).

- Inhalt: Ein Domain Event sollte nur die Identitäten (IDs), den Zeitpunkt und die minimal notwendigen Daten enthalten, um den Empfängern die Arbeit zu ermöglichen

- Unveränderlichkeit: Domain Events sind Records (ArchUnit)

## Read Model und Query Handler
### Regeln für Read Model und Query Handler

- Immutability: Read Models sind Records (ArchUnit); enthaltene Listen werden mit `List.copyOf` kopiert

- Clientzentrierung: Die Struktur eines Read Models richtet sich ausschließlich nach den Bedürfnissen des Konsumenten (z. B. UI oder API-Client); sie darf flach sein, kann aber auch hierarchisch oder verschachtelt aufgebaut sein, wenn das die Verarbeitung im Client vereinfacht

- Grenzüberschreitung: Im Gegensatz zu Aggregates dürfen Read Models Daten aus verschiedenen Aggregates kombinieren (Denormalisierung), um Joins auf Client-Seite zu vermeiden

- Bereitstellung via Query-Handler: Read Models werden niemals über Repositorys bezogen, sondern über dedizierte Query-Handler (ArchUnit)

- Wiederverwendung von Value Objects oder typisierten IDs: Es ist ausdrücklich erlaubt und erwünscht, Value Objects oder typisierte IDs aus der Domäne im Read Model wiederzuverwenden; das stellt sicher, dass fachliche Formatierungen und Berechnungen konsistent bleiben (z. B. rechnet `HouseholdPlanOverview` über `BudgetSummary` statt selbst)

- Keine neue Domänenlogik: Ein Read Model darf existierende Logik von Value Objects nutzen, aber keine eigene neue Geschäftslogik oder Validierungen einführen; es bleibt ein reiner Informationsträger.

- Performancefokus: Query-Handler dürfen den „schweren“ Weg über das Domänenmodell umgehen und optimierte Abfragen (z. B. natives SQL oder spezialisierte Views) nutzen
