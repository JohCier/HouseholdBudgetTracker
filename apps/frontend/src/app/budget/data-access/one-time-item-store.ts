import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import { OneTimeItem, OneTimeItemOverview, OneTimeItemRequest, Year } from '../budget.model';
import { currentYear } from '../../shared/year';
import { OneTimeItemApi } from './one-time-item-api';

/**
 * Signal-based state holder for the one-time items of the year shown in the dashboard (EP-8 to
 * EP-10, ES-1 to ES-11), independent of whether the year has a plan (EP-5). After every change
 * the items of the year are read again, since a changed date may move an item to another year
 * (EP-7). Only the latest load is applied, so a slow answer for a year no longer shown cannot replace the current
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

  /**
   * Moves an item within its area, the incomes or the expenses, from one place to another (ES-4).
   * The new order shows right away; should saving it fail, the items are loaded again, so the
   * stored order shows (ES-9).
   */
  async moveItem(item: OneTimeItem, from: number, to: number): Promise<void> {
    const overview = this._overview();
    if (!overview || from === to) {
      return;
    }
    const area = overview.items.filter((other) => other.type === item.type);
    const moved = area.splice(from, 1)[0];
    if (moved?.id !== item.id || to < 0 || to > area.length) {
      return;
    }
    area.splice(to, 0, item);
    const others = overview.items.filter((other) => other.type !== item.type);
    this._overview.set({ ...overview, items: [...others, ...area] });
    const previous = area[to - 1];
    const next = area[to + 1];
    try {
      await this.mutate(
        this.api.moveItem(item.id, {
          ...(previous ? { previousItemId: previous.id } : {}),
          ...(next ? { nextItemId: next.id } : {}),
        }),
      );
    } catch (error) {
      void this.load();
      throw error;
    }
  }

  /**
   * Enters a deleted item again with everything it had; like a new item it comes last in its
   * area (ES-11).
   */
  async restoreItem(item: OneTimeItem): Promise<void> {
    await this.createItem({
      type: item.type,
      description: item.description,
      amount: item.amount,
      date: item.date,
      ...(item.note ? { note: item.note } : {}),
    });
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
