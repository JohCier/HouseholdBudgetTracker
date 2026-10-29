import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { NetWorthEntry } from '../net-worth.model';

/** Asks before an asset or a liability is removed (VS-5); closes with `true` to remove it. */
@Component({
  selector: 'app-remove-net-worth-entry-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Wert „{{ entry.name }}“ löschen?</h2>
    <mat-dialog-content>
      <p>Das lässt sich nicht rückgängig machen.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" mat-dialog-close>Abbrechen</button>
      <button matButton="filled" type="button" class="delete" [mat-dialog-close]="true">
        Löschen
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    p {
      margin: 0;
    }
    .delete {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class RemoveNetWorthEntryDialog {
  protected readonly entry = inject<NetWorthEntry>(MAT_DIALOG_DATA);
}

/** Opens the dialog; emits `true` if the user confirmed. */
export function openRemoveNetWorthEntryDialog(
  dialog: MatDialog,
  entry: NetWorthEntry,
): Observable<boolean | undefined> {
  return dialog
    .open<RemoveNetWorthEntryDialog, NetWorthEntry, boolean>(RemoveNetWorthEntryDialog, {
      data: entry,
    })
    .afterClosed();
}
