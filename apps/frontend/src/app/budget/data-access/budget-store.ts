import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, map } from 'rxjs';
import { toMessage } from '../../shared/http-error';
import {
  AddExpenseRequest,
  AddIncomeRequest,
  BudgetItem,
  ChangeBudgetItemRequest,
  HouseholdPlanOverview,
  ItemGroup,
  ItemSection,
  ItemType,
  Year,
  subcategorySuggestions,
} from '../budget.model';
import { FIRST_YEAR, LAST_YEAR, currentYear } from '../../shared/year';
import { BudgetApi } from './budget-api';

/**
 * Signal-based state holder for the household plan of the selected year. A year may not
 * have a plan yet; items can only be added once one has been created. The overview,
 * including the items, is derived on the server; every mutation answers with the overview
 * after it, which is shown without reloading (AU-5). Only the latest response is applied,
 * so overlapping loads and mutations (e.g. quick successive deletes or year changes) cannot
 * leave stale data behind (AU-6). Selecting another year forgets the overview of the
 * previous one right away, so nothing of the previous year is shown or changed while the
 * new one loads or if loading it fails (HP-1).
 * Mutations reject with an Error carrying a user-facing message.
 *
 * Provided by the dashboard, so the state lives only as long as the dashboard is shown and
 * never outlasts the session of the user it belongs to.
 */
@Injectable()
export class BudgetStore {
  private readonly api = inject(BudgetApi);

  private readonly _year = signal<Year>(currentYear());
  /** `undefined` until the overview of the selected year has been loaded, `null` without plan. */
  private readonly _overview = signal<HouseholdPlanOverview | null | undefined>(undefined);
  private readonly _loading = signal(false);
  private readonly _loadError = signal<string | null>(null);
  private latestLoad = 0;

  readonly year = this._year.asReadonly();
  /** Whether there is a year before resp. after the selected one that can have a plan (HP-1). */
  readonly hasPreviousYear = computed(() => this._year() > FIRST_YEAR);
  readonly hasNextYear = computed(() => this._year() < LAST_YEAR);
  readonly overview = computed(() => this._overview() ?? null);
  readonly planId = computed(() => this._overview()?.id ?? null);
  /** True once it is known that the selected year has no plan yet. */
  readonly planMissing = computed(() => this._overview() === null);
  readonly items = computed<BudgetItem[]>(() => this._overview()?.items ?? []);
  /** Subcategories to suggest for new or changed incomes resp. expenses (UK-7). */
  readonly incomeSubcategories = computed(() => subcategorySuggestions('INCOME', this.items()));
  readonly expenseSubcategories = computed(() => subcategorySuggestions('EXPENSE', this.items()));
  readonly loading = this._loading.asReadonly();
  readonly loadError = this._loadError.asReadonly();

  selectYear(year: Year): Promise<void> {
    this._year.set(year);
    this._overview.set(undefined);
    return this.load();
  }

  async load(): Promise<void> {
    const loadId = ++this.latestLoad;
    const year = this._year();
    this._loading.set(true);
    try {
      const overview = await firstValueFrom(this.api.getOverview(year));
      if (loadId === this.latestLoad) {
        this._overview.set(overview);
        this._loadError.set(null);
      }
    } catch (error) {
      if (loadId === this.latestLoad) {
        this._loadError.set(toMessage(error));
      }
    } finally {
      if (loadId === this.latestLoad) {
        this._loading.set(false);
      }
    }
  }

  /** Creates the household plan of the selected year. */
  createPlan(): Promise<void> {
    return this.mutate(this.api.createPlan(this._year()).pipe(map((created) => created.overview)));
  }

  async addIncome(request: AddIncomeRequest): Promise<void> {
    return this.mutate(
      this.api.addIncome(this.requirePlanId(), request).pipe(map((added) => added.overview)),
    );
  }

  async addExpense(request: AddExpenseRequest): Promise<void> {
    return this.mutate(
      this.api.addExpense(this.requirePlanId(), request).pipe(map((added) => added.overview)),
    );
  }

  subcategorySuggestions(type: ItemType): string[] {
    return type === 'INCOME' ? this.incomeSubcategories() : this.expenseSubcategories();
  }

  /** Gives an item of the selected year's plan new values; it keeps its id and position. */
  async changeItem(itemId: string, request: ChangeBudgetItemRequest): Promise<void> {
    return this.mutate(this.api.changeItem(this.requirePlanId(), itemId, request));
  }

  /** Marks an item as important or no longer (PM-1, PM-4a). */
  markImportant(itemId: string, important: boolean): Promise<void> {
    return this.mutate(this.api.markImportant(this.requirePlanId(), itemId, { important }));
  }

