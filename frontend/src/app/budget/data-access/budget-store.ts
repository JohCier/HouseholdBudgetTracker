import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, forkJoin } from 'rxjs';
import { AddExpenseRequest, AddIncomeRequest, BudgetItem, BudgetOverview } from '../budget.model';
import { BudgetApi } from './budget-api';

/**
 * Signal-based state holder for the budget. The overview is derived on the server,
 * so every mutation is followed by a reload of items and overview.
 * Mutations reject with an Error carrying a user-facing message.
 */
@Injectable({ providedIn: 'root' })
export class BudgetStore {
  private readonly api = inject(BudgetApi);

  private readonly _items = signal<BudgetItem[]>([]);
  private readonly _overview = signal<BudgetOverview | null>(null);
  private readonly _loading = signal(false);
  private readonly _loadError = signal<string | null>(null);

  readonly items = this._items.asReadonly();
  readonly overview = this._overview.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly loadError = this._loadError.asReadonly();

  async load(): Promise<void> {
    this._loading.set(true);
    try {
      const { items, overview } = await firstValueFrom(
        forkJoin({ items: this.api.listItems(), overview: this.api.getOverview() }),
      );
      this._items.set(items);
      this._overview.set(overview);
      this._loadError.set(null);
    } catch (error) {
      this._loadError.set(toMessage(error));
    } finally {
      this._loading.set(false);
    }
  }

  addIncome(request: AddIncomeRequest): Promise<void> {
    return this.mutate(this.api.addIncome(request));
  }

  addExpense(request: AddExpenseRequest): Promise<void> {
    return this.mutate(this.api.addExpense(request));
  }

  removeItem(id: string): Promise<void> {
    return this.mutate(this.api.removeItem(id));
  }

  private async mutate(request: Observable<unknown>): Promise<void> {
    try {
      await firstValueFrom(request, { defaultValue: undefined });
    } catch (error) {
      throw new Error(toMessage(error));
    }
    await this.load();
  }
}

function toMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error?.error === 'string') {
      return error.error.error;
    }
    if (error.status === 0) {
      return 'Server nicht erreichbar.';
    }
    return `Anfrage fehlgeschlagen (${error.status}).`;
  }
  return 'Unbekannter Fehler.';
}
