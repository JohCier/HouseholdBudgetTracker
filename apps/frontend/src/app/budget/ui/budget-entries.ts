import {
  CdkDrag,
  CdkDragDrop,
  CdkDragHandle,
  CdkDropList,
  moveItemInArray,
} from '@angular/cdk/drag-drop';
import { CurrencyPipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  BudgetItem,
  HouseholdPlanOverview,
  ItemGroup,
  ItemSection,
  NewItemPreset,
  OTHER_SUBCATEGORY,
  categoryLabel,
} from '../budget.model';

/** A new order of the subcategory groups of one section (UK-14). */
export interface GroupOrder {
  section: ItemSection;
  subcategories: string[];
}

interface SectionView {
  section: ItemSection;
  /** Name of the incomes or of the category, also the class giving it its color (UK-19). */
  label: string;
  colorClass: string;
  monthlyTotal: string;
  total: string;
  groups: ItemGroup[];
}

/**
 * The entries of a plan: first the incomes, then the expenses by category in the order of the
 * 50/30/20 rule, each grouped by subcategory (UK-9 to UK-11). Each group, category and area ends
 * with a row of its sum (EG-5, EG-6); every item and every sum is shown per month and per year
 * (MJ-12). All amounts and sums come from the server (UK-13,
 * MJ-10). Groups and the items within a group can be dragged to another position by
 * their handle, or moved with the arrow keys while the handle has the focus (UK-14); the new
 * order is emitted, saving it is up to the parent. The row of a group's sum offers to add an
 * item to the group; what to fill in is emitted (EG-9, EG-10).
 */
@Component({
  selector: 'app-budget-entries',
  imports: [
    CdkDrag,
    CdkDragHandle,
    CdkDropList,
    CurrencyPipe,
    NgTemplateOutlet,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './budget-entries.html',
  styleUrl: './budget-entries.scss',
})
export class BudgetEntries {
  readonly overview = input.required<HouseholdPlanOverview>();
  readonly edit = output<BudgetItem>();
  readonly remove = output<BudgetItem>();
  readonly reorderItems = output<string[]>();
  readonly reorderGroups = output<GroupOrder>();
  readonly add = output<NewItemPreset>();

  protected readonly empty = computed(() => this.overview().items.length === 0);

  protected readonly incomes = computed<SectionView | null>(() => {
    const overview = this.overview();
    return overview.incomeGroups.length === 0
      ? null
      : {
          section: { type: 'INCOME', category: null },
          label: 'Einnahmen',
          colorClass: 'INCOME',
          monthlyTotal: overview.monthlyTotalIncome,
          total: overview.totalIncome,
          groups: overview.incomeGroups,
        };
  });

  protected readonly expenseCategories = computed<SectionView[]>(() =>
    this.overview().expenseCategories.map((category) => ({
      section: { type: 'EXPENSE', category: category.category },
      label: categoryLabel(category.category),
      colorClass: category.category,
      monthlyTotal: category.monthlyTotal,
      total: category.total,
      groups: category.groups,
    })),
  );

  protected readonly trackItem = (_index: number, item: BudgetItem): string => item.id;

  protected dropGroup(section: SectionView, event: CdkDragDrop<unknown>): void {
    this.moveGroup(section, event.previousIndex, event.currentIndex);
  }

  protected dropItem(group: ItemGroup, event: CdkDragDrop<unknown>): void {
    this.moveItem(group, event.previousIndex, event.currentIndex);
  }

  /** Asks to add an item to the group, its type, category and subcategory filled in (EG-10). */
  protected addTo(section: SectionView, group: ItemGroup): void {
    this.add.emit({
      group: group.subcategory,
      type: section.section.type,
      category: section.section.category,
      subcategory: group.subcategory === OTHER_SUBCATEGORY ? '' : group.subcategory,
    });
  }

  /** Moves a group one place up or down with the arrow keys on its handle. */
  protected moveGroupBy(section: SectionView, index: number, by: number, event: Event): void {
    event.preventDefault();
    this.moveGroup(section, index, index + by);
  }

  /** Moves an item one place up or down within its group with the arrow keys on its handle. */
  protected moveItemBy(group: ItemGroup, index: number, by: number, event: Event): void {
    event.preventDefault();
    this.moveItem(group, index, index + by);
  }

  private moveGroup(section: SectionView, from: number, to: number): void {
    const subcategories = section.groups.map((group) => group.subcategory);
    if (moved(subcategories, from, to)) {
      this.reorderGroups.emit({ section: section.section, subcategories });
    }
  }

  private moveItem(group: ItemGroup, from: number, to: number): void {
    const itemIds = group.items.map((item) => item.id);
    if (moved(itemIds, from, to)) {
      this.reorderItems.emit(itemIds);
    }
  }
}

/** Moves an entry of the list in place; false if it stays where it is. */
function moved(list: unknown[], from: number, to: number): boolean {
  if (from === to || to < 0 || to >= list.length) {
    return false;
  }
  moveItemInArray(list, from, to);
  return true;
}
