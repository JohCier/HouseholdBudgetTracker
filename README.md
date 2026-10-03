# Household Budget Tracker

Haushaltsbuch nach der 50/30/20-Regel.

| Ordner      | Inhalt                                                              |
|-------------|---------------------------------------------------------------------|
| `backend/`  | Spring Boot (Java 25, Maven) – DDD / hexagonale Architektur, REST-API unter `/api/budget` |
| `frontend/` | Angular 22 + Angular Material                                       |

## Lokale Entwicklung

```bash
# Backend auf :8080
cd backend && ./mvnw spring-boot:run

# Frontend auf :4200, /api wird per Proxy an :8080 weitergeleitet
cd frontend && npm ci && npm start
```

Tests: `./mvnw verify` (backend) bzw. `npm test` (frontend).

## Auslieferbares JAR

```bash
cd backend && ./mvnw package -Pfrontend
java -jar target/HouseHoldBudgetTracker-0.0.1-SNAPSHOT.jar   # UI + API auf :8080
```

Das Profil `frontend` baut die Angular-App und legt sie unter `classpath:/static` ins JAR.
Die GitHub-Action `.github/workflows/ci.yml` baut und testet beide Teile und stellt das JAR als Artefakt bereit.
