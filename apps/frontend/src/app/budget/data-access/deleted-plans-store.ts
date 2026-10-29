import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import { DeletedPlan } from '../budget.model';
import { BudgetApi } from './budget-api';

/**
 * Signal-based state holder for the deleted plans of the user, in the order the backend sorts
 * them: the newest year first, within a year the latest deleted first (GP-22, GP-24).
 *
 * Provided by the page, so the list is loaded afresh whenever it is shown.
 */
@Injectable()
export class DeletedPlansStore {
  private readonly api = inject(BudgetApi);

  /** `undefined` until loaded. */
  private readonly _plans = signal<DeletedPlan[] | undefined>(undefined);
  private readonly _loading = signal(false);
  private readonly _loadError = signal<string | null>(null);

  readonly plans = computed(() => this._plans() ?? []);
  /** True once it is known that there is no deleted plan (GP-25). */
  readonly empty = computed(() => this._plans()?.length === 0);
  readonly loading = this._loading.asReadonly();
  readonly loadError = this._loadError.asReadonly();

  async load(): Promise<void> {
    this._loading.set(true);
    try {
      const deleted = await firstValueFrom(this.api.getDeletedPlans());
      this._plans.set(deleted.plans);
      this._loadError.set(null);
    } catch (error) {
      this._loadError.set(toMessage(error));
    } finally {
      this._loading.set(false);
    }
  }
}
