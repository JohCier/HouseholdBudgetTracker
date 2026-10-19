import { HttpClient, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import {
  AddExpenseRequest,
  AddIncomeRequest,
  AddedBudgetItemResponse,
  ChangeBudgetItemRequest,
  CreatedHouseholdPlanResponse,
  HouseholdPlanOverview,
  Year,
} from '../budget.model';

/**
 * Thin HTTP adapter for the backend's /api/plans endpoints. Every change answers with the
 * overview of the plan after it (AU-1).
 */
@Injectable({ providedIn: 'root' })
export class BudgetApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/plans';

  /** The overview of the plan of a year including its items, or `null` if there is no plan for it. */
  getOverview(year: Year): Observable<HouseholdPlanOverview | null> {
    return this.http
      .get<HouseholdPlanOverview>(this.baseUrl, { params: { year } })
      .pipe(
        catchError((error: unknown) =>
          error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound
            ? of(null)
            : throwError(() => error),
        ),
      );
  }

  createPlan(year: Year): Observable<CreatedHouseholdPlanResponse> {
    return this.http.post<CreatedHouseholdPlanResponse>(this.baseUrl, { year });
  }

  addIncome(planId: string, request: AddIncomeRequest): Observable<AddedBudgetItemResponse> {
    return this.http.post<AddedBudgetItemResponse>(`${this.planUrl(planId)}/incomes`, request);
  }

  addExpense(planId: string, request: AddExpenseRequest): Observable<AddedBudgetItemResponse> {
    return this.http.post<AddedBudgetItemResponse>(`${this.planUrl(planId)}/expenses`, request);
  }

  changeItem(
    planId: string,
    itemId: string,
    request: ChangeBudgetItemRequest,
  ): Observable<HouseholdPlanOverview> {
    return this.http.put<HouseholdPlanOverview>(
      `${this.planUrl(planId)}/items/${encodeURIComponent(itemId)}`,
      request,
    );
  }

  removeItem(planId: string, itemId: string): Observable<HouseholdPlanOverview> {
    return this.http.delete<HouseholdPlanOverview>(
      `${this.planUrl(planId)}/items/${encodeURIComponent(itemId)}`,
    );
  }

  private planUrl(planId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(planId)}`;
  }
}
