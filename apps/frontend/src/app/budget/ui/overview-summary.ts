import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { HouseholdPlanOverview } from '../budget.model';

/**
 * Income, expenses and balance of the plan in one box, per month and per year side by side (BI-1);
 * below them, apart from the balance, the saving potential of all expenses (PM-14).
 */
@Component({
  selector: 'app-overview-summary',
  imports: [CurrencyPipe, MatCardModule, MatIconModule],
  template: `
    <mat-card appearance="outlined" class="balance-sheet">
      <mat-card-header>
        <mat-card-title id="balance-sheet-title">Bilanz</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <table aria-labelledby="balance-sheet-title">
          <thead>
            <tr>
              <td></td>
              <th scope="col">Monat</th>
              <th scope="col">Jahr</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <th scope="row">Gesamteinnahmen</th>
              <td class="value monthly income">
                {{ overview().monthlyTotalIncome | currency: 'EUR' }}
              </td>
              <td class="value yearly income">{{ overview().totalIncome | currency: 'EUR' }}</td>
            </tr>
            <tr>
              <th scope="row">Gesamtausgaben</th>
              <td class="value monthly expense">
                {{ overview().monthlyTotalExpense | currency: 'EUR' }}
              </td>
              <td class="value yearly expense">{{ overview().totalExpense | currency: 'EUR' }}</td>
            </tr>
            <tr class="balance">
              <th scope="row">Saldo</th>
              <td class="value monthly" [class.expense]="overview().monthlyBalance.startsWith('-')">
                {{ overview().monthlyBalance | currency: 'EUR' }}
              </td>
              <td class="value yearly" [class.expense]="overview().balance.startsWith('-')">
                {{ overview().balance | currency: 'EUR' }}
              </td>
            </tr>
            <tr class="saving-potential">
              <th scope="row">Sparpotential</th>
              <td class="value monthly">
                {{ overview().monthlySavingPotential | currency: 'EUR' }}
              </td>
              <td class="value yearly">{{ overview().savingPotential | currency: 'EUR' }}</td>
            </tr>
          </tbody>
        </table>
      </mat-card-content>
    </mat-card>
    @if (overview().expensesExceedIncome) {
      <div class="warning" role="alert">
        <mat-icon svgIcon="warning" />
        Deine Ausgaben übersteigen deine Einnahmen.
      </div>
    }
  `,
  styles: `
    .balance-sheet {
      max-width: 560px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th,
    td {
      padding: 6px 0 6px 12px;
    }
    th:first-child,
    td:first-child {
      padding-left: 0;
    }
    thead th {
      text-align: right;
      font: var(--mat-sys-label-medium);
      color: var(--mat-sys-on-surface-variant);
    }
    tbody th {
      text-align: left;
      font: var(--mat-sys-label-large);
      color: var(--mat-sys-on-surface-variant);
    }
    .value {
      text-align: right;
      white-space: nowrap;
      font: var(--mat-sys-title-medium);
      font-variant-numeric: tabular-nums;
    }
    /* Separated from the totals above and emphasized, like the result of a balance sheet (BI-3) */
    .balance th,
    .balance td {
      border-top: 1px solid var(--mat-sys-outline-variant);
      padding-top: 10px;
    }
    .balance th {
      color: var(--mat-sys-on-surface);
    }
    .balance .value {
      font: var(--mat-sys-title-large);
      font-variant-numeric: tabular-nums;
    }
    /* Not part of the balance, so set apart from it by a dashed line (PM-14) */
    .saving-potential th,
    .saving-potential td {
      border-top: 1px dashed var(--mat-sys-outline-variant);
      padding-top: 10px;
    }
    .income {
      color: var(--app-income-color);
    }
    .expense {
      color: var(--mat-sys-error);
    }
    @media (max-width: 400px) {
      th,
      td {
        padding-left: 8px;
      }
      .value,
      .balance .value {
        font: var(--mat-sys-title-small);
        font-variant-numeric: tabular-nums;
      }
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
