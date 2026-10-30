# Aufbau der Planansicht: Überblick oben, Tabs darunter

Status: umgesetzt

## Zweck

Die Planansicht ist mit jedem Feature gewachsen. Heute stehen links das Formular „Neuer Eintrag“, die Karte
„50/30/20-Regel“ (umschaltbar zwischen Balken und Torte) und „Top 5“, rechts die Einträge und darunter die Einmalposten.
Oben stehen die Bilanz und daneben das Sparpotential je Unterkategorie. Wer die Seite öffnet, sieht viele Zahlen auf
einmal und muss die Einmalposten unter einer langen Liste von Einträgen suchen. Die Torte ist in der schmalen linken
Spalte klein.

Diese Spec ordnet die Seite neu: Oben steht nur noch der Überblick (Bilanz und 50/30/20-Regel). Alles Weitere liegt
darunter in Tabs: „Regelmäßige Posten“, „Einmalposten“, „Detailansicht“ (die Torte, größer als bisher) und
„Sparhebel“ (Sparpotential je Unterkategorie und die beiden Top-5-Listen). Posten erfasst der Benutzer über einen
Dialog statt über ein dauerhaft sichtbares Formular.

Diese Spec ändert TD-1 in [tortendiagramm.md](tortendiagramm.md), TK-5 in [torte-ausschnitt.md](torte-ausschnitt.md),
TS-1 und TS-6 in [torte-sortierung.md](torte-sortierung.md), SU-6 bis SU-8 in
[sparpotential-unterkategorie.md](sparpotential-unterkategorie.md), TP-7 in [top-5.md](top-5.md), EG-12 in
[eintraege-gruppen.md](eintraege-gruppen.md), MJ-11 in [monats-jahresbetrag.md](monats-jahresbetrag.md) (Formular
„Neuer Eintrag“) sowie EP-8 und EP-10 in [einmalposten.md](einmalposten.md). Wie die Einmalposten innerhalb ihres Tabs
aussehen und sortiert werden, regelt eine eigene Spec.

## Fachliche Regeln

### Überblick oben

- **AP-1**: Unter der Kopfzeile mit Jahr, „Als Vorlage übernehmen“ und „Plan löschen“ stehen nebeneinander die Karte
  „Bilanz“ (BI-1 bis BI-5) und die Karte „50/30/20-Regel“. Beide Spalten sind gleich breit und gleich hoch; die
  Warnung „Deine Ausgaben übersteigen deine Einnahmen.“ (BI-6) steht unter der Bilanz in derselben Spalte.
- **AP-2**: Die Karte „50/30/20-Regel“ zeigt nur noch die Balken mit Prozentangaben. Den Umschalter „Balken | Torte“
  gibt es nicht mehr; die Torte steht im Tab „Detailansicht“ (AP-9). Ändert TD-1.
- **AP-3**: Der Kasten „Sparpotential je Unterkategorie“ steht nicht mehr neben der Bilanz, sondern im Tab „Sparhebel“
  (AP-11). Die Zeile „Sparpotential“ der Bilanz (PM-14) bleibt. Ändert SU-6 bis SU-8.
- **AP-17**: Sind die Ausgaben nicht höher als die Einnahmen, steht unter der Bilanz statt der Warnung die Meldung
  „Deine Einnahmen decken deine Ausgaben. Im Monat bleiben dir ‹Saldo je Monat› übrig.“ mit einem Haken, abgesetzt in
  der Farbe der Einnahmen. So steht unter der Bilanz immer eine Aussage, ob der Plan aufgeht.
- **AP-4**: Auf schmalen Bildschirmen (unter 860 px, auf dem Handy) stehen Bilanz, Warnung und 50/30/20-Regel
  untereinander, darunter die Tabs. Die Seite ist ab 360 px nicht breiter als der Bildschirm (MA-1).

### Tabs

- **AP-5**: Unter dem Überblick stehen die Tabs „Regelmäßige Posten“, „Einmalposten“, „Detailansicht“ und
  „Sparhebel“, in dieser Reihenfolge. Es ist immer genau ein Tab sichtbar. Beim Öffnen der Planansicht ist
  „Regelmäßige Posten“ gewählt. Beim Wechsel des Jahres bleibt die Wahl erhalten, über das Neuladen der Seite hinaus
  nicht (wie TD-1).
