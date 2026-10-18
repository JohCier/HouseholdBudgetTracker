import { TextFieldModule } from '@angular/cdk/text-field';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EXPENSE_CATEGORIES, ExpenseCategory, ItemType } from '../budget.model';
import { BudgetStore } from '../data-access/budget-store';
import { NOTE_MAX_LENGTH, maxTwoDecimals, noteOf } from './item-validators';

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
  protected readonly noteMaxLength = NOTE_MAX_LENGTH;
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
    category: this.fb.control<ExpenseCategory | null>(null, Validators.required),
    note: this.fb.control('', Validators.maxLength(NOTE_MAX_LENGTH)),
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
    const { type, description, amount, category, note } = this.form.getRawValue();
    const request = { description: description.trim(), amount: String(amount), ...noteOf(note) };

    this.submitting.set(true);
    try {
      if (type === 'INCOME') {
        await this.store.addIncome(request);
      } else {
        await this.store.addExpense({ ...request, category: category! });
      }
      this.form.reset({ type, description: '', amount: null, category: null, note: '' });
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
