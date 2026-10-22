# Passwort ändern

Status: umgesetzt

## Zweck

Ein angemeldeter Benutzer soll sein Passwort selbst ändern können, etwa weil er ein schwaches Passwort ersetzen will
oder befürchtet, dass jemand es kennt. Ohne diese Funktion bleibt das Passwort aus der Registrierung für immer gültig.
Gemeint ist das Ändern mit dem aktuellen Passwort, nicht das Zurücksetzen eines vergessenen Passworts per E-Mail.

## Fachliche Regeln

- **PA-1**: Nur ein angemeldeter Benutzer kann sein Passwort ändern, und nur sein eigenes. Dazu gibt er sein aktuelles
  und ein neues Passwort an.
- **PA-2**: Das Passwort wird nur geändert, wenn das aktuelle Passwort stimmt. Sonst bleibt das alte Passwort gültig,
  und die Oberfläche zeigt „Das aktuelle Passwort ist falsch."
- **PA-3**: Für das neue Passwort gelten dieselben Regeln wie bei der Registrierung (AN-4): mindestens 8 Zeichen,
  höchstens 72 Bytes, gespeichert wird nur der BCrypt-Hash.
- **PA-4**: Das neue Passwort muss sich vom aktuellen unterscheiden („Das neue Passwort muss sich vom aktuellen
  unterscheiden.").
- **PA-5**: Die Oberfläche lässt das neue Passwort zweimal eingeben und sendet die Anfrage erst, wenn beide Eingaben
  gleich sind („Die Passwörter stimmen nicht überein."). Die API bekommt das neue Passwort nur einmal.
- **PA-6**: Ein falsches aktuelles Passwort zählt als Fehlversuch der Anmeldesperre (AN-10) für die Adresse des
  Benutzers. Ist die Adresse gesperrt, wird das Passwort auch mit richtigem aktuellen Passwort nicht geändert, und die
  Oberfläche zeigt die Meldung der Anmeldesperre. Damit lässt sich das Passwort in einer offenen Sitzung nicht
  durchprobieren.
- **PA-7**: Nach der Änderung bleibt der Benutzer in dieser Sitzung angemeldet, die Oberfläche bestätigt die Änderung
  mit „Passwort geändert." und kehrt zur Jahresübersicht zurück. Ab sofort gilt bei der Anmeldung nur noch das neue
  Passwort.
- **PA-8**: Die Funktion ist über „Passwort ändern" in der Kopfleiste neben „Abmelden" erreichbar (auf schmalen
  Bildschirmen nur als Schlüssel-Symbol) und führt auf eine eigene Seite (`/passwort`), die wie Anmeldung und
  Registrierung aussieht und auch auf dem Handy ohne waagerechtes Scrollen bedienbar ist
  ([mobile-ansicht.md](mobile-ansicht.md)). „Abbrechen" führt ohne Änderung zurück.

## Akzeptanzkriterien

- **Gegeben** ein angemeldeter Benutzer, **wenn** er aktuelles Passwort und zweimal ein gültiges neues Passwort
  eingibt, **dann** antwortet die API mit 204, die Oberfläche zeigt „Passwort geändert.", und nach dem Abmelden gelingt
  die Anmeldung nur noch mit dem neuen Passwort (PA-1, PA-7).
- **Gegeben** ein falsches aktuelles Passwort, **wenn** der Benutzer das Passwort ändert, **dann** antwortet die API mit
  400 (`urn:hbt:wrong-current-password`), die Oberfläche zeigt „Das aktuelle Passwort ist falsch.", und das alte
  Passwort gilt weiter (PA-2).
- **Gegeben** ein neues Passwort mit 7 Zeichen, **wenn** der Benutzer es abschickt, **dann** zeigt die Oberfläche
  „Mindestens 8 Zeichen.", ohne die Anfrage zu senden; die API antwortet darauf mit 400 (PA-3).
- **Gegeben** ein neues Passwort gleich dem aktuellen, **dann** antwortet die API mit 400, und die Oberfläche zeigt
  „Das neue Passwort muss sich vom aktuellen unterscheiden." (PA-4).
- **Gegeben** zwei verschiedene Eingaben für das neue Passwort, **dann** zeigt die Oberfläche „Die Passwörter stimmen
  nicht überein." und sendet keine Anfrage (PA-5).
- **Gegeben** 5 falsche aktuelle Passwörter innerhalb von 15 Minuten, **wenn** der Benutzer es erneut versucht, auch
  mit richtigem Passwort, **dann** antwortet die API mit 429, und auch die Anmeldung mit dieser Adresse ist gesperrt
  (PA-6).
- **Gegeben** keine Sitzung, **wenn** die Änderung aufgerufen wird, **dann** antwortet die API mit 401 (PA-1).

## Nicht im Umfang

- Passwort vergessen bzw. per E-Mail zurücksetzen (bleibt Nicht-Ziel)
- Andere Sitzungen beim Ändern beenden: Sitzungen werden serverseitig nicht gespeichert (AN-8), eine auf einem anderen
  Gerät offene Sitzung gilt deshalb bis zu ihrem Ablauf (höchstens 8 Stunden, AN-7) weiter
- E-Mail-Adresse ändern, Benachrichtigung per E-Mail über die Änderung
- Anforderungen an die Stärke des Passworts über AN-4 hinaus

## Umsetzung

- Domäne: `User.changePassword` (PA-2, PA-4; gibt den Benutzer mit dem neuen Hash zurück, da alle Felder final sind),
  `ChangePasswordCommand`, Event `PasswordChanged`, `WrongCurrentPasswordException`, `PasswordUnchangedException`;
  `UserRepository.findById` und `save` (`JdbcUserRepository`, ein `UPDATE`)
- Anwendung: `UserApplicationService.changePassword` zählt den Versuch vorab mit `LoginThrottle.countAttempt` für die
  Adresse des Benutzers und setzt den Zähler nach der Änderung zurück (PA-6); ohne Transaktion wie die übrigen Fälle
- API: `POST /api/auth/password` (`ChangePasswordRequest`), 204, verlangt eine Sitzung (`SecurityConfig`); Problemtypen
  `urn:hbt:wrong-current-password` und `urn:hbt:password-unchanged` (400, siehe [fehlermeldungen.md](fehlermeldungen.md))
- Frontend: `auth/ui/change-password-page.ts`, `auth/ui/passwords-match.ts` (gemeinsam mit der Registrierung), Route
  `/passwort` mit `signedInGuard`, `AuthApi.changePassword`, `AuthStore.changePassword`, Link mit Symbol `key` in
  `app.ts`
- Tests: `UserTest`, `UserApplicationServiceTest`, `AuthControllerTest`, `UsersIntegrationTest`,
  `change-password-page.spec.ts`, `auth-store.spec.ts`, `auth-api.spec.ts`, `http-error.spec.ts`, `app.spec.ts`
- E2E: `e2e/tests/passwort-aendern.spec.ts` (PA-1 bis PA-8); die Kopfleiste auf dem Handy deckt
  `mobile-ansicht.spec.ts` (MA-7) mit ab

## Offene Fragen
