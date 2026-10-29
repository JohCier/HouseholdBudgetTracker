import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OneTimeItem, OneTimeItemOverview } from '../budget.model';

/**
 * The section "Einmalposten" of the dashboard (EP-8 to EP-10): the one-time items of the year
 * shown by date, each with its note, and their totals below. Shown whether or not the year has a
 * plan. Entering, changing and deleting are left to the dashboard, which opens the dialogs.
 */
@Component({
  selector: 'app-one-time-items',
  imports: [
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
  ],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>Einmalposten</mat-card-title>
        <mat-card-subtitle
          >Einmalige Einnahmen und Ausgaben, nicht Teil des Haushaltsplans</mat-card-subtitle
        >
      </mat-card-header>
      <mat-card-content>
        @if (loadError(); as error) {
          <div class="load-error" role="alert">
            <span>Einmalposten konnten nicht geladen werden: {{ error }}</span>
            <button matButton type="button" (click)="retry.emit()">Erneut versuchen</button>
          </div>
        } @else if (overview(); as overview) {
          @if (overview.items.length === 0) {
            <p class="empty">Keine Einmalposten.</p>
          } @else {
            <div class="list" role="list" aria-label="Einmalposten">
              @for (item of overview.items; track item.id) {
                <div class="item" role="listitem">
                  <span class="date">{{ item.date | date: 'dd.MM.yyyy' }}</span>
                  <div class="description">
                    {{ item.description }}
                    @if (item.note) {
                      <div class="note">{{ item.note }}</div>
                    }
                  </div>
                  <span class="type">{{ item.type === 'INCOME' ? 'Einnahme' : 'Ausgabe' }}</span>
                  <span
                    class="amount"
                    [class.income]="item.type === 'INCOME'"
                    [class.expense]="item.type === 'EXPENSE'"
                  >
                    {{ item.type === 'EXPENSE' ? '−' : '+' }}{{ item.amount | currency: 'EUR' }}
                  </span>
                  <span class="actions">
                    <button
                      matIconButton
                      type="button"
                      matTooltip="Bearbeiten"
                      [attr.aria-label]="'Einmalposten ' + item.description + ' bearbeiten'"
                      (click)="edit.emit(item)"
                    >
                      <mat-icon svgIcon="edit" />
                    </button>
                    <button
                      matIconButton
                      type="button"
                      matTooltip="Löschen"
                      [attr.aria-label]="'Einmalposten ' + item.description + ' löschen'"
                      (click)="remove.emit(item)"
                    >
                      <mat-icon svgIcon="delete" />
                    </button>
                  </span>
                </div>
              }
            </div>
            <dl class="totals" aria-label="Summen der Einmalposten">
              <div class="total-row">
                <dt>Einnahmen</dt>
                <dd class="amount income">
                  {{ signOf(overview.totalIncome, '+')
                  }}{{ overview.totalIncome | currency: 'EUR' }}
                </dd>
              </div>
              <div class="total-row">
                <dt>Ausgaben</dt>
                <dd class="amount expense">
                  {{ signOf(overview.totalExpense, '−')
                  }}{{ overview.totalExpense | currency: 'EUR' }}
                </dd>
              </div>
              <div class="total-row balance">
                <dt>Saldo</dt>
                <dd class="amount" [class.negative]="overview.expensesExceedIncome">
                  {{ overview.balance | currency: 'EUR' }}
                </dd>
              </div>
            </dl>
          }
          <button matButton="outlined" type="button" class="add" (click)="add.emit()">
            <mat-icon svgIcon="add" />
            Einmalposten erfassen
          </button>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    /* The layout follows the width of the card, as in the entries (MA-2) */
    :host {
      display: block;
      container-type: inline-size;
    }
    mat-card-content {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding-top: 16px;
    }
    .empty {
      margin: 0;
      color: var(--mat-sys-on-surface-variant);
    }
    .load-error {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 12px 16px;
      border-radius: 12px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    .list {
      display: flex;
      flex-direction: column;
      padding-left: 12px;
      border-left: 4px solid var(--mat-sys-outline);
    }
    .item {
      display: grid;
      grid-template-columns: 6.5rem minmax(0, 1fr) auto 8rem 96px;
      align-items: center;
      gap: 8px;
      min-height: 48px;
    }
    .item:not(:last-child) {
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .description {
      overflow-wrap: anywhere;
    }
    .date,
    .type {
      color: var(--mat-sys-on-surface-variant);
      font-variant-numeric: tabular-nums;
    }
    .type {
      font: var(--mat-sys-body-small);
    }
    .note {
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
      white-space: pre-line;
    }
    .amount {
      margin: 0;
      text-align: right;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
      font-weight: 500;
    }
    .income {
      color: var(--app-income-color);
    }
    .expense,
    .negative {
      color: var(--mat-sys-error);
    }
    .actions {
      white-space: nowrap;
    }
    .totals {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin: 0 96px 0 0;
      padding: 8px 12px;
      border-radius: 8px;
      background: var(--mat-sys-surface-container-high);
    }
    .total-row {
      display: flex;
      justify-content: space-between;
      gap: 8px;
    }
    .balance {
      border-top: 1px solid var(--mat-sys-outline-variant);
      padding-top: 4px;
      font-weight: 500;
    }
    .add {
      align-self: flex-start;
    }
    /* Two lines on a narrow card: day and type above, description and amount below (MA-1, EP-18) */
    @container (max-width: 520px) {
      .item {
        grid-template-columns: minmax(0, 1fr) auto auto;
        grid-template-areas:
          'date type actions'
          'description amount actions';
        row-gap: 0;
        padding: 4px 0;
      }
      .date {
        grid-area: date;
        font: var(--mat-sys-body-small);
      }
      .type {
        grid-area: type;
        text-align: right;
      }
      .description {
        grid-area: description;
      }
      .amount {
        grid-area: amount;
      }
      .actions {
        grid-area: actions;
        display: flex;
        flex-direction: column;
      }
      .totals {
        margin-right: 48px;
      }
    }
  `,
})
export class OneTimeItems {
  /** The items of the year shown; `null` while they load. */
  readonly overview = input.required<OneTimeItemOverview | null>();
  readonly loadError = input<string | null>(null);

  readonly add = output<void>();
  readonly edit = output<OneTimeItem>();
  readonly remove = output<OneTimeItem>();
  readonly retry = output<void>();

  /** The sign of a total of incomes or expenses; none for 0,00 €. */
  protected signOf(amount: string, sign: string): string {
    return Number(amount) > 0 ? sign : '';
  }
}
