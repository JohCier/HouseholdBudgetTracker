import { Component, Type, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  Observable,
  catchError,
  distinctUntilChanged,
  firstValueFrom,
  map,
  of,
  startWith,
  switchMap,
} from 'rxjs';
import { toMessage } from './http-error';
import { FIRST_YEAR, LAST_YEAR, Year } from './year';

/**
 * Either what is recorded for a year is taken over and the user chooses the target year, or a year
 * without anything recorded takes over another year and the user chooses which (PU-12, VS-17).
 */
export type CopyYearDirection =
  { sourceYear: Year } | { targetYear: Year; sourceYears: readonly Year[] };

/** The years something was taken over from and to, once it is done. */
export interface CopiedYear {
  sourceYear: Year;
  targetYear: Year;
}

/** What is recorded for a year: how many items or entries, and how to copy them into another year. */
export interface YearContent {
  count: number;
  copyTo(targetYear: Year, overwrite: boolean): Observable<unknown>;
}

/**
 * What is copied (a household plan, the net worth): its texts and its API. Implementations are
 * injectable services, so the dialog can look them up by their class.
 */
export interface CopyYearKind {
  /** Title when the source year is given, e.g. "Plan 2026 als Vorlage übernehmen". */
  titleFrom(sourceYear: Year): string;
  /** Title when the target year is given, e.g. "Plan für 2027 übernehmen". */
  titleTo(targetYear: Year): string;
  /** A year in the list of sources, e.g. "Plan 2025". */
  optionLabel(year: Year): string;
  /** How much is taken over, e.g. "2 Posten werden übernommen." */
  takenOver(count: number): string;
  /** The warning that the target year is overwritten (PU-14, VS-16). */
  overwriteWarning(targetYear: Year, count: number): string;
  /** What is recorded for the year; `null` if nothing is. */
  lookUp(year: Year): Observable<YearContent | null>;
}

export type CopyYearDialogData = CopyYearDirection & { kind: Type<CopyYearKind> };

/** What is known about a year the user chose. */
type YearCheck =
  | { state: 'invalid' }
  | { state: 'checking' }
  | { state: 'checked'; year: Year; content: YearContent | null }
  | { state: 'failed' };

/**
 * Asks before a plan (PU-13) or the net worth of a year (VS-15) is taken over into another year:
 * for the target year, suggesting the following one (PU-2), or for the year to take over,
 * suggesting the newest one before the target year. Names how much is taken over. Should the
 * target year have something recorded already, it warns that it is overwritten and offers
 * "Überschreiben" instead of "Übernehmen" (PU-14, VS-16). Closes with both years once copied; on
 * an error it stays open and shows the message.
 */
