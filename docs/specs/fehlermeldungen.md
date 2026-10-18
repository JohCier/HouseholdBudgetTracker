# Fehlermeldungen

Status: umgesetzt

## Zweck

Die Oberfläche ist deutschsprachig, die Fehlermeldungen der API (`detail` der Problem Details) sind aber englisch und
werden heute unverändert angezeigt, z. B. bei einem doppelten Plan oder einer bereits registrierten E-Mail-Adresse.
Benutzer sollen jede Meldung auf Deutsch lesen.

## Fachliche Regeln

- **FM-1**: Jede Fehlermeldung, die die Oberfläche anzeigt, ist deutsch, auch wenn der Fehler von der API kommt.
- **FM-2**: Die Übersetzung geschieht im Frontend. Die API bleibt sprachneutral und liefert zu jedem fachlichen Fehler
  eine stabile Kennung im Feld `type` der Problem Details (z. B. `urn:hbt:plan-already-exists`), an der das Frontend die Meldung erkennt; der englische `detail`-Text bleibt für Logs und
  API-Clients erhalten.
- **FM-3**: Kennt das Frontend einen Fehler nicht, zeigt es eine allgemeine deutsche Meldung statt des englischen
  Textes: „Die Anfrage ist fehlgeschlagen (400)."
- **FM-4**: Meldungen, die das Frontend schon heute selbst erzeugt (Formularprüfungen, „Server nicht erreichbar."),
  bleiben unverändert.

## Akzeptanzkriterien

- **Gegeben** ein Benutzer mit Plan für 2026, **wenn** er (z. B. in einem zweiten Tab) erneut einen Plan für 2026
  anlegt, **dann** zeigt die Oberfläche „Für dieses Jahr gibt es bereits einen Haushaltsplan."
- **Gegeben** ein Posten, der in einem anderen Tab schon gelöscht wurde, **wenn** er gelöscht werden soll, **dann** zeigt
  die Oberfläche „Diesen Eintrag gibt es nicht (mehr)."
- **Gegeben** eine registrierte Adresse, **wenn** sich jemand erneut mit ihr registriert, **dann** zeigt die Oberfläche
  „Für diese E-Mail-Adresse gibt es bereits ein Konto."
- **Gegeben** eine falsche E-Mail-Adresse oder ein falsches Passwort, **dann** zeigt die Oberfläche „E-Mail-Adresse oder
  Passwort ist falsch." (AN-6)
- **Gegeben** ein Fehler mit unbekannter Kennung, **dann** zeigt die Oberfläche die allgemeine deutsche Meldung, nie den
  englischen `detail`-Text.

## Nicht im Umfang

- Weitere Sprachen oder eine Sprachauswahl
- Übersetzung der API-Texte für andere Clients

## Umsetzung

- Kennungen (`type` der Problem Details), gesetzt von `GlobalExceptionHandler` in beiden Java-Apps:

  | Kennung | Status | Meldung in der Oberfläche |
  |---|---|---|
  | `urn:hbt:invalid-input` | 400 | Die Eingabe ist ungültig. |
  | `urn:hbt:plan-not-found` | 404 | Diesen Haushaltsplan gibt es nicht (mehr). |
  | `urn:hbt:item-not-found` | 404 | Diesen Eintrag gibt es nicht (mehr). |
  | `urn:hbt:plan-already-exists` | 409 | Für dieses Jahr gibt es bereits einen Haushaltsplan. |
  | `urn:hbt:email-already-registered` | 409 | Für diese E-Mail-Adresse gibt es bereits ein Konto. |
  | `urn:hbt:invalid-credentials` | 401 | E-Mail-Adresse oder Passwort ist falsch. |
  | `urn:hbt:too-many-login-attempts` | 429 | Zu viele Anmeldeversuche. Bitte versuche es in 15 Minuten erneut. (AN-10) |

  Fehler des Frameworks (z. B. ein Jahr, das keine Zahl ist) behalten `about:blank` und bekommen die allgemeine Meldung
  (FM-3). Die Kennungen stehen nicht im API-Vertrag, weil Springdoc die Fehlerantworten nicht beschreibt; eine neue
  Kennung braucht deshalb einen Eintrag in dieser Tabelle und in `shared/http-error.ts`.
- Frontend: `shared/http-error.ts` (`toMessage`) übersetzt für alle Stores; `AuthStore` hat keine eigenen Meldungen mehr
- Tests: `HouseholdPlanControllerTest`, `AuthControllerTest`, `http-error.spec.ts`, `auth-store.spec.ts`,
  `budget-store.spec.ts`
- E2E: `e2e/tests/fehlermeldungen.spec.ts` (FM-1, FM-2); `anmeldung.spec.ts` deckt die Meldungen bei Registrierung und
  Anmeldung ab, FM-3 decken die Unit-Tests ab

## Offene Fragen

