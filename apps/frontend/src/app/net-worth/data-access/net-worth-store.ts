import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { Year } from '../../shared/year';
import { FIRST_YEAR, LAST_YEAR, currentYear } from '../../shared/year';
import { toMessage } from '../../shared/http-error';
import { NetWorthEntryRequest, NetWorthOverview, isRecorded } from '../net-worth.model';
import { NetWorthApi } from './net-worth-api';

/**
 * Signal-based state holder for the page "Vermögen" of the selected year (VS-8). Every change answers
 * with the overview of the year after it, which is shown without reloading; only the latest response
 * is applied. Selecting another year forgets the overview of the previous one right away.
 * Changes reject with an Error carrying a user-facing message.
 *
 * Provided by the page, so the state lives only as long as the page is shown.
 */
@Injectable()
export class NetWorthStore {
  private readonly api = inject(NetWorthApi);

  private readonly _year = signal<Year>(currentYear());
  /** `undefined` until the overview of the selected year has been loaded. */
  private readonly _overview = signal<NetWorthOverview | undefined>(undefined);
  private readonly _loading = signal(false);
  private readonly _loadError = signal<string | null>(null);
  private latestRequest = 0;

  readonly year = this._year.asReadonly();
  readonly hasPreviousYear = computed(() => this._year() > FIRST_YEAR);
  readonly hasNextYear = computed(() => this._year() < LAST_YEAR);
  readonly overview = computed(() => this._overview() ?? null);
  /** True once it is known that the selected year has no assets or liabilities (VS-10). */
  readonly empty = computed(() => {
    const overview = this._overview();
    return overview !== undefined && !isRecorded(overview);
  });
  readonly loading = this._loading.asReadonly();
  readonly loadError = this._loadError.asReadonly();

  selectYear(year: Year): Promise<void> {
    this._year.set(year);
    this._overview.set(undefined);
    return this.load();
  }

  async load(): Promise<void> {
    const request = ++this.latestRequest;
    this._loading.set(true);
    try {
      const overview = await firstValueFrom(this.api.getOverview(this._year()));
      if (request === this.latestRequest) {
        this._overview.set(overview);
        this._loadError.set(null);
      }
    } catch (error) {
      if (request === this.latestRequest) {
        this._loadError.set(toMessage(error));
      }
    } finally {
      if (request === this.latestRequest) {
        this._loading.set(false);
      }
    }
  }

  addEntry(request: NetWorthEntryRequest): Promise<void> {
    return this.apply(this.api.addEntry(this._year(), request));
  }

  changeEntry(entryId: string, request: NetWorthEntryRequest): Promise<void> {
    return this.apply(this.api.changeEntry(this._year(), entryId, request));
  }

  removeEntry(entryId: string): Promise<void> {
    return this.apply(this.api.removeEntry(this._year(), entryId));
  }

  private async apply(change: Observable<NetWorthOverview>): Promise<void> {
    const request = ++this.latestRequest;
    const year = this._year();
    try {
      const overview = await firstValueFrom(change);
      if (request === this.latestRequest && year === this._year()) {
        this._overview.set(overview);
      }
    } catch (error) {
      throw new Error(toMessage(error));
    }
  }
}
