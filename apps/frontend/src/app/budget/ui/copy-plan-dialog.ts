import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import {
  CopiedYear,
  CopyYearDirection,
  CopyYearKind,
  YearContent,
  openCopyYearDialog,
} from '../../shared/copy-year-dialog';
import { Year } from '../budget.model';
import { BudgetApi } from '../data-access/budget-api';

/** Either the plan of a year is taken over, or a year without plan takes over one of the user's plans (PU-12). */
export type CopyPlanDialogData = CopyYearDirection;

/** The years a plan was taken over from and to, once it is done. */
export type CopiedPlan = CopiedYear;

/** Texts and API of the copy dialog for household plans (PU-12 to PU-14). */
@Injectable({ providedIn: 'root' })
export class PlanCopy implements CopyYearKind {
  private readonly api = inject(BudgetApi);

  titleFrom(sourceYear: Year): string {
    return `Plan ${sourceYear} als Vorlage übernehmen`;
  }

  titleTo(targetYear: Year): string {
    return `Plan für ${targetYear} übernehmen`;
  }

  optionLabel(year: Year): string {
    return `Plan ${year}`;
  }

  takenOver(count: number): string {
    return `${count} Posten werden übernommen.`;
  }

  overwriteWarning(targetYear: Year, count: number): string {
    return (
      `Für ${targetYear} gibt es bereits einen Haushaltsplan mit ${count} Posten. ` +
      'Er wird überschrieben und unter „Gelöschte Pläne" abgelegt.'
    );
  }

  /** A plan without items is a plan as well, which a copy would overwrite (PU-14). */
  lookUp(year: Year): Observable<YearContent | null> {
    return this.api.getOverview(year).pipe(
      map((overview) =>
        overview
          ? {
              count: overview.items.length,
              copyTo: (targetYear: Year, overwrite: boolean) =>
                this.api.copyPlan(overview.id, { targetYear, overwrite }),
            }
          : null,
      ),
    );
  }
}

/** Opens the dialog; emits the years once the plan has been copied, `undefined` if cancelled. */
export function openCopyPlanDialog(
  dialog: MatDialog,
  data: CopyPlanDialogData,
): Observable<CopiedPlan | undefined> {
  return openCopyYearDialog(dialog, { ...data, kind: PlanCopy });
}

/** The confirmation shown once a plan has been copied (PU-15). */
export function copiedMessage({ sourceYear, targetYear }: CopiedPlan): string {
  return `Plan ${sourceYear} nach ${targetYear} übernommen.`;
}
