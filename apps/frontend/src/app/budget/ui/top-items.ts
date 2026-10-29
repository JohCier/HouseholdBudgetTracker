import { CurrencyPipe } from '@angular/common';
import { Component, computed, input, model } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { TopItem, TopItemsView } from '../budget.model';

/**
 * The card "Top 5": the expenses with the highest saving potential or the highest expenses, as the
 * backend selects and orders them (TP-1 to TP-5). A toggle switches between both lists (TP-7). Each
 * row shows its rank, the description with the subcategory below it and the amounts per month and
 * per year (TP-8); for the saving potential also the monthly amount of the expense (TP-9). In a
 * narrow card the description stands above the amounts (TP-11).
 */
@Component({
  selector: 'app-top-items',
  imports: [CurrencyPipe, MatButtonToggleModule, MatCardModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title id="top-items-title">Top 5</mat-card-title>
        <mat-card-subtitle>Die größten Hebel im Plan</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <mat-button-toggle-group
          class="view-toggle"
          hideSingleSelectionIndicator
          aria-label="Liste"
          [value]="view()"
          (change)="view.set($event.value)"
        >
          <mat-button-toggle value="savingPotential">Sparpotential</mat-button-toggle>
          <mat-button-toggle value="expenses">Ausgaben</mat-button-toggle>
        </mat-button-toggle-group>
        @if (rows().length === 0) {
          <p class="empty">
            {{
              view() === 'savingPotential'
                ? 'Noch kein Sparpotential markiert'
                : 'Noch keine Ausgaben'
            }}
          </p>
        } @else {
          <table aria-labelledby="top-items-title">
            <thead>
              <tr>
                <td></td>
                <th scope="col" class="monthly">Monat</th>
                <th scope="col" class="yearly">Jahr</th>
              </tr>
            </thead>
            <tbody>
              @for (row of rows(); track row.id; let rank = $index) {
                <tr>
                  <th scope="row">
                    <span class="rank">{{ rank + 1 }}.</span>
                    <span class="text">
                      <span class="description">{{ row.description }}</span>
                      <span class="detail">
                        {{ row.subcategory }}
                        @if (view() === 'savingPotential') {
                          · von {{ row.itemMonthlyAmount | currency: 'EUR' }}
                        }
                      </span>
                    </span>
                  </th>
                  <td class="value monthly">{{ row.monthlyAmount | currency: 'EUR' }}</td>
                  <td class="value yearly">{{ row.amount | currency: 'EUR' }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    mat-card-content {
      container-type: inline-size;
    }
    .view-toggle {
      margin: 8px 0 12px;
    }
    .empty {
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-medium);
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th,
    td {
      padding: 6px 0 6px 12px;
      vertical-align: top;
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
    tbody tr + tr {
      border-top: 1px solid var(--mat-sys-outline-variant);
    }
    tbody th {
      display: flex;
      gap: 8px;
      text-align: left;
      font-weight: normal;
    }
    .rank {
      min-width: 1.4em;
      font: var(--mat-sys-title-medium);
      color: var(--mat-sys-on-surface-variant);
    }
    .text {
      display: flex;
      flex-direction: column;
      min-width: 0;
    }
    .description {
      font: var(--mat-sys-body-large);
    }
    .detail {
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
    }
    .value {
      text-align: right;
      white-space: nowrap;
      font: var(--mat-sys-title-medium);
      font-variant-numeric: tabular-nums;
    }
    /* Too narrow for description, month and year in one row: the description above both amounts (TP-11) */
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
export class TopItems {
  readonly savingPotentials = input.required<TopItem[]>();
  readonly expenses = input.required<TopItem[]>();
  readonly view = model<TopItemsView>('savingPotential');

  protected readonly rows = computed(() =>
    this.view() === 'savingPotential' ? this.savingPotentials() : this.expenses(),
  );
}
