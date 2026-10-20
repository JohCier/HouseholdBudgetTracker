import { TextFieldModule } from '@angular/cdk/text-field';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  AMOUNT_PERIODS,
  AmountPeriod,
  EXPENSE_CATEGORIES,
  ExpenseCategory,
  ItemType,
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

@Component({
  selector: 'app-add-item-form',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TextFieldModule,
    SubcategoryField,
  ],
  templateUrl: './add-item-form.html',
  styles: `
    form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding-top: 16px;
    }
    mat-button-toggle-group {
      margin-bottom: 16px;
    }
    mat-button-toggle {
      flex: 1;
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
  `,
})
export class AddItemForm {
  private readonly store = inject(BudgetStore);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly categories = EXPENSE_CATEGORIES;
  protected readonly periods = AMOUNT_PERIODS;
  protected readonly noteMaxLength = NOTE_MAX_LENGTH;
  protected readonly submitting = signal(false);

  private readonly fb = inject(FormBuilder).nonNullable;
  /** Resetting through the directive also forgets that the form was submitted, so no field shows an error. */
  private readonly formDirective = viewChild.required(FormGroupDirective);

  protected readonly form = this.fb.group({
    type: this.fb.control<ItemType>('EXPENSE'),
    description: this.fb.control('', [Validators.required, Validators.pattern(/\S/)]),
    amount: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      maxTwoDecimals,
    ]),
    /** Most items are monthly amounts, so the form starts with them (MJ-11). */
    period: this.fb.control<AmountPeriod>('MONTHLY'),
    category: this.fb.control<ExpenseCategory | null>(null, Validators.required),
    subcategory: this.fb.control('', Validators.maxLength(SUBCATEGORY_MAX_LENGTH)),
    note: this.fb.control('', Validators.maxLength(NOTE_MAX_LENGTH)),
  });

  private readonly type = toSignal(this.form.controls.type.valueChanges, {
    initialValue: this.form.controls.type.value,
  });
  /** Suggestions fitting the type chosen, as incomes and expenses have subcategories of their own (UK-7). */
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

  protected get isExpense(): boolean {
    return this.form.controls.type.value === 'EXPENSE';
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) {
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

    this.submitting.set(true);
    try {
      if (type === 'INCOME') {
        await this.store.addIncome(request);
      } else {
        await this.store.addExpense({ ...request, category: category! });
      }
      // Type and period stay as chosen (PO-1, MJ-11)
      this.formDirective().resetForm({
        type,
        description: '',
        amount: null,
        period,
        category: null,
        subcategory: '',
        note: '',
      });
      this.snackBar.open(
        type === 'INCOME' ? 'Einnahme hinzugefügt' : 'Ausgabe hinzugefügt',
        undefined,
        {
          duration: 3000,
        },
      );
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    } finally {
      this.submitting.set(false);
    }
  }
}
