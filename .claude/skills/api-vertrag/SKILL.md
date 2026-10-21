---
name: api-vertrag
description: Den OpenAPI-Vertrag nach einer Änderung an Controller oder DTO in apps/backend oder apps/users neu schreiben und das Frontend nachziehen. Verwenden, wenn OpenApiContractTest mit „API changed" fehlschlägt oder ein Endpunkt, Request oder Response geändert wird.
---

# API-Vertrag aktualisieren

Richtung immer Java → JSON → TypeScript. `packages/shared-types/openapi/*.json` wird nie von Hand geändert und muss
auch nicht gelesen werden.

1. Controller bzw. DTO ändern, Controller-Test anpassen.
2. In der betroffenen App (`apps/backend` → `plans.json`, `apps/users` → `users.json`):

   ```bash
   ./mvnw -q verify -Dit.test=OpenApiContractTest -Dopenapi.update=true
   ```

3. Prüfen, ob nur das Erwartete geändert ist, ohne die ganze Datei zu lesen:

   ```bash
   git diff --stat packages/shared-types/openapi/
   git diff -U1 packages/shared-types/openapi/plans.json | grep '^[+-]' | head -80
   ```

4. `npm run build -s --workspace=frontend`: erzeugt die Typen neu; die Compilerfehler zeigen, wo das Frontend
   nachgezogen werden muss (meist `data-access/budget-api.ts`, `budget.model.ts`, Testdaten in `*.spec.ts`).
5. `./mvnw -q verify` (ohne `-Dopenapi.update`) und `npm test -s --workspace=frontend` grün.
6. Java, JSON und Frontend in einem Commit.
