import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { BudgetOverview } from '../budget.model';

@Component({
  selector: 'app-overview-summary',
  imports: [CurrencyPipe, MatCardModule, MatIconModule],
  template: `
    <div class="tiles">
      <mat-card appearance="outlined" class="tile">
        <span class="label">Einnahmen</span>
        <span class="value income">{{ overview().totalIncome | currency: 'EUR' }}</span>
      </mat-card>
      <mat-card appearance="outlined" class="tile">
        <span class="label">Ausgaben</span>
        <span class="value expense">{{ overview().totalExpense | currency: 'EUR' }}</span>
      </mat-card>
      <mat-card appearance="outlined" class="tile">
        <span class="label">Saldo</span>
        <span class="value" [class.expense]="overview().balance < 0">
          {{ overview().balance | currency: 'EUR' }}
        </span>
      </mat-card>
    </div>
    @if (overview().expensesExceedIncome) {
      <div class="warning" role="alert">
        <mat-icon>warning</mat-icon>
        Deine Ausgaben übersteigen deine Einnahmen.
      </div>
    }
  `,
  styles: `
    .tiles {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
    }
    .tile {
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .label {
      font: var(--mat-sys-label-large);
      color: var(--mat-sys-on-surface-variant);
    }
    .value {
      font: var(--mat-sys-headline-small);
      font-variant-numeric: tabular-nums;
    }
    .income {
      color: var(--app-income-color);
    }
    .expense {
      color: var(--mat-sys-error);
    }
    .warning {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 16px;
      padding: 12px 16px;
      border-radius: 12px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
  `,
})
export class OverviewSummary {
  readonly overview = input.required<BudgetOverview>();
}
