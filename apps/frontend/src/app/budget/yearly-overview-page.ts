import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { Year } from './budget.model';
import { YearlyOverviewStore } from './data-access/yearly-overview-store';
import { copiedMessage, openCopyPlanDialog } from './ui/copy-plan-dialog';
import { deletedMessage, openDeletePlanDialog } from './ui/delete-plan-dialog';
import { YearChange } from './ui/year-change';

/**
 * The start page after signing in: all plans of the user with their income, expenses and
 * balance and how these changed from the previous year (JU-1 to JU-13). A row opens the plan
 * of its year.
 */
@Component({
  selector: 'app-yearly-overview-page',
  imports: [
    CurrencyPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    RouterLink,
    YearChange,
  ],
  providers: [YearlyOverviewStore],
  template: `
    @if (store.loading()) {
      <mat-progress-bar mode="indeterminate" class="loading" />
    }

    <h1>Haushaltspläne</h1>

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
      <mat-card appearance="outlined">
        <table>
          <thead>
            <tr>
              <th scope="col">Jahr</th>
              <th scope="col" class="amount">Einnahmen</th>
              <th scope="col" class="amount">Ausgaben</th>
              <th scope="col" class="amount">Überschuss</th>
              <th scope="col" class="actions"><span class="visually-hidden">Aktionen</span></th>
            </tr>
          </thead>
          <tbody>
            @for (entry of store.years(); track entry.planId) {
              <tr (click)="open(entry.year)" [attr.data-year]="entry.year">
                <th scope="row">
                  <a [routerLink]="['/jahre', entry.year]" (click)="$event.stopPropagation()">{{
                    entry.year
                  }}</a>
                </th>
                <td class="amount">
                  <span class="value">{{ entry.totalIncome | currency: 'EUR' }}</span>
                  <app-year-change kind="income" [change]="entry.incomeChange" />
                </td>
                <td class="amount">
                  <span class="value">{{ entry.totalExpense | currency: 'EUR' }}</span>
                  <app-year-change kind="expense" [change]="entry.expenseChange" />
                </td>
                <td class="amount">
                  <span class="value" [class.deficit]="entry.expensesExceedIncome">
                    {{ entry.balance | currency: 'EUR' }}
                  </span>
                  @if (entry.expensesExceedIncome) {
                    <span class="deficit-label">Defizit</span>
                  }
                  <app-year-change kind="balance" [change]="entry.balanceChange" />
                </td>
                <td class="actions">
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
    tbody tr {
      cursor: pointer;
    }
    tbody tr:not(:last-child) {
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    tbody tr:hover {
      background: var(--mat-sys-surface-container-high);
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
    @media (max-width: 600px) {
      th,
      td {
        padding: 8px;
      }
    }
  `,
})
export class YearlyOverviewPage implements OnInit {
  protected readonly store = inject(YearlyOverviewStore);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  protected readonly creating = signal(false);

  ngOnInit(): void {
    void this.store.load();
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
        this.snackBar.open(deletedMessage(year), undefined, { duration: 3000 });
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
