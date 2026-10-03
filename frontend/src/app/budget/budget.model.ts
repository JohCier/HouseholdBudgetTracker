/**
 * Client-side representation of the REST contract exposed by the backend's
 * BudgetController (see backend/src/main/java/.../adapter/in/web/dto).
 * Monetary values are transferred as JSON numbers (BigDecimal on the server).
 */

export type ItemType = 'INCOME' | 'EXPENSE';

export type ExpenseCategory = 'LEBENSUNTERHALT' | 'FREIZEIT_SPASS' | 'SPAREN_INVESTIEREN';

export interface BudgetItem {
  id: string;
  description: string;
  amount: number;
  type: ItemType;
  category: ExpenseCategory | null;
}

export interface CategoryBreakdown {
  category: ExpenseCategory;
  total: number;
  actualPercentageOfIncome: number;
  targetPercentageOfIncome: number;
  withinTarget: boolean;
}

export interface BudgetOverview {
  totalIncome: number;
  totalExpense: number;
  balance: number;
  expensesExceedIncome: boolean;
  categoryBreakdowns: CategoryBreakdown[];
}

export interface AddIncomeRequest {
  description: string;
  amount: number;
}

export interface AddExpenseRequest {
  description: string;
  amount: number;
  category: ExpenseCategory;
}

export interface BudgetItemIdResponse {
  id: string;
}

export interface CategoryInfo {
  value: ExpenseCategory;
  label: string;
  hint: string;
}

/** Display metadata for each expense category of the 50/30/20 rule. */
export const EXPENSE_CATEGORIES: readonly CategoryInfo[] = [
  { value: 'LEBENSUNTERHALT', label: 'Lebensunterhalt', hint: 'Miete, Lebensmittel, Versicherungen' },
  { value: 'FREIZEIT_SPASS', label: 'Freizeit & Spaß', hint: 'Hobbys, Ausgehen, Urlaub' },
  { value: 'SPAREN_INVESTIEREN', label: 'Sparen & Investieren', hint: 'Rücklagen, ETFs, Altersvorsorge' },
];

export function categoryLabel(category: ExpenseCategory | null): string {
  return EXPENSE_CATEGORIES.find((c) => c.value === category)?.label ?? '–';
}
