# Fehlermeldungen

Status: geplant

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
  Textes, z. B. „Die Anfrage ist fehlgeschlagen (400)."
- **FM-4**: Meldungen, die das Frontend schon heute selbst erzeugt (Formularprüfungen, „Server nicht erreichbar."),
  bleiben unverändert.

## Akzeptanzkriterien

- **Gegeben** ein Benutzer mit Plan für 2026, **wenn** er erneut einen Plan für 2026 anlegt, **dann** zeigt die
  Oberfläche eine deutsche Meldung, z. B. „Für 2026 gibt es bereits einen Haushaltsplan."
- **Gegeben** eine registrierte Adresse, **wenn** sich jemand erneut mit ihr registriert, **dann** zeigt die Oberfläche
  „Diese E-Mail-Adresse ist bereits registriert."
- **Gegeben** eine falsche E-Mail-Adresse oder ein falsches Passwort, **dann** zeigt die Oberfläche „E-Mail-Adresse oder
  Passwort ist falsch." (AN-6)
- **Gegeben** ein Fehler mit unbekannter Kennung, **dann** zeigt die Oberfläche die allgemeine deutsche Meldung, nie den
  englischen `detail`-Text.

## Nicht im Umfang

- Weitere Sprachen oder eine Sprachauswahl
- Übersetzung der API-Texte für andere Clients

## Umsetzung

Noch nicht umgesetzt. Betroffen: `GlobalExceptionHandler` in beiden Java-Apps (Kennung, z. B. als `type` der Problem
Details), `shared/http-error.ts` im Frontend, API-Vertrag in `packages/shared-types/openapi/`.

## Offene Fragen

