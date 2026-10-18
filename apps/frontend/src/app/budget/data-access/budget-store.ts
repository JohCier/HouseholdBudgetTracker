import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import {
  AddExpenseRequest,
  AddIncomeRequest,
  BudgetItem,
  ChangeBudgetItemRequest,
  HouseholdPlanOverview,
  Year,
} from '../budget.model';
import { FIRST_YEAR, LAST_YEAR, currentYear } from '../year';
import { BudgetApi } from './budget-api';

/**
 * Signal-based state holder for the household plan of the selected year. A year may not
 * have a plan yet; items can only be added once one has been created. The overview,
 * including the items, is derived on the server, so every mutation is followed by a
 * reload of it. Only the response of the latest load is applied, so overlapping reloads
 * (e.g. quick successive deletes or year changes) cannot leave stale data behind.
 * Mutations reject with an Error carrying a user-facing message.
 *
 * Provided by the dashboard, so the state lives only as long as the dashboard is shown and
 * never outlasts the session of the user it belongs to.
 */
@Injectable()
export class BudgetStore {
  private readonly api = inject(BudgetApi);

  private readonly _year = signal<Year>(currentYear());
  /** `undefined` until the overview of the selected year has been loaded, `null` without plan. */
  private readonly _overview = signal<HouseholdPlanOverview | null | undefined>(undefined);
  private readonly _loading = signal(false);
  private readonly _loadError = signal<string | null>(null);
  private latestLoad = 0;

  readonly year = this._year.asReadonly();
  /** Whether there is a year before resp. after the selected one that can have a plan (HP-1). */
  readonly hasPreviousYear = computed(() => this._year() > FIRST_YEAR);
  readonly hasNextYear = computed(() => this._year() < LAST_YEAR);
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

  /** Selects the previous year; does nothing at the first year a plan can exist for. */
  async previousYear(): Promise<void> {
    if (this.hasPreviousYear()) {
      await this.selectYear(this._year() - 1);
    }
  }

  /** Selects the next year; does nothing at the last year a plan can exist for. */
  async nextYear(): Promise<void> {
    if (this.hasNextYear()) {
      await this.selectYear(this._year() + 1);
    }
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

  /** Gives an item of the selected year's plan new values; it keeps its id and position. */
  async changeItem(itemId: string, request: ChangeBudgetItemRequest): Promise<void> {
    return this.mutate(this.api.changeItem(this.requirePlanId(), itemId, request));
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
