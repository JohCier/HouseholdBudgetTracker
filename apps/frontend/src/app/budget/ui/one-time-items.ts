import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList } from '@angular/cdk/drag-drop';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ItemType, OneTimeItem, OneTimeItemOverview } from '../budget.model';

/** A one-time item to be moved within its area, from one place to another (ES-4). */
export interface OneTimeItemMove {
  item: OneTimeItem;
  from: number;
  to: number;
}

/** The incomes or the expenses of the year, each in an area of their own (ES-1). */
interface Area {
  type: ItemType;
  label: string;
  empty: string;
  total: string;
  sign: string;
  items: OneTimeItem[];
}

/**
 * The section "Einmalposten" of the dashboard (EP-8 to EP-10, ES-1 to ES-4): the incomes above,
 * the expenses below, each in an area with its sum, and the balance under both. Within an area the
 * user moves the items by dragging their handle or with the arrow keys on it. Shown whether or not
 * the year has a plan. Changing, moving and deleting are left to the dashboard, which also offers
 * "Einmalposten erfassen" above the section, like "Posten erfassen" (AP-18).
 */
@Component({
  selector: 'app-one-time-items',
  imports: [
    CdkDrag,
    CdkDragHandle,
    CdkDropList,
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
            @for (area of areas(); track area.type) {
              <section
                class="area"
                [class.income-area]="area.type === 'INCOME'"
                [attr.aria-labelledby]="'one-time-' + area.type"
              >
                <h3 [id]="'one-time-' + area.type">{{ area.label }}</h3>
                @if (area.items.length === 0) {
                  <p class="empty">{{ area.empty }}</p>
                } @else {
                  <div
                    class="list"
                    role="list"
                    [attr.aria-label]="area.label"
                    cdkDropList
                    cdkDropListLockAxis="y"
                    (cdkDropListDropped)="drop(area, $event)"
                  >
                    @for (item of area.items; track item.id; let index = $index) {
                      <div class="item" role="listitem" cdkDrag>
                        @if (area.items.length > 1) {
                          <button
                            matIconButton
                            type="button"
                            class="handle"
                            cdkDragHandle
                            matTooltip="Ziehen zum Verschieben"
                            [attr.aria-label]="'Einmalposten ' + item.description + ' verschieben'"
                            (keydown.arrowup)="moveBy(area, index, -1, $event)"
                            (keydown.arrowdown)="moveBy(area, index, 1, $event)"
                          >
                            <mat-icon svgIcon="drag_indicator" />
                          </button>
                        } @else {
                          <span class="handle-space"></span>
                        }
                        <span class="date">{{ item.date | date: 'dd.MM.yyyy' }}</span>
                        <div class="description">
                          {{ item.description }}
                          @if (item.note) {
                            <div class="note">{{ item.note }}</div>
                          }
                        </div>
                        <span
                          class="amount"
                          [class.income]="item.type === 'INCOME'"
                          [class.expense]="item.type === 'EXPENSE'"
                        >
                          {{ area.sign }}{{ item.amount | currency: 'EUR' }}
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
                }
                <div class="sum">
                  <span>Summe {{ area.label }}</span>
                  <span
                    class="amount"
                    [class.income]="area.type === 'INCOME'"
                    [class.expense]="area.type === 'EXPENSE'"
                  >
                    {{ signOf(area.total, area.sign) }}{{ area.total | currency: 'EUR' }}
                  </span>
                </div>
              </section>
            }
            <div class="balance-row">
              <span>Saldo</span>
              <span class="amount" [class.negative]="overview.expensesExceedIncome">
                {{ overview.balance | currency: 'EUR' }}
              </span>
            </div>
          }
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
    .area .empty {
      padding: 8px 0;
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
    /* Incomes green, expenses red, as in the entries (ES-1) */
    .area {
      display: flex;
      flex-direction: column;
      padding: 4px 12px;
      border-left: 4px solid var(--mat-sys-error);
      border-radius: 0 12px 12px 0;
      background: var(--mat-sys-surface-container-low);
    }
    .area.income-area {
      border-left-color: var(--app-income-color);
    }
    .area h3 {
      margin: 8px 0 4px;
      font: var(--mat-sys-title-small);
    }
    .list {
      display: flex;
      flex-direction: column;
    }
    .item {
      display: grid;
      grid-template-columns: 40px 6.5rem minmax(0, 1fr) 8rem 96px;
      align-items: center;
      gap: 8px;
      min-height: 48px;
      background: var(--mat-sys-surface-container-low);
    }
    .item:not(:last-child) {
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .handle {
      cursor: grab;
      color: var(--mat-sys-on-surface-variant);
    }
    .description {
      overflow-wrap: anywhere;
    }
    .date {
      color: var(--mat-sys-on-surface-variant);
      font-variant-numeric: tabular-nums;
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
    .sum,
    .balance-row {
      display: flex;
      justify-content: space-between;
      gap: 8px;
      font-weight: 500;
    }
    .sum {
      padding: 8px 104px 8px 0;
      border-top: 1px solid var(--mat-sys-outline-variant);
    }
    .balance-row {
      padding: 8px 120px 8px 16px;
      border-radius: 8px;
      background: var(--mat-sys-surface-container-high);
    }
    .cdk-drag-preview {
      background: var(--mat-sys-surface);
      box-shadow: var(--mat-sys-level3);
    }
    .cdk-drag-placeholder {
      opacity: 0.4;
    }
    .cdk-drag-animating,
    .cdk-drop-list-dragging .cdk-drag:not(.cdk-drag-placeholder) {
      transition: transform 200ms cubic-bezier(0, 0, 0.2, 1);
    }
    /* Two lines on a narrow card: day above, description and amount below (MA-1, EP-18) */
    @container (max-width: 520px) {
      .item {
        grid-template-columns: 40px minmax(0, 1fr) auto auto;
        grid-template-areas:
          'handle date date actions'
          'handle description amount actions';
        row-gap: 0;
        padding: 4px 0;
      }
      .handle,
      .handle-space {
        grid-area: handle;
      }
      .date {
        grid-area: date;
        font: var(--mat-sys-body-small);
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
      .sum {
        padding-right: 56px;
      }
      .balance-row {
        padding-right: 72px;
      }
    }
  `,
})
export class OneTimeItems {
  /** The items of the year shown; `null` while they load. */
  readonly overview = input.required<OneTimeItemOverview | null>();
  readonly loadError = input<string | null>(null);

  readonly edit = output<OneTimeItem>();
  readonly move = output<OneTimeItemMove>();
  readonly remove = output<OneTimeItem>();
  readonly retry = output<void>();

  /** The incomes above, the expenses below, each in the order the user set (ES-1, ES-4). */
  protected readonly areas = computed<Area[]>(() => {
    const overview = this.overview();
    if (!overview) {
      return [];
    }
    return [
      {
        type: 'INCOME',
        label: 'Einnahmen',
        empty: 'Keine einmaligen Einnahmen.',
        total: overview.totalIncome,
        sign: '+',
        items: overview.items.filter((item) => item.type === 'INCOME'),
      },
      {
        type: 'EXPENSE',
        label: 'Ausgaben',
        empty: 'Keine einmaligen Ausgaben.',
        total: overview.totalExpense,
        sign: '−',
        items: overview.items.filter((item) => item.type === 'EXPENSE'),
      },
    ];
  });

  protected drop(area: Area, event: CdkDragDrop<unknown>): void {
    this.moveWithin(area, event.previousIndex, event.currentIndex);
  }

  /** Moves an item one place up or down within its area with the arrow keys on its handle. */
  protected moveBy(area: Area, index: number, by: number, event: Event): void {
    event.preventDefault();
    this.moveWithin(area, index, index + by);
  }

  /** The sign of a total of incomes or expenses; none for 0,00 €. */
  protected signOf(amount: string, sign: string): string {
    return Number(amount) > 0 ? sign : '';
  }

  private moveWithin(area: Area, from: number, to: number): void {
    if (from !== to && to >= 0 && to < area.items.length) {
      this.move.emit({ item: area.items[from], from, to });
    }
  }
}
