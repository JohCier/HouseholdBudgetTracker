import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BudgetItem } from './budget.model';
import { BudgetStore } from './data-access/budget-store';
import { AddItemForm } from './ui/add-item-form';
import { BudgetItemTable } from './ui/budget-item-table';
import { CategoryBreakdownCard } from './ui/category-breakdown';
import { OverviewSummary } from './ui/overview-summary';

@Component({
  selector: 'app-budget-dashboard',
  imports: [
    MatButtonModule,
    MatProgressBarModule,
    AddItemForm,
    BudgetItemTable,
    CategoryBreakdownCard,
    OverviewSummary,
  ],
  template: `
    @if (store.loading()) {
      <mat-progress-bar mode="indeterminate" class="loading" />
    }

    @if (store.loadError(); as error) {
      <div class="load-error" role="alert">
        <span>Daten konnten nicht geladen werden: {{ error }}</span>
        <button matButton type="button" (click)="store.load()">Erneut versuchen</button>
      </div>
    }

    @if (store.overview(); as overview) {
      <app-overview-summary [overview]="overview" />
    }

    <div class="grid">
      <div class="side">
        <app-add-item-form />
        @if (store.overview(); as overview) {
          <app-category-breakdown [breakdowns]="overview.categoryBreakdowns" />
        }
      </div>
      <app-budget-item-table [items]="store.items()" (remove)="remove($event)" />
    </div>
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
    .side {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    @media (max-width: 860px) {
      .grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class BudgetDashboard implements OnInit {
  protected readonly store = inject(BudgetStore);
  private readonly snackBar = inject(MatSnackBar);

  ngOnInit(): void {
    void this.store.load();
  }

  protected async remove(item: BudgetItem): Promise<void> {
    try {
      await this.store.removeItem(item.id);
      this.snackBar.open(`„${item.description}" gelöscht`, undefined, { duration: 3000 });
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }
}
