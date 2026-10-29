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
  template: `
    @if (store.loading()) {
      <mat-progress-bar mode="indeterminate" class="loading" />
    }

    <div class="heading">
      <h1>Haushaltspläne</h1>
      @if (store.hasPlans()) {
        <mat-button-toggle-group
          aria-label="Beträge monatlich oder jährlich"
          [value]="store.period()"
          (change)="store.setPeriod($event.value)"
        >
          @for (period of periods; track period.value) {
            <mat-button-toggle [value]="period.value">{{ period.label }}</mat-button-toggle>
          }
        </mat-button-toggle-group>
      }
    </div>

    @if (store.loadError(); as error) {
      <div class="load-error" role="alert">
        <span>Daten konnten nicht geladen werden: {{ error }}</span>
        <button matButton type="button" (click)="store.load()">Erneut versuchen</button>
      </div>
    }

    @if (store.currentYearMissing()) {
      <mat-card appearance="outlined" class="create">
        <mat-card-content>
          @if (store.empty()) {
            <p>Noch keine Haushaltspläne.</p>
          } @else {
            <p>Für {{ store.currentYear }} gibt es noch keinen Haushaltsplan.</p>
          }
          <button matButton="filled" type="button" [disabled]="creating()" (click)="createPlan()">
            Haushaltsplan anlegen
          </button>
        </mat-card-content>
      </mat-card>
    }

    @if (store.years().length > 0) {
      <mat-card appearance="outlined" class="years">
        <table>
          <thead>
            <tr class="group-head">
              <td></td>
              <th scope="colgroup" colspan="3" class="group">Haushaltsplan</th>
              <th scope="colgroup" colspan="3" class="group one-time first">Einmalig (Jahr)</th>
              <td></td>
            </tr>
            <tr>
              <th scope="col">Jahr</th>
              <th scope="col" class="amount">Einnahmen</th>
              <th scope="col" class="amount">Ausgaben</th>
              <th scope="col" class="amount">Überschuss</th>
              <th scope="col" class="amount one-time first">
                <span class="visually-hidden">Einmalige </span>Einnahmen
              </th>
              <th scope="col" class="amount one-time">
                <span class="visually-hidden">Einmalige </span>Ausgaben
              </th>
              <th scope="col" class="amount one-time">
                <span class="visually-hidden">Einmaliger </span>Saldo
              </th>
              <th scope="col" class="actions"><span class="visually-hidden">Aktionen</span></th>
            </tr>
          </thead>
          <tbody>
            @for (entry of store.years(); track entry.year) {
              <tr (click)="open(entry.year)" [attr.data-year]="entry.year" class="plan-row">
                <th scope="row">
                  <a [routerLink]="['/jahre', entry.year]" (click)="$event.stopPropagation()">{{
                    entry.year
                  }}</a>
                </th>
                @if (hasPlan(entry)) {
                  <td class="amount">
                    <span class="value">{{ income(entry) | currency: 'EUR' }}</span>
                    <app-year-change kind="income" [change]="entry.incomeChange" />
                  </td>
                  <td class="amount">
                    <span class="value">{{ expense(entry) | currency: 'EUR' }}</span>
                    <app-year-change kind="expense" [change]="entry.expenseChange" />
                  </td>
                  <td class="amount">
                    <span class="value" [class.deficit]="entry.expensesExceedIncome">
                      {{ balance(entry) | currency: 'EUR' }}
                    </span>
                    @if (entry.expensesExceedIncome) {
                      <span class="deficit-label">Defizit</span>
                    }
                    <app-year-change kind="balance" [change]="entry.balanceChange" />
                  </td>
                } @else {
                  <!-- Only one-time items, no plan (EP-12) -->
                  @for (column of planColumns; track column) {
                    <td class="amount no-plan">
                      <span class="value" [attr.aria-label]="'Kein Haushaltsplan'">–</span>
                    </td>
                  }
                }
                <td class="amount one-time first">
                  <span class="value">{{ entry.oneTimeIncome | currency: 'EUR' }}</span>
                </td>
                <td class="amount one-time">
                  <span class="value">{{ entry.oneTimeExpense | currency: 'EUR' }}</span>
                </td>
                <td class="amount one-time">
                  <ng-container *ngTemplateOutlet="oneTimeBalance; context: { $implicit: entry }" />
                </td>
                <td class="actions">
                  @if (hasPlan(entry)) {
                    <button
                      matIconButton
                      type="button"
                      [attr.aria-label]="'Plan ' + entry.year + ' als Vorlage übernehmen'"
                      title="Als Vorlage übernehmen"
                      (click)="$event.stopPropagation(); copyPlan(entry.year)"
                    >
                      <mat-icon svgIcon="content_copy" />
                    </button>
                    <button
                      matIconButton
                      type="button"
                      class="delete-plan"
                      [attr.aria-label]="'Plan ' + entry.year + ' löschen'"
                      title="Plan löschen"
                      (click)="$event.stopPropagation(); deletePlan(entry.year)"
                    >
                      <mat-icon svgIcon="delete" />
                    </button>
                  }
                </td>
              </tr>
              <!-- The one-time totals of the year on a phone, below those of the plan (EP-18) -->
              <tr
                class="one-time-row"
                [attr.data-one-time-year]="entry.year"
                (click)="open(entry.year)"
              >
                <th scope="row" class="one-time-label">Einmalig (Jahr)</th>
                <td class="amount">
                  <span class="value">{{ entry.oneTimeIncome | currency: 'EUR' }}</span>
                </td>
                <td class="amount">
                  <span class="value">{{ entry.oneTimeExpense | currency: 'EUR' }}</span>
                </td>
                <td class="amount">
                  <ng-container *ngTemplateOutlet="oneTimeBalance; context: { $implicit: entry }" />
                </td>
                <td class="actions"></td>
              </tr>
            }
          </tbody>
        </table>
      </mat-card>
    }

    @if (store.deletedPlanCount() > 0) {
      <a matButton routerLink="/geloeschte-plaene" class="deleted-plans">
        <mat-icon svgIcon="delete" />
        Gelöschte Pläne ({{ store.deletedPlanCount() }})
      </a>
    }

    <!-- Independent of the plans: shown also without any, never in monthly amounts (VS-18, VS-19) -->
    <app-net-worth-card />
    <ng-template #oneTimeBalance let-entry>
      <span class="value" [class.deficit]="entry.oneTimeExpensesExceedIncome">
        {{ entry.oneTimeBalance | currency: 'EUR' }}
      </span>
      @if (entry.oneTimeExpensesExceedIncome) {
        <span class="deficit-label">Defizit</span>
      }
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
    h1 {
      margin: 0;
      font: var(--mat-sys-headline-small);
    }
    .heading {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
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
    .create mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
      padding-top: 16px;
    }
    .create p {
      margin: 0;
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th,
    td {
      padding: 12px 16px;
      text-align: left;
      vertical-align: top;
    }
    thead th {
      font: var(--mat-sys-label-large);
      color: var(--mat-sys-on-surface-variant);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .group-head > * {
      padding-bottom: 0;
      border-bottom: none;
      text-align: center;
      font: var(--mat-sys-label-medium);
      color: var(--mat-sys-on-surface-variant);
    }
    /* The one-time totals are set apart from those of the plan (EP-13) */
    .one-time {
      background: var(--mat-sys-surface-container-low);
    }
    .one-time.first {
      border-left: 1px solid var(--mat-sys-outline-variant);
    }
    .no-plan {
      color: var(--mat-sys-on-surface-variant);
    }
    tbody tr {
      cursor: pointer;
    }
    tbody tr.plan-row:not(:nth-last-child(2)) {
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    tbody tr:hover {
      background: var(--mat-sys-surface-container-high);
    }
    .one-time-row {
      display: none;
    }
    tbody th a {
      font: var(--mat-sys-title-medium);
      color: var(--mat-sys-primary);
      text-decoration: none;
    }
    .amount {
      text-align: right;
    }
    .actions {
      width: 96px;
      padding: 4px 8px;
      vertical-align: middle;
      white-space: nowrap;
    }
    .delete-plan {
      --mat-icon-button-icon-color: var(--mat-sys-error);
      --mat-icon-button-state-layer-color: var(--mat-sys-error);
    }
    .deleted-plans {
      align-self: flex-start;
    }
    .visually-hidden {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
    td.amount {
      font-variant-numeric: tabular-nums;
    }
    .value,
    .deficit-label {
      display: block;
    }
    .deficit-label {
      font: var(--mat-sys-body-small);
    }
    .deficit,
    .deficit-label {
      color: var(--mat-sys-error);
    }
    /* Scrolls inside the card rather than widening the page, should the amounts get very long (MA-1) */
    .years {
      overflow-x: auto;
    }
    @media (max-width: 600px) {
      th,
      td {
        padding: 8px 4px;
      }
      /* A row of its own for the one-time totals instead of three more columns (EP-18) */
      .one-time,
      .group-head {
        display: none;
      }
      .one-time-row {
        display: table-row;
        font: var(--mat-sys-body-small);
        background: var(--mat-sys-surface-container-low);
      }
      tbody tr.plan-row {
        border-bottom: none !important;
      }
      tbody tr.one-time-row:not(:last-child) {
        border-bottom: 1px solid var(--mat-sys-outline-variant);
      }
      .one-time-label {
        font: var(--mat-sys-label-small);
        color: var(--mat-sys-on-surface-variant);
      }
      /* The buttons on top of each other, so the four columns of amounts fit on a phone (MA-4) */
      .actions {
        width: 48px;
        padding: 4px 0;
      }
      .actions button {
        display: block;
      }
    }
  `,
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
