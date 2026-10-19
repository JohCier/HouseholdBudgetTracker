/**
 * Client-side representation of the household plans REST contract. The types come from
 * @hbt/shared-types, generated from packages/shared-types/openapi/plans.json, which the
 * backend's OpenApiContractTest keeps in sync with the server. Monetary values are decimal
 * strings (e.g. "850.00") to avoid floating-point rounding.
 */
import type { PlansSchemas as Schemas } from '@hbt/shared-types';

export type ItemType = Schemas['ItemType'];
export type ExpenseCategory = Schemas['ExpenseCategory'];
export type TargetDirection = Schemas['TargetDirection'];
export type BudgetItem = Schemas['BudgetItemResponse'];
export type CategoryBreakdown = Schemas['CategoryBreakdownResponse'];
export type HouseholdPlanOverview = Schemas['HouseholdPlanOverviewResponse'];
export type AddIncomeRequest = Schemas['AddIncomeRequest'];
export type AddExpenseRequest = Schemas['AddExpenseRequest'];
export type ChangeBudgetItemRequest = Schemas['ChangeBudgetItemRequest'];
export type AddedBudgetItemResponse = Schemas['AddedBudgetItemResponse'];
export type CreateHouseholdPlanRequest = Schemas['CreateHouseholdPlanRequest'];
export type CreatedHouseholdPlanResponse = Schemas['CreatedHouseholdPlanResponse'];

/** A calendar year, e.g. 2026. */
export type Year = number;

export interface CategoryInfo {
  value: ExpenseCategory;
  label: string;
  hint: string;
}

/** Display metadata for each expense category of the 50/30/20 rule. */
export const EXPENSE_CATEGORIES: readonly CategoryInfo[] = [
  {
    value: 'LEBENSUNTERHALT',
    label: 'Lebensunterhalt',
    hint: 'Miete, Lebensmittel, Versicherungen',
  },
  { value: 'FREIZEIT_SPASS', label: 'Freizeit & Spaß', hint: 'Hobbys, Ausgehen, Urlaub' },
  {
    value: 'SPAREN_INVESTIEREN',
    label: 'Sparen & Investieren',
    hint: 'Rücklagen, ETFs, Altersvorsorge',
  },
];

export function categoryLabel(category: ExpenseCategory | null): string {
  return EXPENSE_CATEGORIES.find((c) => c.value === category)?.label ?? '–';
}

/** How the 50/30/20 card shows the breakdown: as one bar per category or as a pie chart. */
export type BreakdownView = 'bars' | 'pie';

/** Target wording shared by the bar and pie views, e.g. "max. 50" or "mind. 20". */
export function targetLabel(breakdown: CategoryBreakdown): string {
  const direction = breakdown.targetDirection === 'AT_LEAST' ? 'mind.' : 'max.';
  return `${direction} ${breakdown.targetPercentageOfIncome}`;
}

/** Hint for a missed target, depending on whether the target is a ceiling or a floor. */
export function missedTargetLabel(breakdown: CategoryBreakdown): string {
  return breakdown.targetDirection === 'AT_LEAST' ? 'Ziel nicht erreicht' : 'Ziel überschritten';
}
