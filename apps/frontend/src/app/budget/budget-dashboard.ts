import {
  Component,
  Injector,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { Router, RouterLink } from '@angular/router';
import {
  BudgetItem,
  NewItemPreset,
  AMOUNT_SORT,
  OneTimeItem,
  PieSelection,
  PieSort,
  RULE_SELECTION,
  Year,
} from './budget.model';
import { BudgetApi } from './data-access/budget-api';
import { BudgetStore } from './data-access/budget-store';
import { OneTimeItemStore } from './data-access/one-time-item-store';
import { AddItemDialog } from './ui/add-item-dialog';
import { BudgetEntries, GroupOrder, ImportantMark } from './ui/budget-entries';
import {
  CopiedPlan,
  CopyPlanDialogData,
  copiedMessage,
  openCopyPlanDialog,
} from './ui/copy-plan-dialog';
import { CategoryBreakdownCard } from './ui/category-breakdown';
import { CategoryPieChart } from './ui/category-pie';
import { TopItems } from './ui/top-items';
import { DELETED_PLAN_STATE, openDeletePlanDialog } from './ui/delete-plan-dialog';
import { EditItemDialog } from './ui/edit-item-dialog';
import { openOneTimeItemDialog } from './ui/one-time-item-dialog';
import { OneTimeItemMove, OneTimeItems } from './ui/one-time-items';
import { SavingPotentialDialog } from './ui/saving-potential-dialog';
import { SavingPotentialSummary } from './ui/saving-potential-summary';
import { OverviewSummary } from './ui/overview-summary';
import { parseYear } from '../shared/year';

@Component({
  selector: 'app-budget-dashboard',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatTabsModule,
    BudgetEntries,
    CategoryBreakdownCard,
    TopItems,
    OverviewSummary,
    OneTimeItems,
    SavingPotentialSummary,
    CategoryPieChart,
    RouterLink,
  ],
  providers: [BudgetStore, OneTimeItemStore],
  templateUrl: './budget-dashboard.html',
  styleUrl: './budget-dashboard.scss',
})
export class BudgetDashboard {
  /** The year from the address, `/jahre/2026` (JU-11). */
  readonly year = input<string>();

  protected readonly store = inject(BudgetStore);
  protected readonly oneTimeItems = inject(OneTimeItemStore);
  private readonly api = inject(BudgetApi);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly injector = inject(Injector);

  protected readonly creating = signal(false);
  /** The tab chosen, kept while the page is open, also when the year changes (AP-5). */
  protected readonly tab = signal<PlanTab>('items');
  /** The tab shown: a year without a plan has only the first two, so it falls back to the first (AP-13). */
  protected readonly tabIndex = computed(() => {
    const index = PLAN_TABS.indexOf(this.tab());
    return this.store.planId() || index < 2 ? index : 0;
  });
  /** Part of the plan the pie shows, kept while the page is open (TK-5, AP-10). */
  protected readonly pieSelection = signal<PieSelection>(RULE_SELECTION);
  /** Order of the slices in the pie; kept here like the selection (TS-6, AP-10). */
  protected readonly pieSort = signal<PieSort>(AMOUNT_SORT);
  /** The years the user has plans for, other than the one shown; looked up for a year without plan (PU-12). */
  protected readonly otherPlanYears = signal<Year[]>([]);

  constructor() {
    // Shows the plan of the year in the address; an address without a valid year leads back to the
    // yearly overview (HP-1, JU-11).
    effect(() => {
      const year = parseYear(this.year());
      untracked(() => {
        if (year === null) {
          void this.router.navigateByUrl('/');
        } else {
          void this.store.selectYear(year);
          void this.oneTimeItems.selectYear(year);
        }
      });
    });
    effect(() => {
      if (this.store.planMissing()) {
        untracked(() => void this.lookUpOtherPlanYears(this.store.year()));
      } else {
        this.otherPlanYears.set([]);
      }
    });
  }

  /** Changes the address to the plan of another year, which then shows it. */
  protected showYear(year: number): Promise<boolean> {
    return this.router.navigate(['/jahre', year]);
  }

  protected async createPlan(): Promise<void> {
    this.creating.set(true);
    try {
      await this.store.createPlan();
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    } finally {
      this.creating.set(false);
    }
  }

  /** Takes over the plan shown as template for another year, which then shows (PU-12 to PU-15). */
  protected copyPlan(): void {
    this.openCopyDialog({ sourceYear: this.store.year() });
  }

  /** Takes over another plan as template for the year shown, which has none yet (PU-12). */
  protected copyPlanHere(): void {
    this.openCopyDialog({ targetYear: this.store.year(), sourceYears: this.otherPlanYears() });
  }

  /** Deletes the plan shown after asking, then shows the yearly overview (PL-7 to PL-9). */
  protected deletePlan(): void {
    const year = this.store.year();
    openDeletePlanDialog(this.dialog, year).subscribe(async (deleted) => {
      if (deleted) {
        // The overview confirms the deletion and offers to undo it (GP-9)
        await this.router.navigateByUrl('/', { state: { [DELETED_PLAN_STATE]: deleted } });
      }
    });
  }

  private openCopyDialog(data: CopyPlanDialogData): void {
    openCopyPlanDialog(this.dialog, data).subscribe((copied) => {
      if (copied) {
        void this.showCopied(copied);
      }
    });
  }

