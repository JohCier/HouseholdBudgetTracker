---
paths:
  - "apps/frontend/src/**"
---

# Regeln für das Frontend

- Aufbau: je Feature ein Ordner (`auth/`, `budget/`, `net-worth/`) mit `data-access/` (API, Signal-Store) und `ui/`
  (Komponenten); was mehr als ein Feature braucht, liegt in `shared/`.
- Keine Hilfen quer über Features: Ein Feature importiert aus einem anderen nur fertige Komponenten, die es als Ganzes
  einbettet (z. B. `NetWorthCard` in der Jahresübersicht). Funktionen, Validatoren, Typen und Konstanten, die ein
  zweites Feature braucht, ziehen beim ersten Mal nach `shared/` um. Beispiele: `shared/year.ts` (`Year`, `FIRST_YEAR`,
  `currentYear`), `shared/http-error.ts`.
- Gleicher Dialog für verschiedene Dinge: Ein Dialog, den es für ein zweites Feature fast gleich braucht, wird
  einmal in `shared/` gebaut und bekommt das Unterschiedliche (Texte, API) als injizierbaren Dienst. Muster ist
  `shared/copy-year-dialog.ts`: `CopyYearKind` mit `PlanCopy` und `NetWorthCopy`. Wer ein weiteres „Jahr übernehmen"
  braucht, schreibt nur eine weitere `CopyYearKind`.
- Tests des gemeinsamen Teils stehen einmal beim Teil selbst bzw. bei der ersten Verwendung; jede weitere Verwendung
  testet nur ihre eigenen Texte und API-Aufrufe (z. B. `copy-net-worth-dialog.spec.ts`).
