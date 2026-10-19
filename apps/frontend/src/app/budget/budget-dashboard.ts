import { Component, Injector, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BreakdownView, BudgetItem } from './budget.model';
import { BudgetStore } from './data-access/budget-store';
import { AddItemForm } from './ui/add-item-form';
import { BudgetItemTable } from './ui/budget-item-table';
import { CategoryBreakdownCard } from './ui/category-breakdown';
import { EditItemDialog } from './ui/edit-item-dialog';
import { OverviewSummary } from './ui/overview-summary';

@Component({
  selector: 'app-budget-dashboard',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    AddItemForm,
    BudgetItemTable,
    CategoryBreakdownCard,
    OverviewSummary,
  ],
  providers: [BudgetStore],
  template: `
    @if (store.loading()) {
      <mat-progress-bar mode="indeterminate" class="loading" />
    }

    <nav class="year" aria-label="Jahr wählen">
      <button
        matIconButton
        type="button"
        aria-label="Vorheriges Jahr"
        [disabled]="!store.hasPreviousYear()"
        (click)="store.previousYear()"
      >
        <mat-icon svgIcon="chevron_left" />
      </button>
      <h1>{{ store.year() }}</h1>
      <button
        matIconButton
        type="button"
        aria-label="Nächstes Jahr"
        [disabled]="!store.hasNextYear()"
        (click)="store.nextYear()"
      >
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

    @if (store.planId()) {
      <div class="grid">
        <div class="side">
          <app-add-item-form />
          @if (store.overview(); as overview) {
            <app-category-breakdown
              [breakdowns]="overview.categoryBreakdowns"
              [(view)]="breakdownView"
            />
          }
        </div>
        <app-budget-item-table
          [items]="store.items()"
          (edit)="edit($event)"
          (remove)="remove($event)"
        />
      </div>
    } @else if (store.planMissing() && !store.loadError()) {
      <mat-card appearance="outlined" class="no-plan">
        <mat-card-content>
          <p>Für {{ store.year() }} gibt es noch keinen Haushaltsplan.</p>
          <button matButton="filled" type="button" [disabled]="creating()" (click)="createPlan()">
            Haushaltsplan anlegen
          </button>
        </mat-card-content>
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
  private readonly dialog = inject(MatDialog);
  private readonly injector = inject(Injector);

  protected readonly creating = signal(false);
  /** View of the 50/30/20 card, kept while the page is open (TD-1). */
  protected readonly breakdownView = signal<BreakdownView>('bars');

  ngOnInit(): void {
    void this.store.load();
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
