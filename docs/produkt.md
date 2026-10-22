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

| Bereich | Was der Benutzer tun kann | Spec | Regeln |
|---|---|---|---|
| Anmeldung | Registrieren, anmelden, abmelden; Sitzung läuft nach 8 Stunden ab; Sperre nach 5 Fehlversuchen | [anmeldung.md](specs/anmeldung.md) | AN-… |
| Haushaltsplan | Pro Jahr einen Plan anlegen, zwischen Jahren wechseln | [haushaltsplan.md](specs/haushaltsplan.md) | HP-… |
| Jahresübersicht | Startseite mit allen Plänen: Einnahmen, Ausgaben, Überschuss und Veränderung zum Vorjahr je Jahr | [jahresuebersicht.md](specs/jahresuebersicht.md) | JU-… |
| Monatsbeträge in der Jahresübersicht | Die Jahresübersicht wahlweise in Monats- oder Jahresbeträgen ansehen; der Browser merkt sich die Wahl | [jahresuebersicht-monatsbetraege.md](specs/jahresuebersicht-monatsbetraege.md) | JM-… |
| Plan als Vorlage übernehmen | Einen Plan mit allen Posten in ein frei wählbares Jahr übernehmen (vorgeschlagen: Folgejahr), aus Planansicht, Jahresübersicht oder einem Jahr ohne Plan; einen bestehenden Plan nach Rückfrage überschreiben | [plan-uebernehmen.md](specs/plan-uebernehmen.md) | PU-… |
| Plan löschen | Einen Plan mit allen Posten nach Rückfrage endgültig löschen, aus der Jahresübersicht oder der Planansicht | [plan-loeschen.md](specs/plan-loeschen.md) | PL-… |
| Posten | Einnahmen und Ausgaben (mit Kategorie) erfassen, löschen, Löschen rückgängig machen | [posten.md](specs/posten.md) | PO-… |
| Posten bearbeiten | Beschreibung, Betrag und Kategorie eines Postens ändern | [posten-bearbeiten.md](specs/posten-bearbeiten.md) | PB-… |
| Notiz zum Posten | Optionaler Freitext je Posten, beim Erfassen und Bearbeiten | [posten-notiz.md](specs/posten-notiz.md) | PN-… |
| Unterkategorie | Posten frei benennbaren Unterkategorien zuordnen; Einträge gruppiert nach Art, Kategorie und Unterkategorie mit Summen ansehen und frei sortieren | [unterkategorie.md](specs/unterkategorie.md) | UK-… |
| Monats- und Jahresbetrag | Beträge monatlich oder jährlich angeben; Monats- und Jahresbetrag je Posten und für alle Summen ansehen | [monats-jahresbetrag.md](specs/monats-jahresbetrag.md) | MJ-… |
| Einträge in Gruppen | Neue Posten unten in ihrer Gruppe, Summen als Zeilen unter Gruppe, Kategorie und Bereich, Posten direkt in einer Gruppe erfassen | [eintraege-gruppen.md](specs/eintraege-gruppen.md) | EG-… |
| Farben der Einträge | Unterkategorien als getönte Blöcke in der Farbe ihrer Kategorie, Summen der Kategorien deutlich hervorgehoben, hell und dunkel | [eintraege-farben.md](specs/eintraege-farben.md) | EF-… |
| Ansicht auf schmalen Bildschirmen | Planansicht, Jahresübersicht und Dialoge auf dem Handy ohne waagerechtes Scrollen; Posten und Summen zweizeilig | [mobile-ansicht.md](specs/mobile-ansicht.md) | MA-… |
| Bilanz | Gesamteinnahmen, Gesamtausgaben und Saldo in einem Kasten, je Monat und je Jahr nebeneinander | [bilanz.md](specs/bilanz.md) | BI-… |
| Posten markieren | Posten in den Einträgen als wichtig hervorheben; bei Ausgaben ein monatliches Sparpotential festhalten, in der Bilanz summiert | [posten-markierungen.md](specs/posten-markierungen.md) | PM-… |
| Sparpotential je Unterkategorie | Links neben der Bilanz die Summe des Sparpotentials je Unterkategorie, je Monat und Jahr, das größte oben; je Gruppe auch in ihrer Summenzeile der Einträge | [sparpotential-unterkategorie.md](specs/sparpotential-unterkategorie.md) | SU-… |
| Fehlermeldungen | Alle Meldungen auf Deutsch, auch die der API | [fehlermeldungen.md](specs/fehlermeldungen.md) | FM-… |
| Auswertung | Summen, Saldo, Warnung bei Defizit, Anteil und Zielerreichung je 50/30/20-Kategorie | [auswertung-50-30-20.md](specs/auswertung-50-30-20.md) | AW-… |
| Tortendiagramm | Die 50/30/20-Auswertung wahlweise als Balken oder als Tortendiagramm ansehen | [tortendiagramm.md](specs/tortendiagramm.md) | TD-… |
| Ausschnitt der Torte | Die Torte auf eine Kategorie (nach Unterkategorien) oder eine Unterkategorie (nach Posten) eingrenzen; das Stück unter der Maus hebt seine Zeile in der Legende hervor | [torte-ausschnitt.md](specs/torte-ausschnitt.md) | TK-… |
| Übersicht als Antwort | Nach jeder Änderung liefert die API die neue Übersicht mit, die Oberfläche lädt nicht erneut | [antwort-mit-uebersicht.md](specs/antwort-mit-uebersicht.md) | AU-… |

Alle Beträge sind in Euro. Die Oberfläche ist deutschsprachig.

Maßgebliche Kenngröße ist der Monat: Entscheidend ist, was man im Monat mehr oder weniger zur Verfügung hat. Neue
Werte und Berechnungen gehen deshalb vom Monatsbetrag aus, Jahreswerte sind daraus abgeleitet.

## Nicht-Ziele

- Keine einzelnen Buchungen mit Datum, keine Ansicht je Kalendermonat: Ein Posten gilt ohne eigenes Datum für das
  ganze Jahr, als Monats- oder Jahresbetrag.
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