@Component({
  selector: 'app-copy-year-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <!-- Without white space around the text, which would indent its first line -->
    <h2 mat-dialog-title>{{ title }}</h2>
    <form [formGroup]="form" (ngSubmit)="copy()">
      <mat-dialog-content>
        @if (choosesSource) {
          <mat-form-field>
            <mat-label>Vorlage</mat-label>
            <mat-select [formControl]="sourceYear">
              @for (year of sourceYears; track year) {
                <mat-option [value]="year">{{ kind.optionLabel(year) }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        } @else {
          <mat-form-field>
            <mat-label>Zieljahr</mat-label>
            <input
              matInput
              type="number"
              inputmode="numeric"
              [min]="firstYear"
              [max]="lastYear"
              step="1"
              [formControl]="targetYear"
            />
            @if (targetYear.hasError('sameYear')) {
              <mat-error>Bitte ein anderes Jahr als {{ sourceYear.value }} wählen.</mat-error>
            } @else if (targetYear.invalid) {
              <mat-error>Bitte ein Jahr von {{ firstYear }} bis {{ lastYear }} angeben.</mat-error>
            }
          </mat-form-field>
        }

        @if (sourceContent(); as source) {
          <p>{{ kind.takenOver(source.count) }}</p>
        }
        @if (existingContent(); as existing) {
          <p class="warning" role="alert">
            <mat-icon svgIcon="warning" />
            <span>{{ kind.overwriteWarning(targetYear.value!, existing.count) }}</span>
          </p>
        }
        @if (error(); as message) {
          <p class="error" role="alert">{{ message }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton type="button" mat-dialog-close>Abbrechen</button>
        <button matButton="filled" type="submit" [disabled]="copying() || !ready()">
          {{ existingContent() ? 'Überschreiben' : 'Übernehmen' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    mat-dialog-content {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: min(360px, 70vw);
    }
    /* On a low screen the content scrolls; its fields keep their height instead of being squeezed (MA-5) */
    mat-dialog-content > * {
      flex-shrink: 0;
    }
    mat-form-field {
      margin-top: 8px;
    }
    p {
      margin: 0 0 12px;
    }
    .warning {
      display: flex;
      gap: 8px;
      align-items: flex-start;
      padding: 12px;
      border-radius: 12px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    .warning mat-icon {
      flex: none;
    }
    .error {
      color: var(--mat-sys-error);
    }
  `,
})
export class CopyYearDialog {
  private readonly dialogRef = inject<MatDialogRef<CopyYearDialog, CopiedYear>>(MatDialogRef);
  private readonly data = inject<CopyYearDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);
  protected readonly kind = inject(this.data.kind);

  protected readonly firstYear = FIRST_YEAR;
  protected readonly lastYear = LAST_YEAR;
  protected readonly choosesSource = 'targetYear' in this.data;
  protected readonly sourceYears = 'sourceYears' in this.data ? this.data.sourceYears : [];
  protected readonly title =
    'sourceYear' in this.data
      ? this.kind.titleFrom(this.data.sourceYear)
      : this.kind.titleTo(this.data.targetYear);
  protected readonly copying = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly sourceYear: FormControl<Year | null> =
    'sourceYear' in this.data
      ? this.fb.control<Year | null>({ value: this.data.sourceYear, disabled: true })
      : this.fb.control<Year | null>(
          suggestedSourceYear(this.data.targetYear, this.data.sourceYears),
          Validators.required,
        );

  protected readonly targetYear: FormControl<Year | null> =
    'sourceYear' in this.data
      ? this.fb.control<Year | null>(suggestedTargetYear(this.data.sourceYear), [
          Validators.required,
          Validators.min(FIRST_YEAR),
          Validators.max(LAST_YEAR),
          Validators.pattern(/^\d+$/),
          notYear(this.data.sourceYear),
        ])
      : this.fb.control<Year | null>({ value: this.data.targetYear, disabled: true });

  protected readonly form = new FormGroup({ source: this.sourceYear, target: this.targetYear });

  private readonly source = toSignal(this.checked(this.sourceYear), {
    initialValue: { state: 'checking' } as YearCheck,
  });
  private readonly target = toSignal(this.checked(this.targetYear), {
    initialValue: { state: 'checking' } as YearCheck,
  });

  /** What is taken over, once it has been looked up. */
  protected sourceContent(): YearContent | null {
    const source = this.source();
    return source.state === 'checked' ? source.content : null;
  }

  /** What the target year has already and would be overwritten (PU-14, VS-16). */
  protected existingContent(): YearContent | null {
    const target = this.target();
    return target.state === 'checked' ? target.content : null;
  }

  /** Whether both years are known, the source has something recorded and nothing is left to check. */
  protected ready(): boolean {
    return this.sourceContent() !== null && this.target().state === 'checked';
  }

  protected async copy(): Promise<void> {
    const source = this.source();
    const target = this.target();
    if (
      source.state !== 'checked' ||
      !source.content ||
      target.state !== 'checked' ||
      this.copying()
    ) {
      this.targetYear.markAsTouched();
      return;
    }
    this.copying.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(source.content.copyTo(target.year, target.content !== null));
      this.dialogRef.close({ sourceYear: source.year, targetYear: target.year });
    } catch (error) {
      this.error.set(toMessage(error));
    } finally {
      this.copying.set(false);
    }
  }

  /** Looks up the year in the control whenever a valid year is chosen. */
  private checked(control: FormControl<Year | null>): Observable<YearCheck> {
    return control.valueChanges.pipe(
      startWith(control.value),
      map(() => (control.valid || control.disabled ? control.value : null)),
      distinctUntilChanged(),
      switchMap((year) =>
        year === null ? of<YearCheck>({ state: 'invalid' }) : this.lookUp(year),
      ),
    );
  }

  private lookUp(year: Year): Observable<YearCheck> {
    return this.kind.lookUp(year).pipe(
      map((content): YearCheck => ({ state: 'checked', year, content })),
      catchError((error: unknown) => {
        this.error.set(toMessage(error));
        return of<YearCheck>({ state: 'failed' });
      }),
      startWith<YearCheck>({ state: 'checking' }),
    );
  }
}

/** Opens the dialog; emits the years once copied, `undefined` if cancelled. */
export function openCopyYearDialog(
  dialog: MatDialog,
  data: CopyYearDialogData,
): Observable<CopiedYear | undefined> {
  return dialog
    .open<CopyYearDialog, CopyYearDialogData, CopiedYear>(CopyYearDialog, { data })
    .afterClosed();
}

/** The year suggested as target: the following year, for the last year the one before (PU-2). */
export function suggestedTargetYear(sourceYear: Year): Year {
  return sourceYear < LAST_YEAR ? sourceYear + 1 : sourceYear - 1;
}

/** The year suggested as source: the newest before the target year, otherwise the oldest after it (PU-13). */
export function suggestedSourceYear(targetYear: Year, sourceYears: readonly Year[]): Year | null {
  const before = sourceYears.filter((year) => year < targetYear);
  if (before.length > 0) {
    return Math.max(...before);
  }
  const after = sourceYears.filter((year) => year > targetYear);
  return after.length > 0 ? Math.min(...after) : null;
}

/** Rejects the given year, the year of the source itself (PU-2, VS-15). */
function notYear(year: Year): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    control.value === year ? { sameYear: true } : null;
}
