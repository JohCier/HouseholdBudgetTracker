import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { Observable, catchError, firstValueFrom, map, of, startWith } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import { Year } from '../budget.model';
import { BudgetApi } from '../data-access/budget-api';

/** A deleted plan, as far as restoring it needs to know. */
export interface DeletedPlanRef {
  planId: string;
  year: Year;
  itemCount: number;
}

/** What is known about the active plan the restored one would replace. */
type ActiveCheck =
  | { state: 'checking' }
  | { state: 'checked'; itemCount: number | null }
  | { state: 'failed' };

/**
 * Asks before a deleted plan replaces the active plan of its year, naming the items of both
 * (GP-13). Closes with `true` once the plan has been restored; on an error it stays open and
 * shows the message.
 */
@Component({
  selector: 'app-restore-plan-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Plan {{ plan.year }} wiederherstellen</h2>
    <mat-dialog-content>
      @if (activeItemCount() !== null) {
        <p>
          Für {{ plan.year }} gibt es bereits einen Haushaltsplan mit {{ activeItemCount() }} Posten.
          Er wird durch den gelöschten Plan mit {{ plan.itemCount }} Posten ersetzt und unter
          „Gelöschte Pläne" abgelegt.
        </p>
      }
      @if (error(); as message) {
        <p class="error" role="alert">{{ message }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" mat-dialog-close>Abbrechen</button>
      <button
        matButton="filled"
        type="button"
        class="overwrite"
        [disabled]="restoring() || check().state === 'checking'"
        (click)="overwrite()"
      >
        Überschreiben
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    mat-dialog-content {
      min-width: min(360px, 70vw);
    }
    p {
      margin: 0 0 12px;
    }
    .error {
      color: var(--mat-sys-error);
    }
    .overwrite:not(:disabled) {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class RestorePlanDialog {
  private readonly api = inject(BudgetApi);
  private readonly dialogRef = inject<MatDialogRef<RestorePlanDialog, boolean>>(MatDialogRef);

  protected readonly plan = inject<DeletedPlanRef>(MAT_DIALOG_DATA);
  protected readonly restoring = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly check = toSignal(this.lookUp(), {
    initialValue: { state: 'checking' } as ActiveCheck,
  });

  /** How many items the active plan has, once it has been looked up. */
  protected activeItemCount(): number | null {
    const check = this.check();
    return check.state === 'checked' ? check.itemCount : null;
  }

  protected async overwrite(): Promise<void> {
    if (this.restoring()) {
      return;
    }
    this.restoring.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.api.restorePlan(this.plan.planId, { overwrite: true }));
      this.dialogRef.close(true);
    } catch (error) {
      this.error.set(toMessage(error));
    } finally {
      this.restoring.set(false);
    }
  }

  private lookUp(): Observable<ActiveCheck> {
    return this.api.getOverview(this.plan.year).pipe(
      map((overview): ActiveCheck => ({
        state: 'checked',
        itemCount: overview ? overview.items.length : null,
      })),
      catchError((error: unknown) => {
        this.error.set(toMessage(error));
        return of<ActiveCheck>({ state: 'failed' });
      }),
      startWith<ActiveCheck>({ state: 'checking' }),
    );
  }
}

/**
 * Restores a deleted plan: right away if its year has no active plan (GP-12), otherwise only
 * after the user confirmed replacing it (GP-13, GP-14). Resolves to `true` once restored and
 * `false` if the user cancelled; rejects with an Error carrying a user-facing message.
 */
export async function restorePlan(
  api: BudgetApi,
  dialog: MatDialog,
  plan: DeletedPlanRef,
): Promise<boolean> {
  try {
    await firstValueFrom(api.restorePlan(plan.planId, { overwrite: false }));
    return true;
  } catch (error) {
    if (!(error instanceof HttpErrorResponse && error.status === HttpStatusCode.Conflict)) {
      throw new Error(toMessage(error));
    }
  }
  const restored = await firstValueFrom(
    dialog
      .open<RestorePlanDialog, DeletedPlanRef, boolean>(RestorePlanDialog, { data: plan })
      .afterClosed(),
  );
  return restored === true;
}

/** The confirmation shown once a plan has been restored (GP-12, GP-14). */
export function restoredMessage(year: Year): string {
  return `Plan ${year} wiederhergestellt.`;
}
