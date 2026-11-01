---
paths:
  - "apps/backend/src/**/*.java"
  - "apps/users/src/**/*.java"
---

# DDD-Regeln für die Java-Apps

Der Abschnitt „Konventionen in diesem Projekt“ beschreibt, wie die Bausteine hier konkret gebaut werden; der Abschnitt
„Regeln je Baustein“ ergänzt nur, was darüber hinaus gilt. Die mit „(ArchUnit)“ markierten Regeln prüft `ArchitectureTest` (ArchUnit,
jMolecules) in beiden Apps maschinell; was für beide gilt, steht einmal in `DddArchitectureRules` (`libs/test-support`),
der `ArchitectureTest` eines Service ergänzt nur seine Besonderheiten. Schlägt `ArchitectureTest` fehl, wird der Code angepasst, nicht der Test; eine
Änderung am Test nur nach Rückfrage.

## Konventionen in diesem Projekt

### Annotationen und Pakete

- Annotationen sind Pflicht: Jeder Baustein trägt seine jMolecules-Annotation (`@AggregateRoot`, `@Entity`,
  `@ValueObject`, `@Identity` am ID-Feld, `@Command`, `@QueryModel`) bzw. die eigene `@QueryHandler`-Annotation;
  Domain Events implementieren `TimestampedDomainEvent`. Ohne Annotation greift keine ArchUnit-Prüfung, Verstöße fallen
  dann nicht auf

- Pakete (Beispiel Backend, `users` entsprechend unter `de.jcss.hbt.users`):
  - `domain/<aggregat>/`: Aggregate Root, ihre Entities, Repository-Interface, Domain-Ports. Entities liegen im Paket
    ihrer Root, damit ihre verändernden Methoden package-private bleiben können (ArchUnit)
  - `domain/<aggregat>/valueobject/`: alle Value Objects des Aggregates einschließlich der typisierten IDs und Enums,
    und nur diese (ArchUnit). Value Objects kennen weder Aggregate Root noch Entities (ArchUnit); ein Verweis im
    Javadoc ist erlaubt
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

- Value Objects nur mit fachlicher Regel: Ein Typ wird nur dann Value Object, wenn er eine Invariante im Konstruktor
  hat, die eine Spec-Regel umsetzt (z. B. `BudgetItemNote`: getrimmt, höchstens 1000 Zeichen, PN-2, PN-3), oder
  fachliches Verhalten (rechnen, vergleichen, umrechnen, z. B. `ItemAmount.monthly()`); der Javadoc nennt die
  Regel-ID. Ausnahmen sind typisierte IDs und Enums für eine geschlossene Wertemenge aus dem Glossar (`ItemType`,
  `AmountPeriod`). Kein Value Object sind reine Bündel von Feldern ohne eigene Regel: Ihre Felder stehen direkt in
  der Entity, dem Command oder dem Read Model (`BudgetItem.important` und `savingPotential` statt eines `ItemMarks`).
  Ein Enum, das nur ein Event näher beschreibt, wird im Event verschachtelt (`BudgetItemMarked.Mark`). Texte und
  Zahlen ohne Regel bleiben `String` bzw. `int`. ArchUnit kann das nicht prüfen; es ist eine Review-Regel

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
  im `GlobalExceptionHandler`. Im Backend erben „nicht gefunden" und „existiert schon" von `NotFoundException` (404)
  bzw. `AlreadyExistsException` (409) und geben ihre `type`-Kennung als `code` mit (`plan-not-found` →
  `urn:hbt:plan-not-found`); dafür braucht es keinen weiteren `@ExceptionHandler`

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

## Regeln je Baustein

### Aggregates und Entities

- Invarianten per Guard Clauses im Konstruktor und in jeder fachlichen Methode; keine „leeren“ Objekte; interne
  Listen nur als Kopie herausgeben; keine öffentlichen Setter (ArchUnit)
- Keine Repository-Aufrufe oder Infrastruktur im Aggregate; Domain-Ports als Methodenparameter sind erlaubt
- Klein halten: nur, was atomar konsistent sein muss. Andere Aggregates nur über ihre typisierte ID referenzieren
  (ArchUnit)
- Tell, Don’t Ask: Entscheidungen trifft das Aggregate (`plan.isOwnedBy(owner)` statt `plan.owner().equals(owner)`)
- Interne Entities ändert nur die Root: Die Klasse darf public sein, ihre verändernden Methoden sind package-private;
  sie leben und sterben mit der Root und schützen ihre eigenen Invarianten

### Value Objects

- Unveränderlich, Gleichheit über alle Werte (equals/hashCode nie von Hand), Methoden geben neue Instanzen zurück
  (z. B. `BudgetSummary.withExpense(...)`)
- Gegen Primitive Obsession (`Money`, `Percentage`, `BudgetItemDescription`), aber nur wo fachliche Regeln dranhängen

### Repositorys

- Interface in der Domäne, Implementierung in der Infrastruktur, ohne SQL- oder JDBC-Typen in der Schnittstelle
- Nur für Aggregate Roots, immer das ganze Aggregate; `add` für neue, `findById` zum Laden, `save` nach jeder Änderung
- Fachliche Filter zum Laden oder Steuern sind erlaubt (`findByEmail`, `existsFor(owner, year)`); Abfragen für
  Anzeigen über mehrere Aggregates gehören in Read Models

### Domain Commands

- Records in der Domäne (ArchUnit), nach der fachlichen Absicht benannt (`AddExpenseCommand`, nicht
  `UpdatePlanRequest`); eine Absicht, eine Transaktion, ein Aggregate
- Nur formale Prüfung (meist `Objects.requireNonNull`, die Felder sind schon Value Objects); keine Prüfung gegen den
  Systemzustand. Der Application Service reicht die Daten an die Fachmethode des Aggregates weiter, nicht das Command

### Application Services und Domain Services

- Application Service: koordiniert (laden, Fachmethode, speichern, Events veröffentlichen), enthält keine Fachlogik,
  ist die Transaktionsgrenze (`@Transactional`). Ausnahme: Ein Fall, der höchstens eine Zeile schreibt und etwas
  Langsames ohne Datenbank tut (BCrypt in `apps/users`), läuft ohne Transaktion; der Javadoc begründet das
- Nimmt Commands oder einfache Typen, gibt Read Models, IDs oder Bestätigungen zurück, nie Aggregates; Web-DTOs bleiben
  in `infrastructure/web/dto`
- Domain Service nur, wenn Logik weder in ein Aggregate noch in ein Value Object passt; zustandslos, ohne Infrastruktur,
  Methodennamen aus `docs/glossar.md`

### Domain Events

- Fakten in der Vergangenheitsform (`ExpenseAdded`, `UserRegistered`); nur IDs, Zeitpunkt und die nötigsten Daten

### Read Models und Query Handler

- Aufbau nach den Bedürfnissen des Konsumenten, darf Daten mehrerer Aggregates kombinieren; Listen mit `List.copyOf`
- Value Objects und IDs der Domäne wiederverwenden (z. B. rechnet `HouseholdPlanOverview` über `BudgetSummary`), aber
  keine neue Fachlogik oder Validierung
- Query Handler dürfen optimiertes SQL nutzen und das Domänenmodell umgehen
