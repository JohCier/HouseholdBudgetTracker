import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EXPENSE_CATEGORIES, ExpenseCategory, ItemType } from '../budget.model';
import { BudgetStore } from '../data-access/budget-store';
import { today } from '../month';

/** Amounts are stored in cents, so more than two decimal places are rejected. */
function maxTwoDecimals(control: AbstractControl<number | null>): ValidationErrors | null {
  const value = control.value;
  return value === null || /^\d+(\.\d{1,2})?$/.test(String(value)) ? null : { decimals: true };
}

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
  `,
})
export class AddItemForm {
  private readonly store = inject(BudgetStore);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly categories = EXPENSE_CATEGORIES;
  protected readonly submitting = signal(false);

  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly form = this.fb.group({
    type: this.fb.control<ItemType>('EXPENSE'),
    description: this.fb.control('', [Validators.required, Validators.pattern(/\S/)]),
    amount: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      maxTwoDecimals,
    ]),
    bookingDate: this.fb.control(today(), Validators.required),
    category: this.fb.control<ExpenseCategory | null>(null, Validators.required),
  });

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
    const { type, description, amount, bookingDate, category } = this.form.getRawValue();
    const request = { description: description.trim(), amount: String(amount), bookingDate };

    this.submitting.set(true);
    try {
      if (type === 'INCOME') {
        await this.store.addIncome(request);
      } else {
        await this.store.addExpense({ ...request, category: category! });
      }
      this.form.reset({ type, description: '', amount: null, bookingDate, category: null });
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
