import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, forkJoin } from 'rxjs';
import {
  AddExpenseRequest,
  AddIncomeRequest,
  BudgetItem,
  BudgetOverview,
  Month,
} from '../budget.model';
import { addMonths, currentMonth, monthOf } from '../month';
import { BudgetApi } from './budget-api';

/**
 * Signal-based state holder for the budget of the selected month. The overview is derived
 * on the server, so every mutation is followed by a reload of items and overview.
 * Only the response of the latest load is applied, so overlapping reloads (e.g. quick
 * successive deletes or month changes) cannot leave stale data behind.
 * Mutations reject with an Error carrying a user-facing message.
 */
@Injectable({ providedIn: 'root' })
export class BudgetStore {
  private readonly api = inject(BudgetApi);

  private readonly _month = signal<Month>(currentMonth());
  private readonly _items = signal<BudgetItem[]>([]);
  private readonly _overview = signal<BudgetOverview | null>(null);
  private readonly _loading = signal(false);
  private readonly _loadError = signal<string | null>(null);
  private latestLoad = 0;

  readonly month = this._month.asReadonly();
  readonly items = this._items.asReadonly();
  readonly overview = this._overview.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly loadError = this._loadError.asReadonly();

  selectMonth(month: Month): Promise<void> {
    this._month.set(month);
    return this.load();
  }

  previousMonth(): Promise<void> {
    return this.selectMonth(addMonths(this._month(), -1));
  }

  nextMonth(): Promise<void> {
    return this.selectMonth(addMonths(this._month(), 1));
  }

  async load(): Promise<void> {
    const loadId = ++this.latestLoad;
    const month = this._month();
    this._loading.set(true);
    try {
      const { items, overview } = await firstValueFrom(
        forkJoin({ items: this.api.listItems(month), overview: this.api.getOverview(month) }),
      );
      if (loadId === this.latestLoad) {
        this._items.set(items);
        this._overview.set(overview);
        this._loadError.set(null);
      }
    } catch (error) {
      if (loadId === this.latestLoad) {
        this._loadError.set(toMessage(error));
      }
    } finally {
      if (loadId === this.latestLoad) {
        this._loading.set(false);
      }
    }
  }

  /** Adds an income and switches to the month it is booked in. */
  addIncome(request: AddIncomeRequest): Promise<void> {
    return this.mutate(this.api.addIncome(request), monthOf(request.bookingDate));
  }

  /** Adds an expense and switches to the month it is booked in. */
  addExpense(request: AddExpenseRequest): Promise<void> {
    return this.mutate(this.api.addExpense(request), monthOf(request.bookingDate));
  }

  removeItem(id: string): Promise<void> {
    return this.mutate(this.api.removeItem(id));
  }

  /** Re-creates a previously removed item (with a new id). */
  restoreItem(item: BudgetItem): Promise<void> {
    const request = {
      description: item.description,
      amount: item.amount,
      bookingDate: item.bookingDate,
    };
    return item.category === null
      ? this.addIncome(request)
      : this.addExpense({ ...request, category: item.category });
  }

  private async mutate(request: Observable<unknown>, month: Month = this._month()): Promise<void> {
    try {
      await firstValueFrom(request, { defaultValue: undefined });
    } catch (error) {
      throw new Error(toMessage(error));
    }
    await this.selectMonth(month);
  }
}

/** Extracts a user-facing message, preferring the `detail` of an RFC 9457 problem response. */
function toMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error?.detail === 'string') {
      return error.error.detail;
    }
    if (error.status === 0) {
      return 'Server nicht erreichbar.';
    }
    return `Anfrage fehlgeschlagen (${error.status}).`;
  }
  return 'Unbekannter Fehler.';
}