- **AP-6**: Passen nicht alle Tabs nebeneinander auf den Bildschirm, lassen sie sich seitlich verschieben; die Seite
  selbst scrollt nicht waagerecht (MA-1).
- **AP-7**: „Regelmäßige Posten“ zeigt oben den Knopf „Posten erfassen“ und darunter die Karte „Einträge“ unverändert
  (UK-…, EG-…, EF-…, PM-…).
- **AP-8**: „Einmalposten“ zeigt den Abschnitt „Einmalposten“ (EP-8, EP-9, EP-11) mit dem Knopf „Einmalposten
  erfassen“. Ändert „unter den Einträgen“ in EP-8.
- **AP-9**: „Detailansicht“ zeigt die Torte mit „Ausschnitt“, „Sortierung“ und Legende (TD-2 bis TD-9, TK-…, TS-…) in
  einer Karte über die ganze Breite. Auf breiten Bildschirmen steht die Torte links und bis zu 360 px groß, die Legende
  rechts daneben; auf schmalen Bildschirmen steht die Legende unter der Torte.
- **AP-10**: Beim Öffnen der Planansicht ist in der Detailansicht „Betrag“ absteigend als Sortierung gewählt statt
  „Einträge“. Ändert TS-1. Ausschnitt und Sortierung bleiben beim Wechsel der Tabs und des Jahres erhalten (TK-5, TS-6
  mit „Tabs wechseln“ statt „zwischen Balken und Torte wechseln“).
- **AP-11**: „Sparhebel“ zeigt drei Karten nebeneinander: „Sparpotential je Unterkategorie“ (SU-1 bis SU-5), „Top 5
  Sparpotential“ und „Top 5 Ausgaben“ (TP-1 bis TP-6, TP-8 bis TP-12). Die beiden Top-5-Listen sind eigene Karten ohne
  Umschalter (ändert TP-7). Auf mittleren und schmalen Bildschirmen (unter 1100 px) stehen die Karten untereinander.
- **AP-12**: Die Karte „Sparpotential je Unterkategorie“ hat eine feste Höchsthöhe; reichen die Zeilen darüber
  hinaus, scrollt die Liste innerhalb der Karte, die Spaltenköpfe „Monat“ und „Jahr“ bleiben stehen. Ohne Ausgaben mit
  Sparpotential zeigt sie „Noch kein Sparpotential markiert“ (statt wegzufallen wie in SU-7).

### Jahr ohne Plan

- **AP-13**: In einem Jahr ohne Haushaltsplan gibt es keinen Überblick. Es gibt nur die Tabs „Regelmäßige Posten“ und
  „Einmalposten“. „Regelmäßige Posten“ zeigt den Hinweis „Für ‹Jahr› gibt es noch keinen Haushaltsplan.“ mit
  „Haushaltsplan anlegen“ und „Aus anderem Jahr übernehmen“ (PU-…). Ändert EP-10: Die Einmalposten stehen im eigenen Tab
  statt unter dem Hinweis. War „Detailansicht“ oder „Sparhebel“ gewählt, ist beim Wechsel in ein Jahr ohne Plan
  „Regelmäßige Posten“ gewählt.

### Posten erfassen

- **AP-14**: Das Formular „Neuer Eintrag“ neben den Einträgen entfällt. Der Knopf „Posten erfassen“ (AP-7) öffnet den
  Dialog „Posten erfassen“ mit den Feldern des bisherigen Formulars (PO-…, MJ-11, UK-6, PN-…): Art „Ausgabe“ und
  „Monatlich“ sind gewählt, alle anderen Felder leer, der Fokus steht in „Beschreibung“. Ändert EG-12.
- **AP-15**: „Hinzufügen“ prüft und speichert wie der Dialog „Neuer Eintrag in ‹Gruppe›“ (EG-11): Der Dialog schließt,
  der Posten steht unten in seiner Gruppe (EG-1), die Meldung „Eintrag hinzugefügt“ erscheint. „Abbrechen“ und Escape
  legen nichts an; schlägt das Speichern fehl, bleibt der Dialog mit den Eingaben offen und zeigt den Fehler.
