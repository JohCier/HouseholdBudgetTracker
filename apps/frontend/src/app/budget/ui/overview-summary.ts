import { CurrencyPipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { HouseholdPlanOverview } from '../budget.model';
import { SavingPotentialSummary } from './saving-potential-summary';

/**
 * Income, expenses and balance of the plan in one box, per month and per year side by side (BI-1);
 * below them, apart from the balance, the saving potential of all expenses (PM-14). In a narrow box
 * the year stands below the month (BI-5). Left beside the box, the saving potential per subcategory, if
 * there is any (SU-6, SU-7); below the box where there is no room beside it (SU-8).
 */
@Component({
  selector: 'app-overview-summary',
  imports: [CurrencyPipe, NgTemplateOutlet, MatCardModule, MatIconModule, SavingPotentialSummary],
  template: `
    <!-- In reverse order, so the saving potentials stand left beside the box or below it (SU-6, SU-8) -->
    <div class="boxes">
      <mat-card appearance="outlined" class="balance-sheet">
        <mat-card-header>
          <mat-card-title id="balance-sheet-title">Bilanz</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <!-- Only one of both is displayed, depending on the width of the card (BI-5) -->
          <div class="side-by-side">
            <ng-container
              *ngTemplateOutlet="sheet; context: { $implicit: ['monthly', 'yearly'] }"
            />
          </div>
          <div class="stacked">
            <ng-container *ngTemplateOutlet="sheet; context: { $implicit: ['monthly'] }" />
            <ng-container *ngTemplateOutlet="sheet; context: { $implicit: ['yearly'] }" />
          </div>
          <ng-template #sheet let-periods>
            <table aria-labelledby="balance-sheet-title">
              <thead>
                <tr>
                  <td></td>
                  @for (period of asPeriods(periods); track period) {
                    <th scope="col">{{ periodLabels[period] }}</th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (row of rows(); track row.key) {
                  <tr [class]="row.key">
                    <th scope="row">{{ row.label }}</th>
                    @for (period of asPeriods(periods); track period) {
                      <td
                        class="value"
                        [class]="period"
                        [class.income]="row.key === 'total-income'"
                        [class.expense]="
                          row.key === 'total-expense' ||
                          (row.key === 'balance' && row.amounts[period].startsWith('-'))
                        "
                      >
                        {{ row.amounts[period] | currency: 'EUR' }}
                      </td>
                    }
                  </tr>
                }
              </tbody>
            </table>
          </ng-template>
        </mat-card-content>
      </mat-card>
      @if (overview().savingPotentialsBySubcategory.length) {
        <app-saving-potential-summary
          class="saving-potentials"
          [sums]="overview().savingPotentialsBySubcategory"
        />
      }
    </div>
    @if (overview().expensesExceedIncome) {
      <div class="warning" role="alert">
        <mat-icon svgIcon="warning" />
        Deine Ausgaben übersteigen deine Einnahmen.
      </div>
    }
  `,
  styles: `
    .boxes {
      display: flex;
      flex-flow: row-reverse wrap;
      justify-content: flex-end;
      align-items: flex-start;
      gap: 16px;
    }
    .balance-sheet,
    .saving-potentials {
      flex: 1 1 30rem;
      min-width: 0;
      max-width: 560px;
    }
    .saving-potentials {
      flex-basis: 20rem;
    }
    mat-card-content {
      container-type: inline-size;
    }
    .stacked {
      display: none;
    }
    /* Too narrow for label, month and year in one row, even with larger fonts (BI-5) */
    @container (max-width: 28em) {
      .side-by-side {
        display: none;
      }
      .stacked {
        display: grid;
        gap: 16px;
      }
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

  protected readonly periodLabels: Record<Period, string> = { monthly: 'Monat', yearly: 'Jahr' };

  protected readonly rows = computed<BalanceSheetRow[]>(() => {
    const overview = this.overview();
    return [
      {
        key: 'total-income',
        label: 'Gesamteinnahmen',
        amounts: { monthly: overview.monthlyTotalIncome, yearly: overview.totalIncome },
      },
      {
        key: 'total-expense',
        label: 'Gesamtausgaben',
        amounts: { monthly: overview.monthlyTotalExpense, yearly: overview.totalExpense },
      },
      {
        key: 'balance',
        label: 'Saldo',
        amounts: { monthly: overview.monthlyBalance, yearly: overview.balance },
      },
      {
        key: 'saving-potential',
        label: 'Sparpotential',
        amounts: { monthly: overview.monthlySavingPotential, yearly: overview.savingPotential },
      },
    ];
  });

  /** Types the periods passed to the template of a table. */
  protected asPeriods(periods: Period[]): Period[] {
    return periods;
  }
}

type Period = 'monthly' | 'yearly';

interface BalanceSheetRow {
  key: 'total-income' | 'total-expense' | 'balance' | 'saving-potential';
  label: string;
  amounts: Record<Period, string>;
}
