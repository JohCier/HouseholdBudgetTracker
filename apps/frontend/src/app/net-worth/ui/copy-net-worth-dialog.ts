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
import { Year } from '../../shared/year';
import { NetWorthApi } from '../data-access/net-worth-api';
import { entryCount } from '../net-worth.model';

/**
 * Either the assets and liabilities of a year are taken over, or a year without any takes over
 * those of another year (VS-17).
 */
export type CopyNetWorthDialogData = CopyYearDirection;

/** The years the net worth was taken over from and to, once it is done. */
export type CopiedNetWorth = CopiedYear;

/** Texts and API of the copy dialog for the net worth (VS-15 to VS-17). */
@Injectable({ providedIn: 'root' })
export class NetWorthCopy implements CopyYearKind {
  private readonly api = inject(NetWorthApi);

  titleFrom(sourceYear: Year): string {
    return `Vermögen ${sourceYear} übernehmen`;
  }

  titleTo(targetYear: Year): string {
    return `Vermögen für ${targetYear} übernehmen`;
  }

  optionLabel(year: Year): string {
    return `Vermögen ${year}`;
  }

  takenOver(count: number): string {
    return `${count === 1 ? '1 Wert wird' : count + ' Werte werden'} mit den Beträgen übernommen.`;
  }

  overwriteWarning(targetYear: Year, count: number): string {
    return (
      `Für ${targetYear} ist bereits Vermögen mit ${count} ${count === 1 ? 'Wert' : 'Werten'} ` +
      'erfasst. Es wird überschrieben, die Werte lassen sich nicht wiederherstellen.'
    );
  }

  /** A year without assets and liabilities has no net worth (VS-7). */
  lookUp(year: Year): Observable<YearContent | null> {
    return this.api.getOverview(year).pipe(
      map((overview) => {
        const count = entryCount(overview);
        return count > 0
          ? {
              count,
              copyTo: (targetYear: Year, overwrite: boolean) =>
                this.api.copy(year, { targetYear, overwrite }),
            }
          : null;
      }),
    );
  }
}

/** Opens the dialog; emits the years once copied, `undefined` if cancelled. */
export function openCopyNetWorthDialog(
  dialog: MatDialog,
  data: CopyNetWorthDialogData,
): Observable<CopiedNetWorth | undefined> {
  return openCopyYearDialog(dialog, { ...data, kind: NetWorthCopy });
}

/** The confirmation shown once copied (VS-17). */
export function copiedNetWorthMessage({ sourceYear, targetYear }: CopiedNetWorth): string {
  return `Vermögen ${sourceYear} nach ${targetYear} übernommen.`;
}
