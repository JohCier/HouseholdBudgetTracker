import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import { AmountPeriod, Year, YearlyOverviewEntry } from '../budget.model';
import { currentYear } from '../year';
import { BudgetApi } from './budget-api';

/** Where the browser remembers whether the overview shows monthly or yearly amounts (JM-7). */
export const PERIOD_STORAGE_KEY = 'hbt.yearlyOverview.period';

/**
 * Signal-based state holder for the yearly overview: all years of the user with a plan or one-time
 * items, the newest year first, with the changes from the previous year and the one-time totals the
 * backend works out (JU-1, JU-9, EP-12, EP-13). Offers to
 * create the plan of the current year as long as there is none (JU-12).
 *
 * Provided by the page, so the overview is loaded afresh whenever it is shown and reflects the
 * changes made to a plan in the meantime (JU-11).
 */
@Injectable()
export class YearlyOverviewStore {
  private readonly api = inject(BudgetApi);

  /** `undefined` until loaded. */
  private readonly _years = signal<YearlyOverviewEntry[] | undefined>(undefined);
  private readonly _loading = signal(false);
  private readonly _loadError = signal<string | null>(null);
  private readonly _period = signal<AmountPeriod>(storedPeriod());

  readonly currentYear: Year = currentYear();
  readonly years = computed(() => this._years() ?? []);
  readonly loaded = computed(() => this._years() !== undefined);
  /** Whether the user has a plan for at least one year, not only one-time items (EP-12). */
  readonly hasPlans = computed(() => this.years().some(hasPlan));
  /** True once it is known that the user has no plan at all, though maybe one-time items (JU-13). */
  readonly empty = computed(() => this.loaded() && !this.hasPlans());
  /** True once it is known that the current year has no plan yet (JU-12). */
  readonly currentYearMissing = computed(
    () =>
      this.loaded() &&
      !this.years().some((entry) => entry.year === this.currentYear && hasPlan(entry)),
  );
  readonly loading = this._loading.asReadonly();
  readonly loadError = this._loadError.asReadonly();
  /** Whether the amounts are shown per month or per year; monthly unless chosen otherwise (JM-7). */
  readonly period = this._period.asReadonly();

  /** Switches between monthly and yearly amounts and remembers the choice in the browser (JM-1, JM-7). */
  setPeriod(period: AmountPeriod): void {
    this._period.set(period);
    try {
      localStorage.setItem(PERIOD_STORAGE_KEY, period);
    } catch {
      // Without storage the choice only lasts as long as the page
    }
  }

  async load(): Promise<void> {
    this._loading.set(true);
    try {
      const overview = await firstValueFrom(this.api.getYearlyOverview());
      this._years.set(overview.years);
      this._loadError.set(null);
    } catch (error) {
      this._loadError.set(toMessage(error));
    } finally {
      this._loading.set(false);
    }
  }

  /** Creates the plan of the current year; rejects with an Error carrying a user-facing message. */
  async createCurrentYearPlan(): Promise<Year> {
    try {
      await firstValueFrom(this.api.createPlan(this.currentYear));
      return this.currentYear;
    } catch (error) {
      throw new Error(toMessage(error));
    }
  }
}

/** Whether the year has a plan, not only one-time items (EP-12). */
export function hasPlan(entry: YearlyOverviewEntry): boolean {
  return entry.planId !== null;
}

function storedPeriod(): AmountPeriod {
  try {
    return localStorage.getItem(PERIOD_STORAGE_KEY) === 'YEARLY' ? 'YEARLY' : 'MONTHLY';
  } catch {
    return 'MONTHLY';
  }
}
