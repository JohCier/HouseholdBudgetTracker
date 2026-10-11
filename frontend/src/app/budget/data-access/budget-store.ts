import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import {
  AddExpenseRequest,
  AddIncomeRequest,
  BudgetItem,
  HouseholdPlanOverview,
  Year,
} from '../budget.model';
import { currentYear } from '../year';
import { BudgetApi } from './budget-api';

/**
 * Signal-based state holder for the household plan of the selected year. A year may not
 * have a plan yet; items can only be added once one has been created. The overview,
 * including the items, is derived on the server, so every mutation is followed by a
 * reload of it. Only the response of the latest load is applied, so overlapping reloads
 * (e.g. quick successive deletes or year changes) cannot leave stale data behind.
 * Mutations reject with an Error carrying a user-facing message.
 */
@Injectable({ providedIn: 'root' })
export class BudgetStore {
  private readonly api = inject(BudgetApi);

  private readonly _year = signal<Year>(currentYear());
  /** `undefined` until the overview of the selected year has been loaded, `null` without plan. */
  private readonly _overview = signal<HouseholdPlanOverview | null | undefined>(undefined);
  private readonly _loading = signal(false);
  private readonly _loadError = signal<string | null>(null);
  private latestLoad = 0;

  readonly year = this._year.asReadonly();
  readonly overview = computed(() => this._overview() ?? null);
  readonly planId = computed(() => this._overview()?.id ?? null);
  /** True once it is known that the selected year has no plan yet. */
  readonly planMissing = computed(() => this._overview() === null);
  readonly items = computed<BudgetItem[]>(() => this._overview()?.items ?? []);
  readonly loading = this._loading.asReadonly();
  readonly loadError = this._loadError.asReadonly();

  selectYear(year: Year): Promise<void> {
    this._year.set(year);
    return this.load();
  }

  previousYear(): Promise<void> {
    return this.selectYear(this._year() - 1);
  }

  nextYear(): Promise<void> {
    return this.selectYear(this._year() + 1);
  }

  async load(): Promise<void> {
    const loadId = ++this.latestLoad;
    const year = this._year();
    this._loading.set(true);
    try {
      const overview = await firstValueFrom(this.api.getOverview(year));
      if (loadId === this.latestLoad) {
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

  /** Creates the household plan of the selected year. */
  createPlan(): Promise<void> {
    return this.mutate(this.api.createPlan(this._year()));
  }

  async addIncome(request: AddIncomeRequest): Promise<void> {
    return this.mutate(this.api.addIncome(this.requirePlanId(), request));
  }

  async addExpense(request: AddExpenseRequest): Promise<void> {
    return this.mutate(this.api.addExpense(this.requirePlanId(), request));
  }

  async removeItem(itemId: string): Promise<void> {
    return this.mutate(this.api.removeItem(this.requirePlanId(), itemId));
  }

  /**
   * Re-creates a previously removed item (with a new id) in the plan it was removed from,
   * even if another year has been selected in the meantime.
   */
  restoreItem(planId: string, item: BudgetItem): Promise<void> {
    const request = { description: item.description, amount: item.amount };
    return this.mutate(
      item.category === null
        ? this.api.addIncome(planId, request)
        : this.api.addExpense(planId, { ...request, category: item.category }),
    );
  }

  private requirePlanId(): string {
    const planId = this.planId();
    if (!planId) {
      throw new Error(`Für ${this._year()} gibt es noch keinen Haushaltsplan.`);
    }
    return planId;
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
