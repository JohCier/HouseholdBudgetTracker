# Top 5: Sparpotential und Ausgaben

Status: geplant

## Zweck

Die Karte „Einträge" zeigt alle Posten nach Art, Kategorie und Unterkategorie. Wo die größten Hebel liegen, sieht der
Benutzer dort nur, wenn er die ganze Liste durchgeht. Der Kasten „Sparpotential je Unterkategorie"
([sparpotential-unterkategorie.md](sparpotential-unterkategorie.md)) fasst das Sparpotential nach Unterkategorien
zusammen, nennt aber keine einzelnen Posten, und für die höchsten Ausgaben gibt es keinen Überblick.

Mit dieser Spec steht unter der Karte „50/30/20-Regel" eine Karte „Top 5". Ein Umschalter wie „Balken | Torte"
(TD-1) wechselt zwischen den fünf Ausgaben mit dem höchsten Sparpotential und den fünf höchsten Ausgaben.

## Fachliche Regeln

### Listen

- **TP-1**: „Sparpotential" listet die bis zu fünf Ausgaben mit dem höchsten Sparpotential (PM-2a), aus allen
  Kategorien der 50/30/20-Regel. Ausgaben ohne Sparpotential stehen nicht darin.
- **TP-2**: „Ausgaben" listet die bis zu fünf Ausgaben mit dem höchsten Monatsbetrag (MJ-2) aus Lebensunterhalt und
  Freizeit & Spaß. Ausgaben in Sparen & Investieren stehen nicht darin, weil sie keine Ausgaben sind, die man senken
  möchte. Einnahmen stehen in keiner der beiden Listen.
- **TP-3**: Maßgeblich ist der Monatsbetrag: Eine Ausgabe „Kfz-Versicherung" 600,00 € jährlich zählt mit 50,00 €.
  Der Jahreswert ist das Zwölffache.
- **TP-4**: Geordnet wird nach dem Monatsbetrag, der größte oben; bei gleichem Betrag alphabetisch nach Beschreibung.
  Gibt es weniger als fünf passende Ausgaben, zeigt die Liste entsprechend weniger Zeilen.
- **TP-5**: Das Backend stellt beide Listen zusammen und liefert sie mit jeder Übersicht (AU-1). Die Oberfläche
  ordnet und rechnet nicht selbst.
- **TP-6**: Die Listen haben keinen Einfluss auf Saldo und 50/30/20-Auswertung (PM-8).

### Darstellung

- **TP-7**: Die Karte „Top 5" steht unter der Karte „50/30/20-Regel" und ist da, sobald es einen Plan gibt. Ein
  Umschalter „Sparpotential | Ausgaben" im Stil von „Balken | Torte" wechselt die Liste. Beim Öffnen der Planansicht
  ist „Sparpotential" gewählt; beim Wechsel des Jahres bleibt die Wahl erhalten, über das Neuladen der Seite hinaus
  nicht (wie TD-1).