  /**
   * Sets or changes the saving potential of an expense, always per month, or removes it with
   * `null` (PM-2a, PM-4b).
   */
  changeSavingPotential(itemId: string, monthlyAmount: string | null): Promise<void> {
    const planId = this.requirePlanId();
    return this.mutate(
      monthlyAmount === null
        ? this.api.removeSavingPotential(planId, itemId)
        : this.api.changeSavingPotential(planId, itemId, { monthlyAmount }),
    );
  }

  async removeItem(itemId: string): Promise<void> {
    return this.mutate(this.api.removeItem(this.requirePlanId(), itemId));
  }

  /**
   * Puts the items of one subcategory group in a new order (UK-14). The new order shows right
   * away; should saving it fail, the overview is loaded again, so the stored order shows.
   */
  reorderItems(itemIds: string[]): Promise<void> {
    const planId = this.requirePlanId();
    this.showReordered((groups) => groups.map((group) => withItemOrder(group, itemIds)));
    return this.mutateOrReload(this.api.reorderItems(planId, { itemIds }));
  }

  /** Puts the subcategory groups of a section in a new order, see {@link reorderItems} (UK-14). */
  reorderSubcategories(section: ItemSection, subcategories: string[]): Promise<void> {
    const planId = this.requirePlanId();
    this.showReordered((groups, groupSection) =>
      groupSection.type === section.type && groupSection.category === section.category
        ? withGroupOrder(groups, subcategories)
        : groups,
    );
    return this.mutateOrReload(
      this.api.reorderSubcategories(planId, {
        type: section.type,
        ...(section.category ? { category: section.category } : {}),
        subcategories,
      }),
    );
  }

  /**
   * Re-creates a previously removed item (with a new id), its amount as entered with its period,
   * its subcategory, its note and its marks in the plan it was removed from, even if another year
   * has been selected in the meantime (PO-9, PN-8, UK-4, MJ-4, PM-6).
   */
  restoreItem(planId: string, item: BudgetItem): Promise<void> {
    const request = {
      description: item.description,
      amount: item.amount,
      period: item.period,
      ...(item.subcategory ? { subcategory: item.subcategory } : {}),
      ...(item.note ? { note: item.note } : {}),
      ...(item.important ? { important: true } : {}),
    };
    const added =
      item.category === null
        ? this.api.addIncome(planId, request)
        : this.api.addExpense(planId, {
            ...request,
            category: item.category,
            ...(item.savingPotential ? { savingPotential: item.savingPotential } : {}),
          });
    return this.mutate(added.pipe(map((response) => response.overview)));
  }

  private requirePlanId(): string {
    const planId = this.planId();
    if (!planId) {
      throw new Error(`Für ${this._year()} gibt es noch keinen Haushaltsplan.`);
    }
    return planId;
  }

  /** Shows the overview with the groups of each section rearranged; sums stay as they are (UK-13). */
  private showReordered(
    rearrange: (groups: ItemGroup[], section: ItemSection) => ItemGroup[],
  ): void {
    const overview = this._overview();
    if (!overview) {
      return;
    }
    this._overview.set({
      ...overview,
      incomeGroups: rearrange(overview.incomeGroups, { type: 'INCOME', category: null }),
      expenseCategories: overview.expenseCategories.map((category) => ({
        ...category,
        groups: rearrange(category.groups, { type: 'EXPENSE', category: category.category }),
      })),
    });
  }

  private async mutateOrReload(request: Observable<HouseholdPlanOverview>): Promise<void> {
    try {
      await this.mutate(request);
    } catch (error) {
      void this.load();
      throw error;
    }
  }

  private async mutate(request: Observable<HouseholdPlanOverview>): Promise<void> {
    let overview: HouseholdPlanOverview;
    try {
      overview = await firstValueFrom(request);
    } catch (error) {
      throw new Error(toMessage(error));
    }
    this.show(overview);
  }

  /**
   * Shows the overview a mutation answered with, unless another year has been selected in
   * the meantime (AU-5). It is the latest state, so a load still running is discarded (AU-6).
   */
  private show(overview: HouseholdPlanOverview): void {
    if (overview.year !== this._year()) {
      return;
    }
    this.latestLoad++;
    this._overview.set(overview);
    this._loadError.set(null);
    this._loading.set(false);
  }
}

/** The group with its items in the given order, if they are its items; otherwise unchanged. */
function withItemOrder(group: ItemGroup, itemIds: string[]): ItemGroup {
  const items = itemIds.map((id) => group.items.find((item) => item.id === id));
  return items.length === group.items.length && items.every((item) => item !== undefined)
    ? { ...group, items: items as BudgetItem[] }
    : group;
}

/** The groups in the given order of their names, if they are exactly these; otherwise unchanged. */
function withGroupOrder(groups: ItemGroup[], subcategories: string[]): ItemGroup[] {
  const ordered = subcategories.map((name) => groups.find((group) => group.subcategory === name));
  return ordered.length === groups.length && ordered.every((group) => group !== undefined)
    ? (ordered as ItemGroup[])
    : groups;
}
