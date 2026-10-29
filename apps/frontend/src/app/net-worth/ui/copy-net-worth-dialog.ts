import { Component, inject, signal } from '@angular/core';
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
import { Year } from '../../budget/budget.model';
import { suggestedSourceYear, suggestedTargetYear } from '../../budget/ui/copy-plan-dialog';
import { FIRST_YEAR, LAST_YEAR } from '../../budget/year';
import { toMessage } from '../../shared/http-error';
import { NetWorthApi } from '../data-access/net-worth-api';
import { entryCount } from '../net-worth.model';

/**
 * Either the assets and liabilities of a year are taken over and the user chooses the target year,
 * or a year without any takes over those of another year and the user chooses which (VS-17).
 */
export type CopyNetWorthDialogData =
  | { sourceYear: Year }
  | { targetYear: Year; sourceYears: readonly Year[] };

/** The years the net worth was taken over from and to, once it is done. */
export interface CopiedNetWorth {
  sourceYear: Year;
  targetYear: Year;
}

/** What is known about the assets and liabilities of a year the user chose. */
type YearCheck =
  | { state: 'invalid' }
  | { state: 'checking' }
  | { state: 'checked'; year: Year; entryCount: number }
  | { state: 'failed' };

/**
 * Asks before the assets and liabilities of a year are taken over into another one (VS-15), like
 * the dialog for plans (PU-13, PU-14): suggests the following year resp. the newest year before the
 * target, names how many entries are taken over and warns, with "Überschreiben" instead of
 * "Übernehmen", if the target year has entries already (VS-16). Closes with both years once copied.
 */
@Component({
  selector: 'app-copy-net-worth-dialog',
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
    <h2 mat-dialog-title>{{ title }}</h2>
    <form [formGroup]="form" (ngSubmit)="copy()">
      <mat-dialog-content>
        @if (choosesSource) {
          <mat-form-field>
            <mat-label>Vorlage</mat-label>
            <mat-select [formControl]="sourceYear">
              @for (year of sourceYears; track year) {
                <mat-option [value]="year">Vermögen {{ year }}</mat-option>
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

        @if (sourceCount(); as count) {
          <p>
            {{ count === 1 ? '1 Wert wird' : count + ' Werte werden' }} mit den Beträgen übernommen.
          </p>
        }
        @if (existingCount(); as count) {
          <p class="warning" role="alert">
            <mat-icon svgIcon="warning" />
            <span>
              Für {{ targetYear.value }} ist bereits Vermögen mit {{ count }}
              {{ count === 1 ? 'Wert' : 'Werten' }} erfasst. Es wird überschrieben, die Werte lassen
              sich nicht wiederherstellen.
            </span>
          </p>
        }
        @if (error(); as message) {
          <p class="error" role="alert">{{ message }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton type="button" mat-dialog-close>Abbrechen</button>
        <button matButton="filled" type="submit" [disabled]="copying() || !ready()">
          {{ existingCount() ? 'Überschreiben' : 'Übernehmen' }}
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
export class CopyNetWorthDialog {
  private readonly api = inject(NetWorthApi);
  private readonly dialogRef =
    inject<MatDialogRef<CopyNetWorthDialog, CopiedNetWorth>>(MatDialogRef);
  private readonly data = inject<CopyNetWorthDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  protected readonly firstYear = FIRST_YEAR;
  protected readonly lastYear = LAST_YEAR;
  protected readonly choosesSource = 'targetYear' in this.data;
  protected readonly sourceYears = 'sourceYears' in this.data ? this.data.sourceYears : [];
  protected readonly title =
    'sourceYear' in this.data
      ? `Vermögen ${this.data.sourceYear} übernehmen`
      : `Vermögen für ${this.data.targetYear} übernehmen`;
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

  /** How many entries are taken over, once looked up; 0 hides the line. */
  protected sourceCount(): number {
    const source = this.source();
    return source.state === 'checked' ? source.entryCount : 0;
  }

  /** How many entries the target year has already, which would be overwritten (VS-16). */
  protected existingCount(): number {
    const target = this.target();
    return target.state === 'checked' ? target.entryCount : 0;
  }

  protected ready(): boolean {
    return this.sourceCount() > 0 && this.target().state === 'checked';
  }

  protected async copy(): Promise<void> {
    const source = this.source();
    const target = this.target();
    if (
      source.state !== 'checked' ||
      source.entryCount === 0 ||
      target.state !== 'checked' ||
      this.copying()
    ) {
      this.targetYear.markAsTouched();
      return;
    }
    this.copying.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.api.copy(source.year, { targetYear: target.year, overwrite: target.entryCount > 0 }),
      );
      this.dialogRef.close({ sourceYear: source.year, targetYear: target.year });
    } catch (error) {
      this.error.set(toMessage(error));
    } finally {
      this.copying.set(false);
    }
  }

  /** Looks up the entries of the year in the control whenever a valid year is chosen. */
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
    return this.api.getOverview(year).pipe(
      map((overview): YearCheck => ({ state: 'checked', year, entryCount: entryCount(overview) })),
      catchError((error: unknown) => {
        this.error.set(toMessage(error));
        return of<YearCheck>({ state: 'failed' });
      }),
      startWith<YearCheck>({ state: 'checking' }),
    );
  }
}

/** Opens the dialog; emits the years once copied, `undefined` if cancelled. */
export function openCopyNetWorthDialog(
  dialog: MatDialog,
  data: CopyNetWorthDialogData,
): Observable<CopiedNetWorth | undefined> {
  return dialog
    .open<CopyNetWorthDialog, CopyNetWorthDialogData, CopiedNetWorth>(CopyNetWorthDialog, { data })
    .afterClosed();
}

/** The confirmation shown once copied (VS-17). */
export function copiedNetWorthMessage({ sourceYear, targetYear }: CopiedNetWorth): string {
  return `Vermögen ${sourceYear} nach ${targetYear} übernommen.`;
}

/** Rejects the year of the source itself (VS-15, like PU-2). */
function notYear(year: Year): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    control.value === year ? { sameYear: true } : null;
}
