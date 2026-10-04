import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
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
    MatIconModule,
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

    <nav class="year" aria-label="Jahr wählen">
      <button
        matIconButton
        type="button"
        aria-label="Vorheriges Jahr"
        (click)="store.previousYear()"
      >
        <mat-icon svgIcon="chevron_left" />
      </button>
      <h1>{{ store.year() }}</h1>
      <button matIconButton type="button" aria-label="Nächstes Jahr" (click)="store.nextYear()">
        <mat-icon svgIcon="chevron_right" />
      </button>
    </nav>

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
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
      return;
    }
    const undo = this.snackBar.open(`„${item.description}" gelöscht`, 'Rückgängig', {
      duration: 6000,
    });
    undo.onAction().subscribe(() => void this.restore(item));
  }

  private async restore(item: BudgetItem): Promise<void> {
    try {
      await this.store.restoreItem(item);
    } catch (error) {
      this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
    }
  }
}
