import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { SubcategorySavingPotential } from '../budget.model';

/**
 * The saving potential per subcategory, as the backend sums it up and orders it (SU-1 to SU-4),
 * per month and per year side by side (SU-6). In a narrow box the name stands above both
 * amounts (SU-8). A long list scrolls within the box below its column headers (AP-12).
 */
@Component({
  selector: 'app-saving-potential-summary',
  imports: [CurrencyPipe, MatCardModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title id="saving-potential-title">Sparpotential je Unterkategorie</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @if (sums().length === 0) {
          <p class="empty">Noch kein Sparpotential markiert</p>
        } @else {
          <div class="scroll">
            <table aria-labelledby="saving-potential-title">
              <thead>
                <tr>
                  <td></td>
                  <th scope="col" class="monthly">Monat</th>
                  <th scope="col" class="yearly">Jahr</th>
                </tr>
              </thead>
              <tbody>
                @for (sum of sums(); track sum.subcategory) {
                  <tr>
                    <th scope="row">{{ sum.subcategory }}</th>
                    <td class="value monthly">{{ sum.monthlyAmount | currency: 'EUR' }}</td>
                    <td class="value yearly">{{ sum.amount | currency: 'EUR' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    :host {
      display: block;
    }
    mat-card-content {
      container-type: inline-size;
    }
    .empty {
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-medium);
    }
    /* About as high as the top 5 lists beside it (AP-12) */
    .scroll {
      max-height: 320px;
      overflow-y: auto;
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
      position: sticky;
      top: 0;
      background: var(--mat-sys-surface);
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
    /* Too narrow for name, month and year in one row: the name above both amounts (SU-8) */
    @container (max-width: 22em) {
      tr {
        display: grid;
        grid-template-columns: 1fr auto auto;
        column-gap: 12px;
      }
      thead td {
        display: none;
      }
      tbody th {
        grid-column: 1 / -1;
        padding-bottom: 0;
      }
      th,
      td {
        padding-left: 0;
      }
      .monthly {
        grid-column: 2;
        min-width: 6.5em;
      }
      .yearly {
        grid-column: 3;
        min-width: 6.5em;
      }
    }
  `,
})
export class SavingPotentialSummary {
  readonly sums = input.required<SubcategorySavingPotential[]>();
}
