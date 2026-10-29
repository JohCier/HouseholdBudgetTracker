import { HttpClient, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import {
  AddExpenseRequest,
  AddIncomeRequest,
  AddedBudgetItemResponse,
  ChangeBudgetItemRequest,
  CopiedHouseholdPlanResponse,
  CopyHouseholdPlanRequest,
  CreatedHouseholdPlanResponse,
  DeletedPlans,
  HouseholdPlanOverview,
  MarkImportantRequest,
  ReorderBudgetItemsRequest,
  ReorderSubcategoriesRequest,
  RestoreHouseholdPlanRequest,
  SavingPotentialRequest,
  Year,
  YearlyOverview,
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

  /** All plans of the user, the newest year first, with their changes from the previous year (JU-1). */
  getYearlyOverview(): Observable<YearlyOverview> {
    return this.http.get<YearlyOverview>(`${this.baseUrl}/years`);
  }

  createPlan(year: Year): Observable<CreatedHouseholdPlanResponse> {
    return this.http.post<CreatedHouseholdPlanResponse>(this.baseUrl, { year });
  }

  /**
   * Takes over a plan as template for another year (PU-1 to PU-9); answers with the overview
   * of the plan of the target year. Fails with 409 if that year has a plan that may not be
   * overwritten.
   */
  copyPlan(
    planId: string,
    request: CopyHouseholdPlanRequest,
  ): Observable<CopiedHouseholdPlanResponse> {
    return this.http.post<CopiedHouseholdPlanResponse>(`${this.planUrl(planId)}/copies`, request);
  }

  /** Moves a plan to the deleted plans (GP-7); fails with 404 if it is no longer active. */
  deletePlan(planId: string): Observable<void> {
    return this.http.delete<void>(this.planUrl(planId));
  }

  /** The deleted plans of the user, the newest year and the latest deleted first (GP-22, GP-24). */
  getDeletedPlans(): Observable<DeletedPlans> {
    return this.http.get<DeletedPlans>(`${this.baseUrl}/deleted`);
  }

  /**
   * Makes a deleted plan active again (GP-11). Fails with 409 if its year has an active plan
   * that may not be replaced (GP-14).
   */
  restorePlan(planId: string, request: RestoreHouseholdPlanRequest): Observable<void> {
    return this.http.post<void>(`${this.planUrl(planId)}/restore`, request);
  }

  /** Deletes a deleted plan for good (GP-17); fails with 404 for an active or unknown plan (GP-18). */
  purgePlan(planId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/deleted/${encodeURIComponent(planId)}`);
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

  /** Marks an item as important or no longer (PM-1, PM-4a). */
  markImportant(
    planId: string,
    itemId: string,
    request: MarkImportantRequest,
  ): Observable<HouseholdPlanOverview> {
    return this.http.put<HouseholdPlanOverview>(
      `${this.itemUrl(planId, itemId)}/important`,
      request,
    );
  }

  /** Sets or changes the monthly saving potential of an expense (PM-2a, PM-4b). */
  changeSavingPotential(
    planId: string,
    itemId: string,
    request: SavingPotentialRequest,
  ): Observable<HouseholdPlanOverview> {
    return this.http.put<HouseholdPlanOverview>(
      `${this.itemUrl(planId, itemId)}/saving-potential`,
      request,
    );
  }

  /** Removes the saving potential of an expense (PM-4b). */
  removeSavingPotential(planId: string, itemId: string): Observable<HouseholdPlanOverview> {
    return this.http.delete<HouseholdPlanOverview>(
      `${this.itemUrl(planId, itemId)}/saving-potential`,
    );
  }

  /** Puts the items of one subcategory group in a new order (UK-14). */
  reorderItems(
    planId: string,
    request: ReorderBudgetItemsRequest,
  ): Observable<HouseholdPlanOverview> {
    return this.http.put<HouseholdPlanOverview>(`${this.planUrl(planId)}/item-order`, request);
  }

  /** Puts the subcategory groups of the incomes or of one category in a new order (UK-14). */
  reorderSubcategories(
    planId: string,
    request: ReorderSubcategoriesRequest,
  ): Observable<HouseholdPlanOverview> {
    return this.http.put<HouseholdPlanOverview>(
      `${this.planUrl(planId)}/subcategory-order`,
      request,
    );
  }

  private planUrl(planId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(planId)}`;
  }

  private itemUrl(planId: string, itemId: string): string {
    return `${this.planUrl(planId)}/items/${encodeURIComponent(itemId)}`;
  }
}
