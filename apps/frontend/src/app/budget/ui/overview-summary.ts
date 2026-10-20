import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { HouseholdPlanOverview } from '../budget.model';

/** Income, expenses and balance of the plan, each per month and per year (MJ-13). */
@Component({
  selector: 'app-overview-summary',
  imports: [CurrencyPipe, MatCardModule, MatIconModule],
  template: `
    <div class="tiles">
      <mat-card appearance="outlined" class="tile">
        <span class="label">Einnahmen</span>
        <span class="amount">
          <span class="value monthly income">{{
            overview().monthlyTotalIncome | currency: 'EUR'
          }}</span>
          <span class="period">im Monat</span>
        </span>
        <span class="amount">
          <span class="value yearly income">{{ overview().totalIncome | currency: 'EUR' }}</span>
          <span class="period">im Jahr</span>
        </span>
      </mat-card>
      <mat-card appearance="outlined" class="tile">
        <span class="label">Ausgaben</span>
        <span class="amount">
          <span class="value monthly expense">{{
            overview().monthlyTotalExpense | currency: 'EUR'
          }}</span>
          <span class="period">im Monat</span>
        </span>
        <span class="amount">
          <span class="value yearly expense">{{ overview().totalExpense | currency: 'EUR' }}</span>
          <span class="period">im Jahr</span>
        </span>
      </mat-card>
      <mat-card appearance="outlined" class="tile">
        <span class="label">Saldo</span>
        <span class="amount">
          <span class="value monthly" [class.expense]="overview().monthlyBalance.startsWith('-')">{{
            overview().monthlyBalance | currency: 'EUR'
          }}</span>
          <span class="period">im Monat</span>
        </span>
        <span class="amount">
          <span class="value yearly" [class.expense]="overview().balance.startsWith('-')">{{
            overview().balance | currency: 'EUR'
          }}</span>
          <span class="period">im Jahr</span>
        </span>
      </mat-card>
    </div>
    @if (overview().expensesExceedIncome) {
      <div class="warning" role="alert">
        <mat-icon svgIcon="warning" />
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
      font: var(--mat-sys-title-large);
      font-variant-numeric: tabular-nums;
    }
    .amount {
      display: flex;
      align-items: baseline;
      gap: 6px;
    }
    .period {
      font: var(--mat-sys-label-medium);
      color: var(--mat-sys-on-surface-variant);
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
  readonly overview = input.required<HouseholdPlanOverview>();
}
