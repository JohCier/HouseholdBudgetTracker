/**
 * Client-side representation of the REST contract. The types are generated from
 * api/openapi.json (`npm run generate:api`), which the backend's OpenApiContractTest keeps
 * in sync with the server. Monetary values are decimal strings (e.g. "850.00") to avoid
 * floating-point rounding.
 */
import { components } from './data-access/api.generated';

type Schemas = components['schemas'];

export type ItemType = Schemas['ItemType'];
export type ExpenseCategory = Schemas['ExpenseCategory'];
export type TargetDirection = Schemas['TargetDirection'];
export type BudgetItem = Schemas['BudgetItemResponse'];
export type CategoryBreakdown = Schemas['CategoryBreakdownResponse'];
export type BudgetOverview = Schemas['BudgetOverviewResponse'];
export type AddIncomeRequest = Schemas['AddIncomeRequest'];
export type AddExpenseRequest = Schemas['AddExpenseRequest'];
export type BudgetItemIdResponse = Schemas['BudgetItemIdResponse'];

/** A calendar month in ISO format, e.g. "2026-10". */
export type Month = string;

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
