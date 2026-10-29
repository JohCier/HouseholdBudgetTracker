import { Component, Injector, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { OneTimeItem } from '../budget.model';
import { OneTimeItemStore } from '../data-access/one-time-item-store';

/**
 * Asks before a one-time item is deleted for good; there is no undo (EP-17). Closes with `true`
 * once the item has been deleted; on an error it stays open and shows the message. Needs the
 * dashboard's {@link OneTimeItemStore}, so it is opened with the dashboard's injector.
 */
@Component({
  selector: 'app-delete-one-time-item-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Einmalposten löschen</h2>
    <mat-dialog-content>
      <p>Einmalposten „{{ item.description }}" löschen? Das lässt sich nicht rückgängig machen.</p>
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
        [disabled]="deleting()"
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
export class DeleteOneTimeItemDialog {
  private readonly store = inject(OneTimeItemStore);
  private readonly dialogRef = inject<MatDialogRef<DeleteOneTimeItemDialog, boolean>>(MatDialogRef);

  protected readonly item = inject<OneTimeItem>(MAT_DIALOG_DATA);
  protected readonly deleting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected async delete(): Promise<void> {
    if (this.deleting()) {
      return;
    }
    this.deleting.set(true);
    this.error.set(null);
    try {
      await this.store.deleteItem(this.item.id);
      this.dialogRef.close(true);
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.deleting.set(false);
    }
  }
}

/** Opens the dialog; emits `true` once the item has been deleted, `undefined` if cancelled. */
export function openDeleteOneTimeItemDialog(
  dialog: MatDialog,
  injector: Injector,
  item: OneTimeItem,
): Observable<boolean | undefined> {
  return dialog
    .open<DeleteOneTimeItemDialog, OneTimeItem, boolean>(DeleteOneTimeItemDialog, {
      data: item,
      injector,
    })
    .afterClosed();
}
