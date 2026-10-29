import { Component, Injector, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';
import { maxTwoDecimals } from '../../budget/ui/item-validators';
import { NetWorthStore } from '../data-access/net-worth-store';
import { NET_WORTH_ENTRY_TYPES, NetWorthEntry, NetWorthEntryType } from '../net-worth.model';

/** Either a new entry of the list it is recorded from (VS-11), or an entry to change (VS-5). */
export type NetWorthEntryDialogData = { type: NetWorthEntryType } | { entry: NetWorthEntry };

/** The longest name an entry may have (VS-1). */
const NAME_MAX_LENGTH = 255;

/**
 * Records an asset or a liability of the year shown, or changes one (VS-1, VS-5, VS-11): its type,
 * name and amount, which is positive for liabilities as well. Closes with `true` once saved; on an
 * error it stays open and shows the message.
 */
@Component({
  selector: 'app-net-worth-entry-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ title }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <mat-button-toggle-group formControlName="type" aria-label="Art des Werts">
          @for (type of types; track type.value) {
            <mat-button-toggle [value]="type.value">{{ type.singular }}</mat-button-toggle>
          }
        </mat-button-toggle-group>

        <mat-form-field>
          <mat-label>Bezeichnung</mat-label>
          <input matInput formControlName="name" autocomplete="off" [maxlength]="nameMaxLength" />
          @if (form.controls.name.invalid) {
            <mat-error>Bitte eine Bezeichnung angeben.</mat-error>
          }
        </mat-form-field>

        <mat-form-field>
          <mat-label>Betrag am Jahresende</mat-label>
          <input
            matInput
            type="number"
            inputmode="decimal"
            min="0.01"
            step="0.01"
            formControlName="amount"
          />
          <span matTextSuffix>€</span>
          @if (form.controls.amount.hasError('decimals')) {
            <mat-error>Höchstens zwei Nachkommastellen.</mat-error>
          } @else if (form.controls.amount.invalid) {
            <mat-error>Bitte einen positiven Betrag angeben.</mat-error>
          }
        </mat-form-field>

        @if (error(); as message) {
          <p class="error" role="alert">{{ message }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton type="button" mat-dialog-close>Abbrechen</button>
        <button matButton="filled" type="submit" [disabled]="saving()">Speichern</button>
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
    mat-button-toggle-group {
      align-self: flex-start;
      margin: 8px 0 12px;
    }
    .error {
      margin: 0;
      color: var(--mat-sys-error);
    }
  `,
})
export class NetWorthEntryDialog {
  private readonly store = inject(NetWorthStore);
  private readonly dialogRef = inject<MatDialogRef<NetWorthEntryDialog, boolean>>(MatDialogRef);
  private readonly data = inject<NetWorthEntryDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  private readonly entry = 'entry' in this.data ? this.data.entry : null;
  protected readonly types = NET_WORTH_ENTRY_TYPES;
  protected readonly nameMaxLength = NAME_MAX_LENGTH;
  protected readonly title = this.entry ? 'Wert bearbeiten' : 'Wert erfassen';
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    type: this.fb.nonNullable.control<NetWorthEntryType>(
      this.entry?.type ?? ('type' in this.data ? this.data.type : 'ASSET'),
    ),
    name: this.fb.nonNullable.control(this.entry?.name ?? '', [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(NAME_MAX_LENGTH),
    ]),
    amount: this.fb.control<number | null>(this.entry ? Number(this.entry.amount) : null, [
      Validators.required,
      Validators.min(0.01),
      maxTwoDecimals,
    ]),
  });

  protected async save(): Promise<void> {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const { type, name, amount } = this.form.getRawValue();
    const request = { type, name: name.trim(), amount: String(amount) };
    this.saving.set(true);
    this.error.set(null);
    try {
      if (this.entry) {
        await this.store.changeEntry(this.entry.id, request);
      } else {
        await this.store.addEntry(request);
      }
      this.dialogRef.close(true);
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.saving.set(false);
    }
  }
}

/** Opens the dialog within the injector of the page, whose store it uses; emits `true` once saved. */
export function openNetWorthEntryDialog(
  dialog: MatDialog,
  injector: Injector,
  data: NetWorthEntryDialogData,
): Observable<boolean | undefined> {
  return dialog
    .open<NetWorthEntryDialog, NetWorthEntryDialogData, boolean>(NetWorthEntryDialog, {
      data,
      injector,
    })
    .afterClosed();
}
