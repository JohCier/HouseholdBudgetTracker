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
import { DeletedPlanRef } from './restore-plan-dialog';

export interface DeletePlanDialogData {
  year: Year;
}

/** What is known about the plan to delete. */
type PlanCheck =
  | { state: 'checking' }
  | { state: 'checked'; plan: { id: string; itemCount: number } | null }
  | { state: 'failed' };

/**
 * Asks before the plan of a year is deleted, naming the year and how many items go with it
 * (PL-8, GP-8). Closes with the deleted plan once it has been moved to the deleted plans, so
 * that it can be restored (GP-9); on an error it stays open and shows the message.
 */
@Component({
  selector: 'app-delete-plan-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Plan {{ year }} löschen</h2>
    <mat-dialog-content>
      @if (plan(); as plan) {
        <p>
          Der Haushaltsplan {{ year }} mit {{ plan.itemCount }} Posten wird unter „Gelöschte
          Pläne" abgelegt und lässt sich dort wiederherstellen.
        </p>
      } @else if (check().state === 'checked') {
        <p>Für {{ year }} gibt es keinen Haushaltsplan mehr.</p>
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
        class="delete"
        [disabled]="deleting() || !plan()"
        (click)="delete()"
      >
        Löschen
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
    .delete:not(:disabled) {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class DeletePlanDialog {
  private readonly api = inject(BudgetApi);
  private readonly dialogRef = inject<MatDialogRef<DeletePlanDialog, DeletedPlanRef>>(MatDialogRef);

  protected readonly year = inject<DeletePlanDialogData>(MAT_DIALOG_DATA).year;
  protected readonly deleting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly check = toSignal(this.lookUp(), {
    initialValue: { state: 'checking' } as PlanCheck,
  });

  /** The plan to delete, once it has been looked up. */
  protected plan(): { id: string; itemCount: number } | null {
    const check = this.check();
    return check.state === 'checked' ? check.plan : null;
  }

  protected async delete(): Promise<void> {
    const plan = this.plan();
    if (!plan || this.deleting()) {
      return;
    }
    this.deleting.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.api.deletePlan(plan.id));
      this.dialogRef.close({ planId: plan.id, year: this.year, itemCount: plan.itemCount });
    } catch (error) {
      this.error.set(toMessage(error));
    } finally {
      this.deleting.set(false);
    }
  }

  private lookUp(): Observable<PlanCheck> {
    return this.api.getOverview(this.year).pipe(
      map((overview): PlanCheck => ({
        state: 'checked',
        plan: overview ? { id: overview.id, itemCount: overview.items.length } : null,
      })),
      catchError((error: unknown) => {
        this.error.set(toMessage(error));
        return of<PlanCheck>({ state: 'failed' });
      }),
      startWith<PlanCheck>({ state: 'checking' }),
    );
  }
}

/** Opens the dialog; emits the deleted plan once it has been deleted, `undefined` if cancelled. */
export function openDeletePlanDialog(
  dialog: MatDialog,
  year: Year,
): Observable<DeletedPlanRef | undefined> {
  return dialog
    .open<DeletePlanDialog, DeletePlanDialogData, DeletedPlanRef>(DeletePlanDialog, {
      data: { year },
    })
    .afterClosed();
}

/** The confirmation shown once a plan has been deleted (PL-9, GP-9). */
export function deletedMessage(year: Year): string {
  return `Plan ${year} gelöscht.`;
}

/** The navigation state by which the plan view hands a deleted plan to the overview (GP-9). */
export const DELETED_PLAN_STATE = 'deletedPlan';
