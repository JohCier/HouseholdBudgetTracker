import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import { OneTimeItem, OneTimeItemOverview, OneTimeItemRequest, Year } from '../budget.model';
import { currentYear } from '../year';
import { OneTimeItemApi } from './one-time-item-api';

/**
 * Signal-based state holder for the one-time items of the year shown in the dashboard (EP-8 to
 * EP-10), independent of whether the year has a plan (EP-5). After every change the items of the
 * year are read again, since a changed date may move an item to another year (EP-7). Only the
 * latest load is applied, so a slow answer for a year no longer shown cannot replace the current
 * one. Mutations reject with an Error carrying a user-facing message.
 *
 * Provided by the dashboard, like the {@link BudgetStore}.
 */
@Injectable()
export class OneTimeItemStore {
  private readonly api = inject(OneTimeItemApi);

  private readonly _year = signal<Year>(currentYear());
  /** `undefined` until the items of the selected year have been loaded. */
  private readonly _overview = signal<OneTimeItemOverview | undefined>(undefined);
  private readonly _loadError = signal<string | null>(null);
  private latestLoad = 0;

  readonly year = this._year.asReadonly();
  readonly overview = computed(() => this._overview() ?? null);
  readonly items = computed<OneTimeItem[]>(() => this._overview()?.items ?? []);
  readonly loadError = this._loadError.asReadonly();

  selectYear(year: Year): Promise<void> {
    this._year.set(year);
    this._overview.set(undefined);
    return this.load();
  }

  async load(): Promise<void> {
    const loadId = ++this.latestLoad;
    try {
      const overview = await firstValueFrom(this.api.getOverview(this._year()));
      if (loadId === this.latestLoad) {
        this._overview.set(overview);
        this._loadError.set(null);
      }
    } catch (error) {
      if (loadId === this.latestLoad) {
        this._loadError.set(toMessage(error));
      }
    }
  }

  async createItem(request: OneTimeItemRequest): Promise<void> {
    await this.mutate(this.api.createItem(request));
  }

  async changeItem(itemId: string, request: OneTimeItemRequest): Promise<void> {
    await this.mutate(this.api.changeItem(itemId, request));
  }

  async deleteItem(itemId: string): Promise<void> {
    await this.mutate(this.api.deleteItem(itemId));
  }

  private async mutate(request: Observable<unknown>): Promise<void> {
    try {
      await firstValueFrom(request);
    } catch (error) {
      throw new Error(toMessage(error));
    }
    await this.load();
  }
}
