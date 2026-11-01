import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Year } from '../../shared/year';
import {
  CopyNetWorthRequest,
  NetWorthEntryRequest,
  NetWorthHistory,
  NetWorthOverview,
} from '../net-worth.model';

/**
 * Thin HTTP adapter for the backend's /api/net-worth endpoints. Assets and liabilities are addressed
 * by their year; every change answers with the overview of the year after it.
 */
@Injectable({ providedIn: 'root' })
export class NetWorthApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/net-worth';

  /** The assets and liabilities of a year; without entries if none are recorded (VS-10). */
  getOverview(year: Year): Observable<NetWorthOverview> {
    return this.http.get<NetWorthOverview>(`${this.baseUrl}/${year}`);
  }

  /** All years with assets or liabilities, the newest first, with their changes (VS-18). */
  getHistory(): Observable<NetWorthHistory> {
    return this.http.get<NetWorthHistory>(`${this.baseUrl}/years`);
  }

  addEntry(year: Year, request: NetWorthEntryRequest): Observable<NetWorthOverview> {
    return this.http.post<NetWorthOverview>(`${this.baseUrl}/${year}/entries`, request);
  }

  changeEntry(
    year: Year,
    entryId: string,
    request: NetWorthEntryRequest,
  ): Observable<NetWorthOverview> {
    return this.http.put<NetWorthOverview>(`${this.baseUrl}/${year}/entries/${entryId}`, request);
  }

  removeEntry(year: Year, entryId: string): Observable<NetWorthOverview> {
    return this.http.delete<NetWorthOverview>(`${this.baseUrl}/${year}/entries/${entryId}`);
  }

  /**
   * Takes over the assets and liabilities of a year into another one (VS-15); fails with 409 if the
   * target year has some that may not be overwritten (VS-16).
   */
  copy(year: Year, request: CopyNetWorthRequest): Observable<NetWorthOverview> {
    return this.http.post<NetWorthOverview>(`${this.baseUrl}/${year}/copies`, request);
  }
}
