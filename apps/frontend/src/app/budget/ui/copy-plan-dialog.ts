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
import { toMessage } from '../../shared/http-error';
import { Year } from '../budget.model';
import { BudgetApi } from '../data-access/budget-api';
import { FIRST_YEAR, LAST_YEAR } from '../year';

/**
 * Either the plan of a year is taken over and the user chooses the target year, or a year
 * without plan takes over one of the user's plans and they choose which (PU-12).
 */
export type CopyPlanDialogData =
  | { sourceYear: Year }
  | { targetYear: Year; sourceYears: readonly Year[] };

/** The years a plan was taken over from and to, once it is done. */
export interface CopiedPlan {
  sourceYear: Year;
  targetYear: Year;
}

/** What is known about the plan of a year the user chose. */
type YearCheck =
  | { state: 'invalid' }
  | { state: 'checking' }
  | { state: 'checked'; year: Year; plan: { id: string; itemCount: number } | null }
  | { state: 'failed'; message: string };

/**
 * Asks before a plan is taken over as template (PU-13): for the target year, suggesting the
 * following one (PU-2), or for the plan to take over, suggesting the newest one before the
 * target year. Names how many items are taken over. Should the target year have a plan
 * already, it warns that the plan is overwritten and offers "Überschreiben" instead of
 * "Übernehmen" (PU-14). Closes with both years once the plan has been copied; on an error it
 * stays open and shows the message.
 */
@Component({
  selector: 'app-copy-plan-dialog',
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
                <mat-option [value]="year">Plan {{ year }}</mat-option>
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

        @if (sourcePlan(); as plan) {
          <p>{{ plan.itemCount }} Posten werden übernommen.</p>
        }
        @if (existingPlan(); as plan) {
          <p class="warning" role="alert">
            <mat-icon svgIcon="warning" />
            <span>
              Für {{ targetYear.value }} gibt es bereits einen Haushaltsplan mit
              {{ plan.itemCount }} Posten. Er wird überschrieben, die Posten lassen sich nicht
              wiederherstellen.
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
          {{ existingPlan() ? 'Überschreiben' : 'Übernehmen' }}
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
export class CopyPlanDialog {
  private readonly api = inject(BudgetApi);
  private readonly dialogRef = inject<MatDialogRef<CopyPlanDialog, CopiedPlan>>(MatDialogRef);
  private readonly data = inject<CopyPlanDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  protected readonly firstYear = FIRST_YEAR;
  protected readonly lastYear = LAST_YEAR;
  protected readonly choosesSource = 'targetYear' in this.data;
  protected readonly sourceYears = 'sourceYears' in this.data ? this.data.sourceYears : [];
  protected readonly title =
    'sourceYear' in this.data
      ? `Plan ${this.data.sourceYear} als Vorlage übernehmen`
      : `Plan für ${this.data.targetYear} übernehmen`;
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

  /** The plan to take over, once it has been looked up. */
  protected sourcePlan(): { id: string; itemCount: number } | null {
    const source = this.source();
    return source.state === 'checked' ? source.plan : null;
  }

  /** The plan the target year has already and that would be overwritten (PU-14). */
  protected existingPlan(): { itemCount: number } | null {
    const target = this.target();
    return target.state === 'checked' ? target.plan : null;
  }

  /** Whether both years are known, the source has a plan and nothing is left to check. */
  protected ready(): boolean {
    return (
      this.source().state === 'checked' &&
      this.sourcePlan() !== null &&
      this.target().state === 'checked'
    );
  }

  protected async copy(): Promise<void> {
    const source = this.source();
    const target = this.target();
    if (
      source.state !== 'checked' ||
      !source.plan ||
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
        this.api.copyPlan(source.plan.id, {
          targetYear: target.year,
          overwrite: target.plan !== null,
        }),
      );
      this.dialogRef.close({ sourceYear: source.year, targetYear: target.year });
    } catch (error) {
      this.error.set(toMessage(error));
    } finally {
      this.copying.set(false);
    }
  }

  /** Looks up the plan of the year in the control whenever a valid year is chosen. */
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
      map(
        (overview): YearCheck => ({
          state: 'checked',
          year,
          plan: overview ? { id: overview.id, itemCount: overview.items.length } : null,
        }),
      ),
      catchError((error: unknown) => {
        this.error.set(toMessage(error));
        return of<YearCheck>({ state: 'failed', message: toMessage(error) });
      }),
      startWith<YearCheck>({ state: 'checking' }),
    );
  }
}

/** Opens the dialog; emits the years once the plan has been copied, `undefined` if cancelled. */
export function openCopyPlanDialog(
  dialog: MatDialog,
  data: CopyPlanDialogData,
): Observable<CopiedPlan | undefined> {
  return dialog
    .open<CopyPlanDialog, CopyPlanDialogData, CopiedPlan>(CopyPlanDialog, { data })
    .afterClosed();
}

/** The confirmation shown once a plan has been copied (PU-15). */
export function copiedMessage({ sourceYear, targetYear }: CopiedPlan): string {
  return `Plan ${sourceYear} nach ${targetYear} übernommen.`;
}

/** The year suggested as target: the following year, for the last year the one before (PU-2). */
export function suggestedTargetYear(sourceYear: Year): Year {
  return sourceYear < LAST_YEAR ? sourceYear + 1 : sourceYear - 1;
}

/** The plan suggested as source: the newest before the target year, otherwise the oldest after it (PU-13). */
export function suggestedSourceYear(targetYear: Year, sourceYears: readonly Year[]): Year | null {
  const before = sourceYears.filter((year) => year < targetYear);
  if (before.length > 0) {
    return Math.max(...before);
  }
  const after = sourceYears.filter((year) => year > targetYear);
  return after.length > 0 ? Math.min(...after) : null;
}

/** Rejects the given year, the year of the source itself (PU-2). */
function notYear(year: Year): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    control.value === year ? { sameYear: true } : null;
}
