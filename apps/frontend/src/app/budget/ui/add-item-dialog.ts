import { TextFieldModule } from '@angular/cdk/text-field';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
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
  EXPENSE_CATEGORIES,
  ExpenseCategory,
  ItemType,
  NewItemPreset,
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
 * Adds an item right from one of the groups of the entries: the same fields as the form "Neuer
 * Eintrag", with type, category and subcategory of the group filled in and the focus on the
 * description (EG-10). Every field can still be changed; the item then goes to the group its
 * values belong to. Closes with `true` once the item is added; on an error it stays open and
 * shows the message, so nothing typed is lost (EG-11).
 * Needs the dashboard's {@link BudgetStore}, so it is opened with the dashboard's injector.
 */
@Component({
  selector: 'app-add-item-dialog',
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
    <h2 mat-dialog-title>Neuer Eintrag in {{ preset.group }}</h2>
    <form [formGroup]="form" (ngSubmit)="add()">
      <mat-dialog-content>
        <mat-button-toggle-group formControlName="type" aria-label="Art des Eintrags">
          <mat-button-toggle value="EXPENSE">Ausgabe</mat-button-toggle>
          <mat-button-toggle value="INCOME">Einnahme</mat-button-toggle>
        </mat-button-toggle-group>

        <mat-form-field>
          <mat-label>Beschreibung</mat-label>
          <input
            matInput
            formControlName="description"
            autocomplete="off"
            maxlength="255"
            cdkFocusInitial
          />
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

        @if (type() === 'EXPENSE') {
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
          [suggestions]="subcategories()"
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
        <button matButton="filled" type="submit" [disabled]="saving()">Hinzufügen</button>
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
    mat-button-toggle-group:first-child {
      margin: 8px 0 16px;
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
export class AddItemDialog {
  private readonly store = inject(BudgetStore);
  private readonly dialogRef = inject<MatDialogRef<AddItemDialog, boolean>>(MatDialogRef);
  protected readonly preset = inject<NewItemPreset>(MAT_DIALOG_DATA);

  protected readonly categories = EXPENSE_CATEGORIES;
  protected readonly periods = AMOUNT_PERIODS;
  protected readonly noteMaxLength = NOTE_MAX_LENGTH;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly form = this.fb.group({
    type: this.fb.control<ItemType>(this.preset.type),
    description: this.fb.control('', [Validators.required, Validators.pattern(/\S/)]),
    amount: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      maxTwoDecimals,
    ]),
    period: this.fb.control<AmountPeriod>('MONTHLY'),
    category: this.fb.control<ExpenseCategory | null>(
      { value: this.preset.category, disabled: this.preset.type === 'INCOME' },
      Validators.required,
    ),
    subcategory: this.fb.control(
      this.preset.subcategory,
      Validators.maxLength(SUBCATEGORY_MAX_LENGTH),
    ),
    note: this.fb.control('', Validators.maxLength(NOTE_MAX_LENGTH)),
  });

  protected readonly type = toSignal(this.form.controls.type.valueChanges, {
    initialValue: this.form.controls.type.value,
  });
  /** Suggestions fitting the type chosen (UK-7). */
  protected readonly subcategories = computed(() => this.store.subcategorySuggestions(this.type()));

  constructor() {
    this.form.controls.type.valueChanges.subscribe((type) => {
      const category = this.form.controls.category;
      if (type === 'INCOME') {
        category.reset(null);
        category.disable();
      } else {
        category.enable();
      }
    });
  }

  protected async add(): Promise<void> {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const { type, description, amount, period, category, subcategory, note } =
      this.form.getRawValue();
    const request = {
      description: description.trim(),
      amount: String(amount),
      period,
      ...subcategoryOf(subcategory),
      ...noteOf(note),
    };
    this.saving.set(true);
    this.error.set(null);
    try {
      if (type === 'INCOME') {
        await this.store.addIncome(request);
      } else {
        await this.store.addExpense({ ...request, category: category! });
      }
      this.dialogRef.close(true);
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.saving.set(false);
    }
  }
}