- **AP-16**: Der Knopf „Posten hinzufügen“ in den Summenzeilen der Gruppen (EG-9, EG-10) bleibt unverändert.

## Akzeptanzkriterien

- **Gegeben** ein Plan, **wenn** der Benutzer die Planansicht auf einem breiten Bildschirm öffnet, **dann** stehen
  oben „Bilanz“ und „50/30/20-Regel“ nebeneinander und gleich hoch, darunter die Tabs „Regelmäßige Posten“,
  „Einmalposten“, „Detailansicht“, „Sparhebel“, und „Regelmäßige Posten“ ist gewählt (AP-1, AP-5).
- **Gegeben** ein Plan, **dann** hat die Karte „50/30/20-Regel“ keinen Umschalter „Balken | Torte“, und es gibt kein
  Formular „Neuer Eintrag“ auf der Seite (AP-2, AP-14).
- **Gegeben** Ausgaben über den Einnahmen, **dann** steht die Warnung unter der Bilanz (AP-1).
- **Gegeben** Einnahmen 4.455,00 € und Ausgaben 2.515,00 € im Monat, **dann** steht unter der Bilanz „Deine Einnahmen
  decken deine Ausgaben. Im Monat bleiben dir 1.940,00 € übrig.“ mit einem Haken (AP-17).
- **Gegeben** „Kaltmiete“ 950,00 €, „Lebensmittel“ 450,00 € und „Handy“ 30,00 € im Monat in Lebensunterhalt mit den
  Unterkategorien „Wohnen“, „Essen“ und „Kommunikation“, **wenn** der Benutzer „Detailansicht“ wählt und den Ausschnitt
  „Lebensunterhalt“ einstellt, **dann** ist „Betrag“ gewählt, und die Legende zeigt „Wohnen“, „Essen“,
  „Kommunikation“ (AP-9, AP-10).
- **Gegeben** in der Detailansicht der Ausschnitt „Lebensunterhalt“, **wenn** der Benutzer zu „Regelmäßige Posten“
  und zurück wechselt oder das Jahr wechselt, **dann** ist weiterhin „Lebensunterhalt“ gewählt (AP-10).
- **Gegeben** Ausgaben mit Sparpotential, **wenn** der Benutzer „Sparhebel“ wählt, **dann** stehen dort
  „Sparpotential je Unterkategorie“, „Top 5 Sparpotential“ und „Top 5 Ausgaben“ nebeneinander, und neben der Bilanz
  steht kein Kasten „Sparpotential je Unterkategorie“ mehr (AP-3, AP-11).
- **Gegeben** zwölf Unterkategorien mit Sparpotential, **dann** scrollt die Liste „Sparpotential je Unterkategorie“
  innerhalb ihrer Karte (AP-12).
- **Gegeben** ein Plan ohne Sparpotential, **wenn** der Benutzer „Sparhebel“ wählt, **dann** zeigen „Sparpotential je
  Unterkategorie“ und „Top 5 Sparpotential“ „Noch kein Sparpotential markiert“ (AP-12, TP-10).
- **Gegeben** ein Plan, **wenn** der Benutzer „Einmalposten“ wählt, **dann** sieht er die Einmalposten des Jahres und
  „Einmalposten erfassen“, aber nicht die Karte „Einträge“ (AP-8).
- **Gegeben** kein Plan für 2027, **wenn** der Benutzer `/jahre/2027` öffnet, **dann** gibt es keine Bilanz, nur die
  Tabs „Regelmäßige Posten“ mit „Für 2027 gibt es noch keinen Haushaltsplan.“ und „Einmalposten“ (AP-13).
- **Gegeben** „Sparhebel“ ist im Plan 2026 gewählt, **wenn** der Benutzer zu 2027 ohne Plan wechselt, **dann** ist
  „Regelmäßige Posten“ gewählt (AP-13).
- **Gegeben** ein Plan, **wenn** der Benutzer „Posten erfassen“ wählt, **dann** öffnet sich der Dialog „Posten
  erfassen“ mit „Ausgabe“ und „Monatlich“ und dem Fokus in „Beschreibung“; nach „Sprit“, 120,00 €, Kategorie
  „Lebensunterhalt“, Unterkategorie „Auto“ und „Hinzufügen“ schließt er, und „Sprit“ steht unten in „Auto“ (AP-14,
  AP-15).
