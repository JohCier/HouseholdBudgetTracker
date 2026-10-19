# Anmeldung

Status: umgesetzt

## Zweck

Jeder Benutzer soll nur seine eigenen Haushaltspläne sehen. Dafür registriert er sich mit E-Mail-Adresse und Passwort
und meldet sich an. Die Sitzung gilt für beide Dienste (Users und Backend).

## Fachliche Regeln

- **AN-1**: Ein Benutzer meldet sich mit E-Mail-Adresse und Passwort an.
- **AN-2**: Die E-Mail-Adresse wird ohne umgebende Leerzeichen und kleingeschrieben gespeichert. `Max@Example.org` und
  `max@example.org` sind dasselbe Konto. Geprüft wird nur die grobe Form (`etwas@etwas.etwas`, höchstens 254 Zeichen),
  nicht, ob die Adresse existiert.
- **AN-3**: Eine E-Mail-Adresse gehört zu höchstens einem Benutzer.
- **AN-4**: Ein Passwort hat mindestens 8 Zeichen und höchstens 72 Bytes (UTF-8). Es wird nie gespeichert, nur sein
  BCrypt-Hash.
- **AN-5**: Nach der Registrierung ist der Benutzer sofort angemeldet.
- **AN-6**: Eine falsche E-Mail-Adresse und ein falsches Passwort führen zur selben Fehlermeldung, damit nicht erkennbar
  ist, welche Adressen registriert sind.
- **AN-7**: Eine Sitzung dauert 8 Stunden. Danach muss sich der Benutzer neu anmelden; die Oberfläche leitet dann zur
  Anmeldung weiter.
- **AN-8**: Abmelden beendet die Sitzung im Browser. Sitzungen werden serverseitig nicht gespeichert.
- **AN-9**: Angemeldete Benutzer sehen Anmeldung und Registrierung nicht, nicht angemeldete nur diese beiden Seiten.
- **AN-10**: Nach 5 fehlgeschlagenen Anmeldungen für dieselbe E-Mail-Adresse innerhalb von 15 Minuten wird
  die Anmeldung für diese Adresse 15 Minuten lang abgelehnt, auch mit richtigem Passwort (Anmeldesperre). Das gilt
  genauso für nicht registrierte Adressen, damit die Sperre nicht verrät, welche Adressen es gibt (AN-6). Eine
  erfolgreiche Anmeldung setzt den Zähler zurück.

## Akzeptanzkriterien

- **Gegeben** eine noch nicht registrierte Adresse, **wenn** sich jemand mit gültigem Passwort registriert, **dann**
  antwortet die API mit 201, setzt das Sitzungs-Cookie, und die Oberfläche zeigt das Haushaltsbuch.
- **Gegeben** eine registrierte Adresse, **wenn** sich jemand erneut mit ihr registriert (auch in anderer
  Groß-/Kleinschreibung), **dann** antwortet die API mit 409.
- **Gegeben** ein Passwort mit 7 Zeichen, **wenn** sich jemand registriert, **dann** antwortet die API mit 400, und die
  Oberfläche zeigt „Mindestens 8 Zeichen.", ohne die Anfrage zu senden.
- **Gegeben** ein registrierter Benutzer, **wenn** er sich mit falschem Passwort anmeldet, **dann** antwortet die API mit
  401.
- **Gegeben** 5 fehlgeschlagene Anmeldungen für eine Adresse innerhalb von 15 Minuten, **wenn** sich jemand erneut mit
  dieser Adresse anmeldet, auch mit richtigem Passwort, **dann** antwortet die API mit 429, und die Oberfläche zeigt
  „Zu viele Anmeldeversuche. Bitte versuche es in 15 Minuten erneut." (AN-10).
- **Gegeben** eine Anmeldesperre, **wenn** 15 Minuten vergangen sind, **dann** ist die Anmeldung wieder möglich (AN-10).
- **Gegeben** eine abgelaufene Sitzung, **wenn** die Oberfläche Daten lädt, **dann** leitet sie zur Anmeldung weiter.
- **Gegeben** eine Sitzung, **wenn** `GET /api/auth/me` aufgerufen wird, **dann** liefert die API ID und E-Mail-Adresse
  des Benutzers, ohne Sitzung 401.

## Nicht im Umfang

- Begrenzung der Anmeldeversuche je IP-Adresse; Sperre bei der Registrierung
- Passwort zurücksetzen oder ändern
- E-Mail-Adresse bestätigen
- Benutzerkonto löschen
- Anmeldung über externe Anbieter (Google, GitHub …)
- Sitzungen serverseitig beenden (z. B. „auf allen Geräten abmelden")

## Umsetzung

- Domäne: `User` (`register`, `authenticate`), `EmailAddress`, `Password`, `PasswordHash`, `PasswordHasher`,
  `RegisterUserCommand`, Event `UserRegistered`
- AN-3: `UserApplicationService.register` prüft vorab, die Unique-Constraint in `V1__app_user.sql` sichert gegen
  gleichzeitige Anfragen
- AN-6: `UserApplicationService.authenticate` prüft das Passwort auch bei unbekannter Adresse (gegen einen Hash, zu
  dem kein Passwort passt), damit die Antwortzeit nicht verrät, ob es die Adresse gibt
- AN-10: Port `LoginThrottle` im Application Layer, umgesetzt von `InMemoryLoginThrottle` (Infrastruktur, Zähler im
  Speicher); `UserApplicationService.authenticate` meldet Fehlversuche und setzt den Zähler nach einer erfolgreichen
  Anmeldung zurück; `LoginLockedException` wird zu 429 mit `urn:hbt:too-many-login-attempts` (siehe
  [fehlermeldungen.md](fehlermeldungen.md)). Die Zähler gelten je Instanz des Users-Dienstes und gehen bei einem
  Neustart verloren
- Sitzung: `SessionCookies` (Users) stellt das Cookie `HBT_SESSION` mit JWT (HS256) aus; das Backend prüft es mit
  demselben `JWT_SECRET` (`CookieBearerTokenResolver`, `SecurityConfig`)
- API: `POST /api/auth/register`, `/login`, `/logout`, `GET /api/auth/me`
- Frontend: `auth/ui/login-page.ts`, `auth/ui/register-page.ts`, `auth/auth.guards.ts`,
  `auth/session-expired.interceptor.ts`, `auth/data-access/auth-store.ts`
- Tests: `UserTest`, `EmailAddressTest`, `PasswordTest`, `LoginThrottleTest`, `UserApplicationServiceTest`,
  `AuthControllerTest`, `UsersIntegrationTest`, die `*.spec.ts` unter `auth/` und `shared/http-error.spec.ts`
- E2E: `e2e/tests/anmeldung.spec.ts`

## Offene Fragen

