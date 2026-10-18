# Produkt: Household Budget Tracker

> **Entwurf.** Aus dem bestehenden Code und dem README abgeleitet. Abschnitte mit „(bitte bestätigen)" sind Annahmen,
> die noch geprüft und ergänzt werden müssen.

## Zweck

Der Household Budget Tracker ist ein Haushaltsbuch, das die geplanten Einnahmen und Ausgaben eines Jahres nach der
**50/30/20-Regel** bewertet: höchstens 50 % des Einkommens für den Lebensunterhalt, höchstens 30 % für Freizeit & Spaß,
mindestens 20 % für Sparen & Investieren. Die Anwendung zeigt auf einen Blick, ob der eigene Jahresplan diese Regel
einhält und ob mehr ausgegeben als eingenommen wird.

## Zielgruppe

Privatpersonen, die ihr Jahresbudget einfach planen wollen, ohne einzelne Buchungen oder Kontoauszüge zu erfassen.
Jeder Benutzer verwaltet nur seine eigenen Pläne. (bitte bestätigen)

## Funktionsumfang heute

| Bereich | Was der Benutzer tun kann | Spec |
|---|---|---|
| Anmeldung | Registrieren, anmelden, abmelden; Sitzung läuft nach 8 Stunden ab | [anmeldung.md](specs/anmeldung.md) |
| Haushaltsplan | Pro Jahr einen Plan anlegen, zwischen Jahren wechseln | [haushaltsplan.md](specs/haushaltsplan.md) |
| Posten | Einnahmen und Ausgaben (mit Kategorie) erfassen, löschen, Löschen rückgängig machen | [posten.md](specs/posten.md) |
| Auswertung | Summen, Saldo, Warnung bei Defizit, Anteil und Zielerreichung je 50/30/20-Kategorie | [auswertung-50-30-20.md](specs/auswertung-50-30-20.md) |

Alle Beträge sind in Euro. Die Oberfläche ist deutschsprachig.

## Nicht-Ziele (bitte bestätigen)

Abgeleitet aus dem, was der Code bewusst nicht kann:

- Keine einzelnen Buchungen mit Datum, keine Monatsansicht: Ein Posten ist ein Jahresbetrag ohne eigenes Datum.
- Keine Anbindung an Bankkonten, kein Import von Kontoauszügen.
- Keine gemeinsamen Pläne mehrerer Benutzer, keine Freigaben.
- Keine Bearbeitung eines Postens; ändern heißt löschen und neu anlegen.
- Keine anderen Budgetregeln als 50/30/20, keine eigenen Kategorien, keine anderen Währungen.
- Keine Passwort-Zurücksetzung, keine E-Mail-Bestätigung, kein Löschen des Benutzerkontos.

## Roadmap

Noch offen. Hier sollen die nächsten geplanten Features stehen, idealerweise jeweils mit einem GitHub-Issue und einer
Spec mit Status „geplant".

## Weiterführend

- [Glossar](glossar.md): Fachbegriffe und ihre Namen im Code
- [Specs](specs/): fachliche Regeln und Akzeptanzkriterien je Feature
- [README](../README.md): Architektur, lokale Entwicklung, CI/CD
