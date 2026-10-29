import { TextFieldModule } from '@angular/cdk/text-field';
import { Component, Injector, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
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
import { ITEM_TYPES, ItemType, OneTimeItem, Year } from '../budget.model';
import { OneTimeItemStore } from '../data-access/one-time-item-store';
import { defaultDateIn } from '../year';
import { NOTE_MAX_LENGTH, maxTwoDecimals, noteOf } from './item-validators';

/** The first and last day a one-time item can have (EP-3). */
export const FIRST_DATE = '1900-01-01';
export const LAST_DATE = '2200-12-31';

export interface OneTimeItemDialogData {
  /** The year shown, whose days a new item starts with (EP-11). */
  year: Year;
  /** The item to change; a new one is entered without. */
  item?: OneTimeItem;
}

/** A day from {@link FIRST_DATE} to {@link LAST_DATE} as an input of type date gives it (EP-3). */
function dayInRange(control: AbstractControl<string>): ValidationErrors | null {
  const value = control.value;
  return /^\d{4}-\d{2}-\d{2}$/.test(value) && value >= FIRST_DATE && value <= LAST_DATE
    ? null
    : { date: true };
}

/**
 * Enters a one-time item or changes one: type, description, amount, day and an optional note
 * (EP-1, EP-7). A new item starts with today if it lies in the year shown, otherwise with 1 January
 * of it (EP-11). Closes with `true` once the item is saved; on an error it stays open and shows the
 * message, so nothing typed is lost. Needs the dashboard's {@link OneTimeItemStore}, so it is opened
 * with the dashboard's injector.
 */
@Component({
  selector: 'app-one-time-item-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    TextFieldModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ item ? 'Einmalposten bearbeiten' : 'Einmalposten erfassen' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <mat-button-toggle-group formControlName="type" aria-label="Art des Einmalpostens">
          @for (type of types; track type.value) {
            <mat-button-toggle [value]="type.value">{{ type.label }}</mat-button-toggle>
          }
        </mat-button-toggle-group>

        <mat-form-field>
          <mat-label>Beschreibung</mat-label>
          <input matInput formControlName="description" autocomplete="off" maxlength="255" />
          @if (form.controls.description.invalid) {
            <mat-error>Bitte eine Beschreibung angeben.</mat-error>
          }
        </mat-form-field>

        <div class="row">
          <mat-form-field>
            <mat-label>Betrag</mat-label>
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
          <mat-form-field>
            <mat-label>Datum</mat-label>
            <input matInput type="date" [min]="firstDate" [max]="lastDate" formControlName="date" />
            @if (form.controls.date.invalid) {
              <mat-error>Bitte ein Datum von 1900 bis 2200 angeben.</mat-error>
            }
          </mat-form-field>
        </div>

        <mat-form-field>
          <mat-label>Notiz (optional)</mat-label>
          <textarea matInput formControlName="note" rows="2" cdkTextareaAutosize></textarea>
          <mat-hint align="end"
            >{{ form.controls.note.value.length }} / {{ noteMaxLength }}</mat-hint
          >
          @if (form.controls.note.invalid) {
            <mat-error>Höchstens {{ noteMaxLength }} Zeichen.</mat-error>
          }
        </mat-form-field>

        <p class="hint">
          Einmalposten zählen nicht zum Haushaltsplan und nicht zur 50/30/20-Regel.
        </p>

        @if (error(); as message) {
          <p class="error" role="alert">{{ message }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton type="button" mat-dialog-close>Abbrechen</button>
        <button matButton="filled" type="submit" [disabled]="saving()">
          {{ item ? 'Speichern' : 'Erfassen' }}
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
    mat-button-toggle-group {
      align-self: flex-start;
      margin: 8px 0 16px;
    }
    .row {
      display: flex;
      flex-wrap: wrap;
      column-gap: 12px;
    }
    .row mat-form-field {
      flex: 1 1 140px;
    }
    .hint {
      margin: 0 0 8px;
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
    }
    .error {
      color: var(--mat-sys-error);
      margin: 0;
    }
  `,
})
export class OneTimeItemDialog {
  private readonly store = inject(OneTimeItemStore);
  private readonly dialogRef = inject<MatDialogRef<OneTimeItemDialog, boolean>>(MatDialogRef);
  private readonly data = inject<OneTimeItemDialogData>(MAT_DIALOG_DATA);

  protected readonly item = this.data.item;
  protected readonly types = ITEM_TYPES;
  protected readonly noteMaxLength = NOTE_MAX_LENGTH;
  protected readonly firstDate = FIRST_DATE;
  protected readonly lastDate = LAST_DATE;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly form = this.fb.group({
    type: this.fb.control<ItemType>(this.item?.type ?? 'EXPENSE'),
    description: this.fb.control(this.item?.description ?? '', [
      Validators.required,
      Validators.pattern(/\S/),
    ]),
    amount: this.fb.control<number | null>(this.item ? Number(this.item.amount) : null, [
      Validators.required,
      Validators.min(0.01),
      maxTwoDecimals,
    ]),
    date: this.fb.control(this.item?.date ?? defaultDateIn(this.data.year), dayInRange),
    note: this.fb.control(this.item?.note ?? '', Validators.maxLength(NOTE_MAX_LENGTH)),
  });

  protected async save(): Promise<void> {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const { type, description, amount, date, note } = this.form.getRawValue();
    const request = {
      type,
      description: description.trim(),
      amount: String(amount),
      date,
      ...noteOf(note),
    };
    this.saving.set(true);
    this.error.set(null);
    try {
      if (this.item) {
        await this.store.changeItem(this.item.id, request);
      } else {
        await this.store.createItem(request);
      }
      this.dialogRef.close(true);
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.saving.set(false);
    }
  }
}

/** Opens the dialog; emits `true` once the item has been saved, `undefined` if cancelled. */
export function openOneTimeItemDialog(
  dialog: MatDialog,
  injector: Injector,
  data: OneTimeItemDialogData,
): Observable<boolean | undefined> {
  return dialog
    .open<OneTimeItemDialog, OneTimeItemDialogData, boolean>(OneTimeItemDialog, { data, injector })
    .afterClosed();
}
