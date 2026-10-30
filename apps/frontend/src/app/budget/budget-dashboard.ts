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
import { parseYear } from './year';

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
  template: `
    @if (store.loading()) {
      <mat-progress-bar mode="indeterminate" class="loading" />
    }

    <a matButton routerLink="/" class="back">
      <mat-icon svgIcon="arrow_back" />
      Alle Jahre
    </a>

    <div class="toolbar">
      <nav class="year" aria-label="Jahr wählen">
        <button
          matIconButton
          type="button"
          aria-label="Vorheriges Jahr"
          [disabled]="!store.hasPreviousYear()"
          (click)="showYear(store.year() - 1)"
        >
          <mat-icon svgIcon="chevron_left" />
        </button>
        <h1>{{ store.year() }}</h1>
        <button
          matIconButton
          type="button"
          aria-label="Nächstes Jahr"
          [disabled]="!store.hasNextYear()"
          (click)="showYear(store.year() + 1)"
        >
          <mat-icon svgIcon="chevron_right" />
        </button>
      </nav>
      @if (store.planId()) {
        <div class="actions">
          <button matButton type="button" (click)="copyPlan()">
            <mat-icon svgIcon="content_copy" />
            Als Vorlage übernehmen
          </button>
          <button matButton type="button" class="delete-plan" (click)="deletePlan()">
            <mat-icon svgIcon="delete" />
            Plan löschen
          </button>
        </div>
      }
    </div>

    @if (store.loadError(); as error) {
      <div class="load-error" role="alert">
        <span>Daten konnten nicht geladen werden: {{ error }}</span>
        <button matButton type="button" (click)="store.load()">Erneut versuchen</button>
      </div>
    }

    <!-- The overview at a glance; everything else is in the tabs below (AP-1, AP-5) -->
    @if (store.planId()) {
      @if (store.overview(); as overview) {
        <div class="row overview">
          <app-overview-summary [overview]="overview" />
          <app-category-breakdown [breakdowns]="overview.categoryBreakdowns" />
        </div>
      }
    }

    @if (store.planId() || (store.planMissing() && !store.loadError())) {
      <mat-tab-group
        mat-stretch-tabs="false"
        animationDuration="0ms"
        [selectedIndex]="tabIndex()"
        (selectedIndexChange)="selectTab($event)"
      >
        <mat-tab label="Regelmäßige Posten">
          <div class="tab-body">
            @if (store.planId()) {
              <button matButton="filled" type="button" class="add-item" (click)="addItem()">
                <mat-icon svgIcon="add" />
                Posten erfassen
              </button>
              @if (store.overview(); as overview) {
                <app-budget-entries
                  [overview]="overview"
                  (edit)="edit($event)"
                  (remove)="remove($event)"
                  (reorderItems)="reorderItems($event)"
                  (reorderGroups)="reorderGroups($event)"
                  (add)="addTo($event)"
                  (markImportant)="markImportant($event)"
                  (savingPotential)="editSavingPotential($event)"
                />
              }
            } @else {
              <!-- A year without a plan has only this hint and the one-time items (AP-13) -->
              <mat-card appearance="outlined" class="no-plan">
                <mat-card-content>
                  <p>Für {{ store.year() }} gibt es noch keinen Haushaltsplan.</p>
                  <div class="actions">
                    <button
                      matButton="filled"
                      type="button"
                      [disabled]="creating()"
                      (click)="createPlan()"
                    >
                      Haushaltsplan anlegen
                    </button>
                    @if (otherPlanYears().length > 0) {
                      <button matButton="outlined" type="button" (click)="copyPlanHere()">
                        <mat-icon svgIcon="content_copy" />
                        Aus anderem Jahr übernehmen
                      </button>
                    }
                  </div>
                </mat-card-content>
              </mat-card>
            }
          </div>
        </mat-tab>
        <!-- With and without a plan; one-time items do not belong to it (EP-5, AP-8) -->
        <mat-tab label="Einmalposten">
          <div class="tab-body">
            <app-one-time-items
              [overview]="oneTimeItems.overview()"
              [loadError]="oneTimeItems.loadError()"
              (add)="addOneTimeItem()"
              (edit)="editOneTimeItem($event)"
              (move)="moveOneTimeItem($event)"
              (remove)="removeOneTimeItem($event)"
              (retry)="oneTimeItems.load()"
            />
          </div>
        </mat-tab>
        @if (store.planId()) {
          @if (store.overview(); as overview) {
            <mat-tab label="Detailansicht">
              <div class="tab-body">
                <mat-card appearance="outlined">
                  <mat-card-content class="pie">
                    <app-category-pie
                      [breakdowns]="overview.categoryBreakdowns"
                      [expenseCategories]="overview.expenseCategories"
                      [(selection)]="pieSelection"
                      [(sort)]="pieSort"
                    />
                  </mat-card-content>
                </mat-card>
              </div>
            </mat-tab>
            <mat-tab label="Sparhebel">
              <div class="tab-body">
                <div class="row levers">
                  <app-saving-potential-summary [sums]="overview.savingPotentialsBySubcategory" />
                  <app-top-items list="savingPotential" [items]="overview.topSavingPotentials" />
                  <app-top-items list="expenses" [items]="overview.topExpenses" />
                </div>
              </div>
            </mat-tab>
          }
        }
      </mat-tab-group>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    .loading {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 10;
    }
    .back {
      align-self: flex-start;
      margin-bottom: -16px;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .delete-plan {
      --mat-button-text-label-text-color: var(--mat-sys-error);
      --mat-button-text-state-layer-color: var(--mat-sys-error);
    }
    .toolbar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }
    .year {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .year h1 {
      margin: 0;
      min-width: 6ch;
      text-align: center;
      font: var(--mat-sys-headline-small);
    }
    .load-error {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 12px 16px;
      border-radius: 12px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    .row {
      display: grid;
      gap: 24px;
    }
    /* Narrower than their content, so a wide card cannot push the page past the screen (MA-1) */
    .row > * {
      min-width: 0;
    }
    /* Both columns as high as the higher one (AP-1) */
    .overview {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .levers {
      grid-template-columns: repeat(3, minmax(0, 1fr));
      align-items: start;
    }
    .tab-body {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding-top: 16px;
    }
    .add-item {
      align-self: flex-start;
    }
    .pie {
      padding-top: 16px;
    }
    @media (max-width: 1100px) {
      .levers {
        grid-template-columns: minmax(0, 1fr);
      }
    }
    @media (max-width: 860px) {
      .overview {
        grid-template-columns: minmax(0, 1fr);
      }
    }
    .no-plan mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
      padding-top: 16px;
    }
  `,
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
