import { NgTemplateOutlet } from '@angular/common';
import { Component, Injector, effect, inject, input, signal, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { Router, RouterLink } from '@angular/router';
import {
  BreakdownView,
  BudgetItem,
  NewItemPreset,
  OneTimeItem,
  PieSelection,
  RULE_SELECTION,
  Year,
} from './budget.model';
import { BudgetApi } from './data-access/budget-api';
import { BudgetStore } from './data-access/budget-store';
import { OneTimeItemStore } from './data-access/one-time-item-store';
import { AddItemDialog } from './ui/add-item-dialog';
import { AddItemForm } from './ui/add-item-form';
import { BudgetEntries, GroupOrder, ImportantMark } from './ui/budget-entries';
import {
  CopiedPlan,
  CopyPlanDialogData,
  copiedMessage,
  openCopyPlanDialog,
} from './ui/copy-plan-dialog';
import { CategoryBreakdownCard } from './ui/category-breakdown';
import { DELETED_PLAN_STATE, openDeletePlanDialog } from './ui/delete-plan-dialog';
import { openDeleteOneTimeItemDialog } from './ui/delete-one-time-item-dialog';
import { EditItemDialog } from './ui/edit-item-dialog';
import { openOneTimeItemDialog } from './ui/one-time-item-dialog';
import { OneTimeItems } from './ui/one-time-items';
import { SavingPotentialDialog } from './ui/saving-potential-dialog';
import { OverviewSummary } from './ui/overview-summary';
import { parseYear } from './year';

@Component({
  selector: 'app-budget-dashboard',
  imports: [
    MatButtonModule,
    MatCardModule,
    NgTemplateOutlet,
    MatIconModule,
    MatProgressBarModule,
    AddItemForm,
    BudgetEntries,
    CategoryBreakdownCard,
    OverviewSummary,
    OneTimeItems,
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

    @if (store.overview(); as overview) {
      <app-overview-summary [overview]="overview" />
    }

    @if (store.planId()) {
      <div class="grid">
        <div class="side">
          <app-add-item-form />
          @if (store.overview(); as overview) {
            <app-category-breakdown
              [breakdowns]="overview.categoryBreakdowns"
              [expenseCategories]="overview.expenseCategories"
              [(view)]="breakdownView"
              [(pieSelection)]="pieSelection"
            />
          }
        </div>
        <div class="main">
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
          <ng-container *ngTemplateOutlet="oneTimeSection" />
        </div>
      </div>
    } @else if (store.planMissing() && !store.loadError()) {
      <mat-card appearance="outlined" class="no-plan">
        <mat-card-content>
          <p>Für {{ store.year() }} gibt es noch keinen Haushaltsplan.</p>
          <div class="actions">
            <button matButton="filled" type="button" [disabled]="creating()" (click)="createPlan()">
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
      <ng-container *ngTemplateOutlet="oneTimeSection" />
    }

    <!-- Shown with and without a plan; one-time items do not belong to it (EP-5, EP-10) -->
    <ng-template #oneTimeSection>
      <app-one-time-items
        [overview]="oneTimeItems.overview()"
        [loadError]="oneTimeItems.loadError()"
        (add)="addOneTimeItem()"
        (edit)="editOneTimeItem($event)"
        (remove)="removeOneTimeItem($event)"
        (retry)="oneTimeItems.load()"
      />
    </ng-template>
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
    .grid {
      display: grid;
      grid-template-columns: minmax(280px, 380px) 1fr;
      gap: 24px;
      align-items: start;
    }
    .no-plan mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
      padding-top: 16px;
    }
    /* Narrower than their content, so a wide card cannot push the page past the screen (MA-1) */
    .grid > * {
      min-width: 0;
    }
    .side,
    .main {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    @media (max-width: 860px) {
      .grid {
        grid-template-columns: minmax(0, 1fr);
      }
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
  /** View of the 50/30/20 card, kept while the page is open (TD-1). */
  protected readonly breakdownView = signal<BreakdownView>('bars');
  /** Part of the plan the pie shows, kept like the view (TK-5). */
  protected readonly pieSelection = signal<PieSelection>(RULE_SELECTION);
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

  /** Deletes a one-time item after asking; there is no undo (EP-17). */
  protected removeOneTimeItem(item: OneTimeItem): void {
    openDeleteOneTimeItemDialog(this.dialog, this.injector, item).subscribe((deleted) => {
      if (deleted) {
        this.snackBar.open(`„${item.description}" gelöscht`, undefined, { duration: 3000 });
      }
    });
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

  /** Adds an item right in one of the groups, its type, category and subcategory filled in (EG-9 to EG-11). */
  protected addTo(preset: NewItemPreset): void {
    this.dialog
      .open<AddItemDialog, NewItemPreset, boolean>(AddItemDialog, {
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
