import { TextFieldModule } from '@angular/cdk/text-field';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  AMOUNT_PERIODS,
  AmountPeriod,
  BudgetItem,
  EXPENSE_CATEGORIES,
  ExpenseCategory,
} from '../budget.model';
import { BudgetStore } from '../data-access/budget-store';
import {
  NOTE_MAX_LENGTH,
  SUBCATEGORY_MAX_LENGTH,
  maxTwoDecimals,
  noteOf,
  subcategoryOf,
} from './item-validators';
import { SubcategoryField } from './subcategory-field';

/**
 * Changes description, amount, period, subcategory, note and, for an expense, the category of an
 * item. The amount is shown as entered with its period; switching the period does not convert it
 * (MJ-5, MJ-11).
 * Emptying the note removes it (PN-4), emptying the subcategory moves the item to "Sonstiges" (UK-4). Whether it is
 * an income or an expense cannot be changed. Closes with `true` once the change is saved;
 * on an error it stays open and shows the message, so nothing typed is lost.
 * Needs the dashboard's {@link BudgetStore}, so it is opened with the dashboard's injector.
 */
@Component({
  selector: 'app-edit-item-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TextFieldModule,
    SubcategoryField,
  ],
  template: `
    <h2 mat-dialog-title>{{ isExpense ? 'Ausgabe' : 'Einnahme' }} bearbeiten</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <mat-form-field>
          <mat-label>Beschreibung</mat-label>
          <input matInput formControlName="description" autocomplete="off" maxlength="255" />
          @if (form.controls.description.invalid) {
            <mat-error>Bitte eine Beschreibung angeben.</mat-error>
          }
        </mat-form-field>

        <div class="amount">
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
          <mat-button-toggle-group
            formControlName="period"
            aria-label="Angabe monatlich oder jährlich"
          >
            @for (period of periods; track period.value) {
              <mat-button-toggle [value]="period.value">{{ period.label }}</mat-button-toggle>
            }
          </mat-button-toggle-group>
        </div>

        @if (isExpense) {
          <mat-form-field>
            <mat-label>Kategorie</mat-label>
            <mat-select formControlName="category">
              @for (category of categories; track category.value) {
                <mat-option [value]="category.value">{{ category.label }}</mat-option>
              }
            </mat-select>
            @if (form.controls.category.invalid) {
              <mat-error>Bitte eine Kategorie wählen.</mat-error>
            }
          </mat-form-field>
        }

        <app-subcategory-field
          [control]="form.controls.subcategory"
          [suggestions]="subcategories"
        />

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
    mat-form-field:first-child {
      margin-top: 8px;
    }
    .amount {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      column-gap: 12px;
    }
    .amount mat-form-field {
      flex: 1 1 140px;
    }
    .amount mat-button-toggle-group {
      margin: 4px 0 16px;
    }
    mat-button-toggle {
      flex: 1;
    }
    .error {
      color: var(--mat-sys-error);
      margin: 0;
    }
  `,
})
export class EditItemDialog {
  private readonly store = inject(BudgetStore);
  private readonly dialogRef = inject<MatDialogRef<EditItemDialog, boolean>>(MatDialogRef);
  private readonly item = inject<BudgetItem>(MAT_DIALOG_DATA);

  protected readonly categories = EXPENSE_CATEGORIES;
  protected readonly periods = AMOUNT_PERIODS;
  protected readonly noteMaxLength = NOTE_MAX_LENGTH;
  protected readonly isExpense = this.item.type === 'EXPENSE';
  protected readonly subcategories = this.store.subcategorySuggestions(this.item.type);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly form = this.fb.group({
    description: this.fb.control(this.item.description, [
      Validators.required,
      Validators.pattern(/\S/),
    ]),
    amount: this.fb.control<number | null>(Number(this.item.amount), [
      Validators.required,
      Validators.min(0.01),
      maxTwoDecimals,
    ]),
    period: this.fb.control<AmountPeriod>(this.item.period),
    category: this.fb.control<ExpenseCategory | null>(
      { value: this.item.category, disabled: !this.isExpense },
      Validators.required,
    ),
    subcategory: this.fb.control(
      this.item.subcategory ?? '',
      Validators.maxLength(SUBCATEGORY_MAX_LENGTH),
    ),
    note: this.fb.control(this.item.note ?? '', Validators.maxLength(NOTE_MAX_LENGTH)),
  });

  protected async save(): Promise<void> {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const { description, amount, period, category, subcategory, note } = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    try {
      await this.store.changeItem(this.item.id, {
        description: description.trim(),
        amount: String(amount),
        period,
        ...(this.isExpense ? { category: category! } : {}),
        ...subcategoryOf(subcategory),
        ...noteOf(note),
      });
      this.dialogRef.close(true);
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.saving.set(false);
    }
  }
}
