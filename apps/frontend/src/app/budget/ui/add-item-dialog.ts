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
 * Adds an item, either from the button "Posten erfassen" as an expense with all fields empty
 * (AP-14) or right from one of the groups of the entries, with type, category and subcategory of
 * the group filled in (EG-10). The focus is on the description. Every field can still be changed; the item then goes to the group its
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
  templateUrl: './add-item-dialog.html',
  styleUrl: './add-item-dialog.scss',
})
export class AddItemDialog {
  private readonly store = inject(BudgetStore);
  private readonly dialogRef = inject<MatDialogRef<AddItemDialog, boolean>>(MatDialogRef);
  /** The group the item is added to; `null` for an item from "Posten erfassen" (AP-14). */
  protected readonly preset = inject<NewItemPreset | null>(MAT_DIALOG_DATA);

  protected readonly categories = EXPENSE_CATEGORIES;
  protected readonly periods = AMOUNT_PERIODS;
  protected readonly noteMaxLength = NOTE_MAX_LENGTH;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly form = this.fb.group({
    type: this.fb.control<ItemType>(this.preset?.type ?? 'EXPENSE'),
    description: this.fb.control('', [Validators.required, Validators.pattern(/\S/)]),
    amount: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      maxTwoDecimals,
    ]),
    period: this.fb.control<AmountPeriod>('MONTHLY'),
    category: this.fb.control<ExpenseCategory | null>(
      { value: this.preset?.category ?? null, disabled: this.preset?.type === 'INCOME' },
      Validators.required,
    ),
    subcategory: this.fb.control(
      this.preset?.subcategory ?? '',
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