- **TP-8**: Jede Zeile zeigt links ihren Rang (1. bis 5.), die Beschreibung und klein darunter die Unterkategorie
  („Sonstiges" ohne Unterkategorie, UK-1), rechtsbündig daneben Monats- und Jahreswert in den Spalten „Monat" und
  „Jahr" wie in der Bilanz (BI-2).
- **TP-9**: In der Liste „Sparpotential" sind Monats- und Jahreswert die des Sparpotentials. Unter der Beschreibung
  steht zusätzlich klein der Monatsbetrag der Ausgabe: „Abonnements · von 15,00 €".
- **TP-10**: Ohne Ausgabe mit Sparpotential zeigt „Sparpotential" den Hinweis „Noch kein Sparpotential markiert",
  ohne passende Ausgabe zeigt „Ausgaben" den Hinweis „Noch keine Ausgaben".
- **TP-11**: Auf schmaler Karte (auf dem Handy, auch mit vergrößerter Schrift) steht die Beschreibung oben und die
  Beträge darunter in ihren Spalten, wie bei den Posten (MA-2). Lange Beschreibungen brechen nur zwischen Wörtern um
  (MA-4), und die Seite ist ab 360 px nicht breiter als der Bildschirm (MA-1).
- **TP-12**: Jede Änderung (Posten erfassen, bearbeiten, löschen, rückgängig, Sparpotential setzen oder entfernen)
  zeigt sich sofort in der Liste, ohne Neuladen (AU-1).

## Akzeptanzkriterien

- **Gegeben** die Ausgaben „Streaming" 15,00 € monatlich (Unterkategorie „Abonnements", Sparpotential 5,00 €),
  „Fitnessstudio" 360,00 € jährlich („Hobbys", Sparpotential 30,00 €) und „Miete" 1.000,00 € ohne Sparpotential,
  **wenn** der Benutzer die Planansicht öffnet, **dann** ist in der Karte „Top 5" „Sparpotential" gewählt, und sie
  zeigt „1. Fitnessstudio" mit 30,00 € und 360,00 € und darunter „Hobbys · von 30,00 €", dann
  „2. Streaming" mit 5,00 € und 60,00 €; „Miete" steht nicht darin (TP-1, TP-3, TP-4, TP-7 bis TP-9).
- **Gegeben** sieben Ausgaben in Lebensunterhalt und Freizeit & Spaß und „ETF-Sparplan" 500,00 € in Sparen &
  Investieren, **wenn** der Benutzer auf „Ausgaben" umschaltet, **dann** zeigt die Liste die fünf höchsten der sieben
  nach Monatsbetrag, ohne „ETF-Sparplan" (TP-2, TP-4).
- **Gegeben** „Kfz-Versicherung" 600,00 € jährlich und „Handy" 40,00 € monatlich, **dann** steht in „Ausgaben"
  „Kfz-Versicherung" mit 50,00 € und 600,00 € über „Handy" (TP-3, TP-4).
- **Gegeben** „Kino" und „Bücher" mit je 20,00 €, **dann** steht in „Ausgaben" „Bücher" über „Kino" (TP-4).
- **Gegeben** ein Plan ohne Sparpotential, **dann** zeigt „Sparpotential" „Noch kein Sparpotential markiert"; setzt
  der Benutzer bei einer Ausgabe ein Sparpotential, steht sie sofort in der Liste (TP-10, TP-12).
- **Gegeben** ein leerer Plan, **wenn** der Benutzer auf „Ausgaben" umschaltet, **dann** steht dort „Noch keine
  Ausgaben" (TP-10).
- **Gegeben** „Ausgaben" ist gewählt, **wenn** der Benutzer ins Vorjahr wechselt, **dann** bleibt „Ausgaben" gewählt
  (TP-7).
- **Gegeben** ein 390 px breiter Bildschirm, **dann** stehen in der Karte „Top 5" die Beträge unter „Monat" und
  „Jahr", und die Seite ist nicht breiter als der Bildschirm (TP-11).

## Nicht im Umfang

- Klicken auf eine Zeile, um zum Posten in der Karte „Einträge" zu springen oder ihn zu bearbeiten
- Eine andere Anzahl als fünf oder eine vom Benutzer gewählte Anzahl
- Top-Listen der Einnahmen oder je Kategorie, Ausgaben in Sparen & Investieren in der Liste „Ausgaben"
- Top 5 in der Jahresübersicht oder über mehrere Jahre
- Speichern der gewählten Liste über das Neuladen der Seite hinaus

## Umsetzung

Folgt nach der Freigabe.

## Offene Fragen

Keine. Entschieden mit dem Benutzer: eigene Karte unter der 50/30/20-Karte mit Umschalter „Sparpotential | Ausgaben"
(TP-7), einzelne Posten statt Unterkategorien (TP-1, TP-2), nach Monatsbetrag geordnet mit Monat und Jahr (TP-3,
TP-8), „Ausgaben" ohne Sparen & Investieren (TP-2), Ausgabebetrag neben dem Sparpotential (TP-9), Hinweise bei leerer
Liste und „Sparpotential" beim Öffnen gewählt (TP-7, TP-10).
