/**
 * Client-side representation of the household plans REST contract. The types come from
 * @hbt/shared-types, generated from packages/shared-types/openapi/plans.json, which the
 * backend's OpenApiContractTest keeps in sync with the server. Monetary values are decimal
 * strings (e.g. "850.00") to avoid floating-point rounding.
 */
import type { PlansSchemas as Schemas } from '@hbt/shared-types';

export type ItemType = Schemas['ItemType'];
export type ExpenseCategory = Schemas['ExpenseCategory'];
export type AmountPeriod = Schemas['AmountPeriod'];
export type TargetDirection = Schemas['TargetDirection'];
export type BudgetItem = Schemas['BudgetItemResponse'];
export type CategoryBreakdown = Schemas['CategoryBreakdownResponse'];
export type ItemGroup = Schemas['ItemGroupResponse'];
export type CategoryItems = Schemas['CategoryItemsResponse'];
export type HouseholdPlanOverview = Schemas['HouseholdPlanOverviewResponse'];
export type AddIncomeRequest = Schemas['AddIncomeRequest'];
export type AddExpenseRequest = Schemas['AddExpenseRequest'];
export type ChangeBudgetItemRequest = Schemas['ChangeBudgetItemRequest'];
export type MarkImportantRequest = Schemas['MarkImportantRequest'];
export type SavingPotentialRequest = Schemas['SavingPotentialRequest'];
export type ReorderBudgetItemsRequest = Schemas['ReorderBudgetItemsRequest'];
export type ReorderSubcategoriesRequest = Schemas['ReorderSubcategoriesRequest'];
export type AddedBudgetItemResponse = Schemas['AddedBudgetItemResponse'];
export type CreateHouseholdPlanRequest = Schemas['CreateHouseholdPlanRequest'];
export type CreatedHouseholdPlanResponse = Schemas['CreatedHouseholdPlanResponse'];
export type YearlyOverview = Schemas['YearlyOverviewResponse'];
export type YearlyOverviewEntry = Schemas['YearlyOverviewEntryResponse'];
export type CopyHouseholdPlanRequest = Schemas['CopyHouseholdPlanRequest'];
export type CopiedHouseholdPlanResponse = Schemas['CopiedHouseholdPlanResponse'];

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

/** Whether an amount is entered per month or per year (MJ-1), in the order the toggle offers them (MJ-11). */
export const AMOUNT_PERIODS: readonly { value: AmountPeriod; label: string }[] = [
  { value: 'MONTHLY', label: 'Monatlich' },
  { value: 'YEARLY', label: 'Jährlich' },
];

/** How the 50/30/20 card shows the breakdown: as one bar per category or as a pie chart. */
export type BreakdownView = 'bars' | 'pie';

/**
 * Which part of the plan the pie shows (TK-1): the 50/30/20 rule, the subcategory groups of one
 * category, or the items of one group. A group always belongs to its category (TK-3).
 */
export type PieSelection =
  | { level: 'rule' }
  | { level: 'category'; category: ExpenseCategory }
  | { level: 'group'; category: ExpenseCategory; subcategory: string };

export const RULE_SELECTION: PieSelection = { level: 'rule' };

/** Whether two subcategory names denote the same group, ignoring case (UK-5). */
export function isSameSubcategory(a: string, b: string): boolean {
  return a.toLocaleLowerCase('de') === b.toLocaleLowerCase('de');
}

/** Target wording shared by the bar and pie views, e.g. "max. 50" or "mind. 20". */
export function targetLabel(breakdown: CategoryBreakdown): string {
  const direction = breakdown.targetDirection === 'AT_LEAST' ? 'mind.' : 'max.';
  return `${direction} ${breakdown.targetPercentageOfIncome}`;
}

/** Hint for a missed target, depending on whether the target is a ceiling or a floor. */
export function missedTargetLabel(breakdown: CategoryBreakdown): string {
  return breakdown.targetDirection === 'AT_LEAST' ? 'Ziel nicht erreicht' : 'Ziel überschritten';
}

/**
 * Where an item is listed among the entries: the incomes, or the expenses of one category
 * (UK-9, UK-10). Its subcategory groups can be put in another order (UK-14).
 */
export interface ItemSection {
  type: ItemType;
  category: ExpenseCategory | null;
}

/**
 * What to fill in when the user adds an item right in one of the groups shown (EG-10): its
 * type, category and subcategory; the subcategory is empty for "Sonstiges" (UK-1).
 */
export interface NewItemPreset {
  /** Name of the group, as shown */
  group: string;
  type: ItemType;
  category: ExpenseCategory | null;
  subcategory: string;
}

/** The group of the items without a subcategory, also offered as a subcategory of its own (UK-1, UK-8). */
export const OTHER_SUBCATEGORY = 'Sonstiges';

/** Predefined subcategories offered for expenses (UK-8). */
export const EXPENSE_SUBCATEGORIES: readonly string[] = [
  'Abonnements',
  'Altersvorsorge',
  'Auto',
  'Bildung',
  'Energie',
  'Entertainment',
  'Essen und Trinken',
  'Geschenke und Spenden',
  'Gesundheit',
  'Haus',
  'Haustiere',
  'Hobbys',
  'Internet und Handy',
  'Investieren',
  'Kinder',
  'Kleidung',
  'Kredite',
  'Miete',
  'Mobilität',
  'Rücklagen',
  OTHER_SUBCATEGORY,
  'Urlaub',
  'Vereine',
  'Versicherungen',
];

/** Predefined subcategories offered for incomes (UK-8). */
export const INCOME_SUBCATEGORIES: readonly string[] = [
  'Gehalt',
  'Kapitalerträge',
  'Kindergeld',
  'Mieteinnahmen',
  'Nebenjob',
  OTHER_SUBCATEGORY,
];

/**
 * The subcategories to suggest for an item of the given type: the predefined ones and those the
 * plan's items of that type already have, alphabetically and each once, ignoring case; a used
 * spelling wins over the predefined one, as new items take it (UK-5, UK-7).
 */
export function subcategorySuggestions(type: ItemType, items: readonly BudgetItem[]): string[] {
  const predefined = type === 'INCOME' ? INCOME_SUBCATEGORIES : EXPENSE_SUBCATEGORIES;
  const used = items
    .filter((item) => item.type === type && item.subcategory)
    .map((item) => item.subcategory!);
  const byKey = new Map<string, string>();
  for (const subcategory of [...predefined, ...used]) {
    byKey.set(subcategory.toLocaleLowerCase('de'), subcategory);
  }
  return [...byKey.values()].sort((a, b) => a.localeCompare(b, 'de', { sensitivity: 'base' }));
}
