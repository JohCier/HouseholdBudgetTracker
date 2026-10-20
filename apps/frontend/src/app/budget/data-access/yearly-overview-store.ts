import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import { Year, YearlyOverviewEntry } from '../budget.model';
import { currentYear } from '../year';
import { BudgetApi } from './budget-api';

/**
 * Signal-based state holder for the yearly overview: all plans of the user, the newest year
 * first, with the changes from the previous year the backend works out (JU-1, JU-9). Offers to
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

  readonly currentYear: Year = currentYear();
  readonly years = computed(() => this._years() ?? []);
  readonly loaded = computed(() => this._years() !== undefined);
  /** True once it is known that the user has no plan at all (JU-13). */
  readonly empty = computed(() => this._years()?.length === 0);
  /** True once it is known that the current year has no plan yet (JU-12). */
  readonly currentYearMissing = computed(
    () => this.loaded() && !this.years().some((entry) => entry.year === this.currentYear),
  );
  readonly loading = this._loading.asReadonly();
  readonly loadError = this._loadError.asReadonly();

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
