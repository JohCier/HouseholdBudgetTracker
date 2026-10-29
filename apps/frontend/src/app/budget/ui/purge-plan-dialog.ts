import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { Observable, firstValueFrom } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import { Year } from '../budget.model';
import { BudgetApi } from '../data-access/budget-api';
import { DeletedPlanRef } from './restore-plan-dialog';

/**
 * Asks before a deleted plan is deleted for good, naming its year and items (GP-16). Closes
 * with `true` once it is gone; on an error it stays open and shows the message.
 */
@Component({
  selector: 'app-purge-plan-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Plan {{ plan.year }} endgültig löschen</h2>
    <mat-dialog-content>
      <p>
        Der gelöschte Haushaltsplan {{ plan.year }} mit {{ plan.itemCount }} Posten wird endgültig
        gelöscht. Das lässt sich nicht rückgängig machen.
      </p>
      @if (error(); as message) {
        <p class="error" role="alert">{{ message }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" mat-dialog-close>Abbrechen</button>
      <button
        matButton="filled"
        type="button"
        class="purge"
        [disabled]="purging()"
        (click)="purge()"
      >
        Endgültig löschen
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
    .purge:not(:disabled) {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class PurgePlanDialog {
  private readonly api = inject(BudgetApi);
  private readonly dialogRef = inject<MatDialogRef<PurgePlanDialog, boolean>>(MatDialogRef);

  protected readonly plan = inject<DeletedPlanRef>(MAT_DIALOG_DATA);
  protected readonly purging = signal(false);
  protected readonly error = signal<string | null>(null);

  protected async purge(): Promise<void> {
    if (this.purging()) {
      return;
    }
    this.purging.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.api.purgePlan(this.plan.planId));
      this.dialogRef.close(true);
    } catch (error) {
      this.error.set(toMessage(error));
    } finally {
      this.purging.set(false);
    }
  }
}

/** Opens the dialog; emits `true` once the plan is gone for good, `undefined` if cancelled. */
export function openPurgePlanDialog(
  dialog: MatDialog,
  plan: DeletedPlanRef,
): Observable<boolean | undefined> {
  return dialog
    .open<PurgePlanDialog, DeletedPlanRef, boolean>(PurgePlanDialog, { data: plan })
    .afterClosed();
}

/** The confirmation shown once a plan has been deleted for good (GP-17). */
export function purgedMessage(year: Year): string {
  return `Plan ${year} endgültig gelöscht.`;
}