  private async showCopied(copied: CopiedPlan): Promise<void> {
    if (copied.targetYear === this.store.year()) {
      await this.store.load();
    } else {
      await this.showYear(copied.targetYear);
    }
    this.snackBar.open(copiedMessage(copied), undefined, { duration: 3000 });
  }

  private async lookUpOtherPlanYears(year: Year): Promise<void> {
    try {
      const overview = await firstValueFrom(this.api.getYearlyOverview());
      if (this.store.year() === year && this.store.planMissing()) {
        this.otherPlanYears.set(
          overview.years
            .filter((entry) => entry.planId !== null)
            .map((entry) => entry.year)
            .filter((y) => y !== year),
        );
      }
    } catch {
      // Without the list the year just offers to create a plan.
      this.otherPlanYears.set([]);
    }
  }

  /** Enters a one-time item, starting with a day of the year shown (EP-7, EP-11). */
  protected addOneTimeItem(): void {
    openOneTimeItemDialog(this.dialog, this.injector, { year: this.store.year() }).subscribe(
      (saved) => {
        if (saved) {
          this.snackBar.open('Einmalposten erfasst', undefined, { duration: 3000 });
        }
      },
    );
  }

  /** Changes a one-time item; with a day in another year it leaves the section (EP-7). */
  protected editOneTimeItem(item: OneTimeItem): void {
    openOneTimeItemDialog(this.dialog, this.injector, { year: this.store.year(), item }).subscribe(
      (saved) => {
        if (saved) {
          this.snackBar.open('Einmalposten geändert', undefined, { duration: 3000 });
        }
      },
    );
  }

  /** Saves the new place of a one-time item; on failure the stored order shows again (ES-4, ES-9). */
  protected async moveOneTimeItem(move: OneTimeItemMove): Promise<void> {
    try {
      await this.oneTimeItems.moveItem(move.item, move.from, move.to);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }

  /** Deletes a one-time item right away and offers to undo it, as for items (ES-10, ES-11). */
  protected async removeOneTimeItem(item: OneTimeItem): Promise<void> {
    try {
      await this.oneTimeItems.deleteItem(item.id);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
      return;
    }
    const undo = this.snackBar.open(`„${item.description}" gelöscht`, 'Rückgängig', {
      duration: 6000,
    });
    undo.onAction().subscribe(() => void this.restoreOneTimeItem(item));
  }

  private async restoreOneTimeItem(item: OneTimeItem): Promise<void> {
    try {
      await this.oneTimeItems.restoreItem(item);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }

  protected edit(item: BudgetItem): void {
    this.dialog
      .open<EditItemDialog, BudgetItem, boolean>(EditItemDialog, {
        data: item,
        injector: this.injector,
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.snackBar.open('Eintrag geändert', undefined, { duration: 3000 });
        }
      });
  }

  protected selectTab(index: number): void {
    this.tab.set(PLAN_TABS[index]);
  }

  /** Adds an item from the button above the entries, starting with an expense (AP-14, AP-15). */
  protected addItem(): void {
    this.openAddItemDialog(null);
  }

  /** Adds an item right in one of the groups, its type, category and subcategory filled in (EG-9 to EG-11). */
  protected addTo(preset: NewItemPreset): void {
    this.openAddItemDialog(preset);
  }

  private openAddItemDialog(preset: NewItemPreset | null): void {
    this.dialog
      .open<AddItemDialog, NewItemPreset | null, boolean>(AddItemDialog, {
        data: preset,
        injector: this.injector,
      })
      .afterClosed()
      .subscribe((added) => {
        if (added) {
          this.snackBar.open('Eintrag hinzugefügt', undefined, { duration: 3000 });
        }
      });
  }

  /** Marks an item as important or no longer, right away and without asking (PM-4a). */
  protected async markImportant({ item, important }: ImportantMark): Promise<void> {
    try {
      await this.store.markImportant(item.id, important);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }

  /** Sets, changes or removes the saving potential of an expense in a dialog (PM-4b). */
  protected editSavingPotential(item: BudgetItem): void {
    this.dialog.open<SavingPotentialDialog, BudgetItem, boolean>(SavingPotentialDialog, {
      data: item,
      injector: this.injector,
    });
  }

  /** Saves a new order of the items of a group; on failure the stored order shows again (UK-14). */
  protected async reorderItems(itemIds: string[]): Promise<void> {
    try {
      await this.store.reorderItems(itemIds);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }

  /** Saves a new order of the groups of a section, see {@link reorderItems} (UK-14). */
  protected async reorderGroups(order: GroupOrder): Promise<void> {
    try {
      await this.store.reorderSubcategories(order.section, order.subcategories);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }

  protected async remove(item: BudgetItem): Promise<void> {
    const planId = this.store.planId()!;
    try {
      await this.store.removeItem(item.id);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
      return;
    }
    const undo = this.snackBar.open(`„${item.description}" gelöscht`, 'Rückgängig', {
      duration: 6000,
    });
    undo.onAction().subscribe(() => void this.restore(planId, item));
  }

  private async restore(planId: string, item: BudgetItem): Promise<void> {
    try {
      await this.store.restoreItem(planId, item);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }
}

/** The tabs below the overview, in their order (AP-5). */
const PLAN_TABS = ['items', 'oneTime', 'details', 'levers'] as const;
type PlanTab = (typeof PLAN_TABS)[number];
