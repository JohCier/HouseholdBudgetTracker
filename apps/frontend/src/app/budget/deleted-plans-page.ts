import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { DeletedPlan } from './budget.model';
import { BudgetApi } from './data-access/budget-api';
import { DeletedPlansStore } from './data-access/deleted-plans-store';
import { storedPeriod } from './data-access/yearly-overview-store';
import { openPurgePlanDialog, purgedMessage } from './ui/purge-plan-dialog';
import { restorePlan, restoredMessage } from './ui/restore-plan-dialog';

/**
 * The deleted plans of the user with their year, when they were deleted, their items and
 * amounts; each can be restored or deleted for good (GP-21 to GP-26). The amounts are monthly
 * or yearly as chosen in the yearly overview (GP-23).
 */
@Component({
  selector: 'app-deleted-plans-page',
  imports: [
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    RouterLink,
  ],
  providers: [DeletedPlansStore],
  template: `
    @if (store.loading()) {
      <mat-progress-bar mode="indeterminate" class="loading" />
    }

    <a matButton routerLink="/" class="back">
      <mat-icon svgIcon="arrow_back" />
      Alle Jahre
    </a>

    <h1>Gelöschte Pläne</h1>

    @if (store.loadError(); as error) {
      <div class="load-error" role="alert">
        <span>Daten konnten nicht geladen werden: {{ error }}</span>
        <button matButton type="button" (click)="store.load()">Erneut versuchen</button>
      </div>
    }

    @if (store.empty()) {
      <p class="empty">Keine gelöschten Pläne.</p>
    }

    @if (store.plans().length > 0) {
      <mat-card appearance="outlined">
        <table>
          <thead>
            <tr>
              <th scope="col">Jahr</th>
              <th scope="col">Gelöscht am</th>
              <th scope="col" class="amount">Posten</th>
              <th scope="col" class="amount">Einnahmen</th>
              <th scope="col" class="amount">Ausgaben</th>
              <th scope="col" class="amount">Überschuss</th>
              <th scope="col" class="actions"><span class="visually-hidden">Aktionen</span></th>
            </tr>
          </thead>
          <tbody>
            @for (plan of store.plans(); track plan.planId) {
              <tr [attr.data-plan-id]="plan.planId">
                <th scope="row" class="year">{{ plan.year }}</th>
                <td class="deleted-at">
                  {{ plan.deletedAt | date: 'dd.MM.yyyy, HH:mm' }}
                </td>
                <td class="amount items">
                  <span class="label">Posten</span>
                  {{ plan.itemCount }}
                </td>
                <td class="amount">
                  <span class="label">Einnahmen</span>
                  {{ income(plan) | currency: 'EUR' }}
                </td>
                <td class="amount">
                  <span class="label">Ausgaben</span>
                  {{ expense(plan) | currency: 'EUR' }}
                </td>
                <td class="amount">
                  <span class="label">Überschuss</span>
                  <span [class.deficit]="plan.expensesExceedIncome">
                    {{ balance(plan) | currency: 'EUR' }}
                  </span>
                  @if (plan.expensesExceedIncome) {
                    <span class="deficit deficit-label">Defizit</span>
                  }
                </td>
                <td class="actions">
                  <button matButton type="button" (click)="restore(plan)">Wiederherstellen</button>
                  <button matButton type="button" class="purge" (click)="purge(plan)">
                    Endgültig löschen
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </mat-card>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 16px;
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
    }
    h1 {
      margin: 0;
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
    .empty {
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
      vertical-align: middle;
    }
    thead th {
      font: var(--mat-sys-label-large);
      color: var(--mat-sys-on-surface-variant);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    tbody tr:not(:last-child) {
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .year {
      font: var(--mat-sys-title-medium);
    }
    .deleted-at {
      color: var(--mat-sys-on-surface-variant);
      white-space: nowrap;
    }
    .amount {
      text-align: right;
      font-variant-numeric: tabular-nums;
    }
    .label {
      display: none;
    }
    .deficit-label {
      display: block;
      font: var(--mat-sys-body-small);
    }
    .deficit {
      color: var(--mat-sys-error);
    }
    .actions {
      padding: 4px 8px;
      text-align: right;
      white-space: nowrap;
    }
    .purge {
      --mat-button-text-label-text-color: var(--mat-sys-error);
      --mat-button-text-state-layer-color: var(--mat-sys-error);
    }
    .visually-hidden {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
    /* Each plan as a block without scrolling sideways: year and date on top, the items and
       amounts with their labels below, the actions at the bottom (GP-26) */
    @media (max-width: 600px) {
      thead {
        display: none;
      }
      tbody tr {
        display: grid;
        grid-template-columns: auto 1fr;
        align-items: center;
        column-gap: 8px;
        padding: 8px 12px;
      }
      th,
      td {
        padding: 0;
      }
      .year {
        grid-row: 1;
        grid-column: 1;
      }
      .deleted-at {
        grid-row: 1;
        grid-column: 2;
        font: var(--mat-sys-body-small);
      }
      .actions {
        grid-column: 1 / -1;
        display: flex;
        justify-content: flex-end;
        flex-wrap: wrap;
        padding: 0;
      }
      .amount {
        grid-column: 1 / -1;
        display: flex;
        justify-content: space-between;
        font: var(--mat-sys-body-medium);
      }
      .amount .label {
        display: inline;
        color: var(--mat-sys-on-surface-variant);
      }
      .amount .deficit-label {
        display: none;
      }
      .items {
        margin-top: 4px;
      }
    }
  `,
})
export class DeletedPlansPage implements OnInit {
  protected readonly store = inject(DeletedPlansStore);
  private readonly api = inject(BudgetApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly monthly = storedPeriod() === 'MONTHLY';

  ngOnInit(): void {
    void this.store.load();
  }

  /** The amounts of the period chosen in the yearly overview (GP-23). */
  protected income(plan: DeletedPlan): string {
    return this.monthly ? plan.monthlyTotalIncome : plan.totalIncome;
  }

  protected expense(plan: DeletedPlan): string {
    return this.monthly ? plan.monthlyTotalExpense : plan.totalExpense;
  }

  protected balance(plan: DeletedPlan): string {
    return this.monthly ? plan.monthlyBalance : plan.balance;
  }



  /** Restores the plan, asking first if its year has an active plan; the page stays open (GP-12 to GP-14). */
  protected async restore(plan: DeletedPlan): Promise<void> {
    try {
      if (await restorePlan(this.api, this.dialog, plan)) {
        await this.store.load();
        this.snackBar.open(restoredMessage(plan.year), undefined, { duration: 3000 });
      }
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }

  /** Deletes the plan for good after asking (GP-16, GP-17). */
  protected purge(plan: DeletedPlan): void {
    openPurgePlanDialog(this.dialog, plan).subscribe(async (purged) => {
      if (purged) {
        await this.store.load();
        this.snackBar.open(purgedMessage(plan.year), undefined, { duration: 3000 });
      }
    });
  }
}
