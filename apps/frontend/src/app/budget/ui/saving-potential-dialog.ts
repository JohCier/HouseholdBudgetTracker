import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { BudgetItem } from '../budget.model';
import { BudgetStore } from '../data-access/budget-store';
import { maxTwoDecimals } from './item-validators';

/**
 * Sets, changes or removes the saving potential of an expense: how much of it could be saved, always
 * per month, at most its monthly amount (PM-2a, PM-4b). Filled in with the saving potential the
 * expense has, otherwise with its whole monthly amount. Closes with `true` once the change is
 * saved; on an error it stays open and shows the message.
 * Needs the dashboard's {@link BudgetStore}, so it is opened with the dashboard's injector.
 */
@Component({
  selector: 'app-saving-potential-dialog',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Sparpotential bei {{ item.description }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <p class="hint">
          Wie viel davon könntest du im Monat einsparen? Die Ausgabe beträgt
          {{ item.monthlyAmount | currency: 'EUR' }} im Monat.
        </p>
        <mat-form-field>
          <mat-label>Betrag pro Monat</mat-label>
          <input
            matInput
            type="number"
            inputmode="decimal"
            min="0.01"
            step="0.01"
            formControlName="monthlyAmount"
          />
          <span matTextSuffix>€</span>
          @if (form.controls.monthlyAmount.hasError('decimals')) {
            <mat-error>Höchstens zwei Nachkommastellen.</mat-error>
          } @else if (form.controls.monthlyAmount.hasError('max')) {
            <mat-error>Höchstens {{ item.monthlyAmount | currency: 'EUR' }}.</mat-error>
          } @else if (form.controls.monthlyAmount.invalid) {
            <mat-error>Bitte einen positiven Betrag angeben.</mat-error>
          }
        </mat-form-field>

        @if (error(); as message) {
          <p class="error" role="alert">{{ message }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        @if (item.savingPotential) {
          <button matButton type="button" class="remove" [disabled]="saving()" (click)="remove()">
            Entfernen
          </button>
        }
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
    .hint {
      margin: 8px 0 12px;
      color: var(--mat-sys-on-surface-variant);
    }
    .remove {
      margin-right: auto;
    }
    .error {
      color: var(--mat-sys-error);
      margin: 0;
    }
  `,
})
export class SavingPotentialDialog {
  private readonly store = inject(BudgetStore);
  private readonly dialogRef = inject<MatDialogRef<SavingPotentialDialog, boolean>>(MatDialogRef);
  protected readonly item = inject<BudgetItem>(MAT_DIALOG_DATA);

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly form = this.fb.group({
    monthlyAmount: this.fb.control<number | null>(
      Number(this.item.savingPotential ?? this.item.monthlyAmount),
      [
        Validators.required,
        Validators.min(0.01),
        Validators.max(Number(this.item.monthlyAmount)),
        maxTwoDecimals,
      ],
    ),
  });

  protected save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return Promise.resolve();
    }
    return this.apply(String(this.form.getRawValue().monthlyAmount));
  }

  protected remove(): Promise<void> {
    return this.apply(null);
  }

  private async apply(monthlyAmount: string | null): Promise<void> {
    if (this.saving()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    try {
      await this.store.changeSavingPotential(this.item.id, monthlyAmount);
      this.dialogRef.close(true);
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.saving.set(false);
    }
  }
}
