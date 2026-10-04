import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AddExpenseRequest,
  AddIncomeRequest,
  BudgetItem,
  BudgetItemIdResponse,
  BudgetOverview,
  Year,
} from '../budget.model';

/** Thin HTTP adapter for the backend's /api/budget endpoints. */
@Injectable({ providedIn: 'root' })
export class BudgetApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/budget';

  addIncome(request: AddIncomeRequest): Observable<BudgetItemIdResponse> {
    return this.http.post<BudgetItemIdResponse>(`${this.baseUrl}/incomes`, request);
  }

  addExpense(request: AddExpenseRequest): Observable<BudgetItemIdResponse> {
    return this.http.post<BudgetItemIdResponse>(`${this.baseUrl}/expenses`, request);
  }

  removeItem(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/items/${encodeURIComponent(id)}`);
  }

  listItems(year: Year): Observable<BudgetItem[]> {
    return this.http.get<BudgetItem[]>(`${this.baseUrl}/items`, { params: { year } });
  }

  getOverview(year: Year): Observable<BudgetOverview> {
    return this.http.get<BudgetOverview>(`${this.baseUrl}/overview`, { params: { year } });
  }
}