- **Gegeben** der Dialog „Posten erfassen“, **wenn** der Benutzer „Abbrechen“ wählt, **dann** wird nichts angelegt
  (AP-15).
- **Gegeben** ein 390 px breiter Bildschirm, **dann** stehen Bilanz, 50/30/20-Regel und Tabs untereinander, und die
  Seite ist nicht breiter als der Bildschirm (AP-4, AP-6).

## Nicht im Umfang

- Einmalposten nach Einnahmen und Ausgaben trennen, verschieben und mit „Rückgängig“ löschen; dafür gibt es eine eigene
  Spec
- Den gewählten Tab über das Neuladen hinaus speichern oder in der Adresse führen
- Änderungen an Inhalt und Berechnung von Bilanz, 50/30/20-Regel, Torte, Sparpotential und Top 5
- Die Jahresübersicht

## Umsetzung

- Nur Frontend; API und Backend bleiben unverändert
- `budget/budget-dashboard.ts`: oben `.overview` mit `app-overview-summary` und `app-category-breakdown` in zwei
  gleich hohen Spalten, unter 860 px untereinander (AP-1, AP-4); darunter `mat-tab-group` mit den Tabs aus
  `PLAN_TABS`, gewählt über das Signal `tab`, `tabIndex` fällt in einem Jahr ohne Plan auf den ersten Tab zurück
  (AP-5, AP-13). „Detailansicht“ zeigt `app-category-pie`, Sortierung beginnt mit `AMOUNT_SORT` (AP-9, AP-10);
  „Sparhebel“ zeigt `.levers` mit drei Karten, unter 1100 px untereinander (AP-11). „Posten erfassen“ öffnet
  `AddItemDialog` ohne Preset (AP-14)
- `ui/overview-summary.ts`: ohne Kasten „Sparpotential je Unterkategorie“ (AP-3); Meldung mit Haken `check_circle`
  ohne Defizit (AP-17)
- `ui/category-breakdown.ts`: nur noch Balken, ohne Umschalter (AP-2)
- `ui/top-items.ts`: eine Karte je Liste (`list`, `items`), ohne Umschalter (AP-11)
- `ui/saving-potential-summary.ts`: Höchsthöhe 320 px mit scrollender Liste und stehenden Spaltenköpfen, Hinweis ohne
  Sparpotential (AP-12)
- `ui/add-item-dialog.ts`: ohne Preset Titel „Posten erfassen“, Art „Ausgabe“, leere Felder (AP-14, AP-15);
  `ui/add-item-form.*` entfällt
- `ui/category-pie.scss`: ab 861 px Torte bis 360 px links, Legende rechts (AP-9)
- Tests: `budget-dashboard.spec.ts`, `add-item-dialog.spec.ts`, `overview-summary.spec.ts`,
  `category-breakdown.spec.ts`, `top-items.spec.ts`, `saving-potential-summary.spec.ts`
- E2E: `e2e/tests/planansicht-aufbau.spec.ts` (AP-1, AP-4 bis AP-9, AP-11, AP-13, AP-17); AP-2 in
  `tortendiagramm.spec.ts`, AP-3, AP-11 und AP-12 in `sparpotential-unterkategorie.spec.ts`, AP-10 in
  `torte-sortierung.spec.ts`, AP-14 und AP-15 in `posten.spec.ts` und `monats-jahresbetrag.spec.ts`, AP-16 in
  `eintraege-gruppen.spec.ts`; die übrigen E2E-Tests erfassen Posten über den Dialog (`openAddItemDialog`) und
  wechseln die Tabs mit `showTab`

## Offene Fragen

Keine. Entschieden mit JC anhand der Screenshots: Bilanz und 50/30/20-Regel oben, die drei Hebel-Karten im Tab
„Sparhebel“ statt oben (AP-3, AP-11), die Torte als „Detailansicht“ (AP-9), Standardsortierung der Torte „Betrag“
(AP-10), im Jahr ohne Plan der Hinweis im Tab „Regelmäßige Posten“ (AP-13), der Dialog „Posten erfassen“ startet mit
„Ausgabe“ (AP-14), eine Meldung mit Haken auch bei aufgehendem Plan (AP-17), Einmalposten in einer eigenen Spec.
