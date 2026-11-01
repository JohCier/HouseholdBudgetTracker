import { CurrencyPipe, NgTemplateOutlet } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { AMOUNT_PERIODS, Year, YearlyOverviewEntry } from './budget.model';
import { BudgetApi } from './data-access/budget-api';
import { YearlyOverviewStore, hasPlan } from './data-access/yearly-overview-store';
import { copiedMessage, openCopyPlanDialog } from './ui/copy-plan-dialog';
import { DELETED_PLAN_STATE, deletedMessage, openDeletePlanDialog } from './ui/delete-plan-dialog';
import { DeletedPlanRef, restorePlan, restoredMessage } from './ui/restore-plan-dialog';
import { NetWorthCard } from '../net-worth/ui/net-worth-card';
import { YearChange } from './ui/year-change';

/**
 * The start page after signing in: all plans of the user with their income, expenses and
 * balance and how these changed from the previous year (JU-1 to JU-13). A row opens the plan
 * of its year. The amounts are shown per month or per year, as the user chooses (JM-1 to JM-7).
 * Next to them stand the yearly totals of the one-time items, also for years with only those
 * (EP-12 to EP-15); on a phone they move to a row of their own below (EP-18).
 * Below the plans, the card "Vermögen" shows the net worth of all years (VS-18).
 */
@Component({
  selector: 'app-yearly-overview-page',
  imports: [
    CurrencyPipe,
    MatButtonModule,
    MatButtonToggleModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    NetWorthCard,
    NgTemplateOutlet,
    RouterLink,
    YearChange,
  ],
  providers: [YearlyOverviewStore],
  templateUrl: './yearly-overview-page.html',
  styleUrl: './yearly-overview-page.scss',
})
export class YearlyOverviewPage implements OnInit {
  protected readonly store = inject(YearlyOverviewStore);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly api = inject(BudgetApi);

  /** A plan deleted in the plan view just before it showed this page, to offer undo for (GP-9). */
  private readonly deletedInPlanView = this.router.currentNavigation()?.extras.state?.[
    DELETED_PLAN_STATE
  ] as DeletedPlanRef | undefined;

  protected readonly creating = signal(false);
  protected readonly periods = AMOUNT_PERIODS;
  protected readonly hasPlan = hasPlan;
  /** The columns of a plan, shown as "–" in a year without plan (EP-12). */
  protected readonly planColumns = ['income', 'expense', 'balance'];

  /**
   * The amounts of the plan in the chosen period; the backend works out both (JM-2, JM-3, JM-6).
   * `null` only in a year without plan, which shows none (EP-12).
   */
  protected income(entry: YearlyOverviewEntry): string | null {
    return this.monthly() ? entry.monthlyTotalIncome : entry.totalIncome;
  }

  protected expense(entry: YearlyOverviewEntry): string | null {
    return this.monthly() ? entry.monthlyTotalExpense : entry.totalExpense;
  }

  protected balance(entry: YearlyOverviewEntry): string | null {
    return this.monthly() ? entry.monthlyBalance : entry.balance;
  }

  private monthly(): boolean {
    return this.store.period() === 'MONTHLY';
  }

  ngOnInit(): void {
    void this.store.load();
    if (this.deletedInPlanView) {
      this.offerUndo(this.deletedInPlanView);
    }
  }

  /** Opens the plan of the year (JU-11). */
  protected open(year: Year): Promise<boolean> {
    return this.router.navigate(['/jahre', year]);
  }

  /** Takes over the plan of the year as template for another year and opens that one (PU-12, PU-15). */
  protected copyPlan(sourceYear: Year): void {
    openCopyPlanDialog(this.dialog, { sourceYear }).subscribe(async (copied) => {
      if (copied) {
        await this.open(copied.targetYear);
        this.snackBar.open(copiedMessage(copied), undefined, { duration: 3000 });
      }
    });
  }

  /** Deletes the plan of the year after asking; the overview stays open without it (PL-7 to PL-9). */
  protected deletePlan(year: Year): void {
    openDeletePlanDialog(this.dialog, year).subscribe(async (deleted) => {
      if (deleted) {
        await this.store.load();
        this.offerUndo(deleted);
      }
    });
  }

  /** Confirms the deletion and restores the plan if the user chooses undo (GP-9). */
  private offerUndo(deleted: DeletedPlanRef): void {
    const undo = this.snackBar.open(deletedMessage(deleted.year), 'Rückgängig', {
      duration: 6000,
    });
    undo.onAction().subscribe(async () => {
      try {
        if (await restorePlan(this.api, this.dialog, deleted)) {
          await this.store.load();
          this.snackBar.open(restoredMessage(deleted.year), undefined, { duration: 3000 });
        }
      } catch (error) {
        this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
      }
    });
  }

  /** Creates the plan of the current year and opens it (JU-12). */
  protected async createPlan(): Promise<void> {
    this.creating.set(true);
    try {
      await this.open(await this.store.createCurrentYearPlan());
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    } finally {
      this.creating.set(false);
    }
  }
}
