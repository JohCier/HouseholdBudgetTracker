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
| Anmeldung | Registrieren, anmelden, abmelden; Sitzung läuft nach 8 Stunden ab; Sperre nach 5 Fehlversuchen | [anmeldung.md](specs/anmeldung.md) |
| Haushaltsplan | Pro Jahr einen Plan anlegen, zwischen Jahren wechseln | [haushaltsplan.md](specs/haushaltsplan.md) |
| Posten | Einnahmen und Ausgaben (mit Kategorie) erfassen, löschen, Löschen rückgängig machen | [posten.md](specs/posten.md) |
| Posten bearbeiten | Beschreibung, Betrag und Kategorie eines Postens ändern | [posten-bearbeiten.md](specs/posten-bearbeiten.md) |
| Notiz zum Posten | Optionaler Freitext je Posten, beim Erfassen und Bearbeiten | [posten-notiz.md](specs/posten-notiz.md) |
| Unterkategorie | Posten frei benennbaren Unterkategorien zuordnen; Einträge gruppiert nach Art, Kategorie und Unterkategorie mit Summen ansehen und frei sortieren | [unterkategorie.md](specs/unterkategorie.md) |
| Fehlermeldungen | Alle Meldungen auf Deutsch, auch die der API | [fehlermeldungen.md](specs/fehlermeldungen.md) |
| Auswertung | Summen, Saldo, Warnung bei Defizit, Anteil und Zielerreichung je 50/30/20-Kategorie | [auswertung-50-30-20.md](specs/auswertung-50-30-20.md) |
| Tortendiagramm | Die 50/30/20-Auswertung wahlweise als Balken oder als Tortendiagramm ansehen | [tortendiagramm.md](specs/tortendiagramm.md) |
| Übersicht als Antwort | Nach jeder Änderung liefert die API die neue Übersicht mit, die Oberfläche lädt nicht erneut | [antwort-mit-uebersicht.md](specs/antwort-mit-uebersicht.md) |

Alle Beträge sind in Euro. Die Oberfläche ist deutschsprachig.

## Nicht-Ziele

- Keine einzelnen Buchungen mit Datum, keine Monatsansicht: Ein Posten ist ein Jahresbetrag ohne eigenes Datum.
- Keine Anbindung an Bankkonten, kein Import von Kontoauszügen.
- Keine gemeinsamen Pläne mehrerer Benutzer, keine Freigaben.
- Keine anderen Budgetregeln als 50/30/20, keine eigenen Kategorien, keine anderen Währungen.
- Keine Passwort-Zurücksetzung, keine E-Mail-Bestätigung, kein Löschen des Benutzerkontos.

## Roadmap

Geplante Änderungen, je eine Spec bzw. Regel mit Status „geplant". Derzeit ist keine Änderung geplant.

## Weiterführend

- [Glossar](glossar.md): Fachbegriffe und ihre Namen im Code
- [Specs](specs/): fachliche Regeln und Akzeptanzkriterien je Feature
- [README](../README.md): Architektur, lokale Entwicklung, CI/CD
