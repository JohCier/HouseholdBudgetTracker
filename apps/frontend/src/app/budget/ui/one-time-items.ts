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
  templateUrl: './one-time-items.html',
  styleUrl: './one-time-items.scss',
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
