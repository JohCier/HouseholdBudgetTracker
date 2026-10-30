import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreatedOneTimeItemResponse,
  MoveOneTimeItemRequest,
  OneTimeItemOverview,
  OneTimeItemRequest,
  Year,
} from '../budget.model';

/**
 * Thin HTTP adapter for the backend's /api/one-time-items endpoints (EP-1 to EP-17, ES-4). Changes
 * answer without the items; the caller reads the items of the year it shows again, since a
 * changed date may move an item to another year (EP-7).
 */
@Injectable({ providedIn: 'root' })
export class OneTimeItemApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/one-time-items';

  /** The one-time items of the user in a year with their totals; without items if there are none (EP-8, EP-9). */
  getOverview(year: Year): Observable<OneTimeItemOverview> {
    return this.http.get<OneTimeItemOverview>(this.baseUrl, { params: { year } });
  }

  createItem(request: OneTimeItemRequest): Observable<CreatedOneTimeItemResponse> {
    return this.http.post<CreatedOneTimeItemResponse>(this.baseUrl, request);
  }

  changeItem(itemId: string, request: OneTimeItemRequest): Observable<void> {
    return this.http.put<void>(this.itemUrl(itemId), request);
  }

  /** Moves an item between two neighbors of its area, the incomes or the expenses (ES-4). */
  moveItem(itemId: string, request: MoveOneTimeItemRequest): Observable<void> {
    return this.http.put<void>(`${this.itemUrl(itemId)}/position`, request);
  }

  deleteItem(itemId: string): Observable<void> {
    return this.http.delete<void>(this.itemUrl(itemId));
  }

  private itemUrl(itemId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(itemId)}`;
  }
}
