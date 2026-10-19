import { CurrencyPipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BudgetItem, categoryLabel } from '../budget.model';

@Component({
  selector: 'app-budget-item-table',
  imports: [
    CurrencyPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
  ],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>Einträge</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @if (items().length === 0) {
          <p class="empty">Noch keine Einträge in diesem Haushaltsplan.</p>
        } @else {
          <table mat-table [dataSource]="items()" [trackBy]="trackById">
            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef>Art</th>
              <td mat-cell *matCellDef="let item">
                {{ item.type === 'INCOME' ? 'Einnahme' : 'Ausgabe' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="description">
              <th mat-header-cell *matHeaderCellDef>Beschreibung</th>
              <td mat-cell *matCellDef="let item">
                {{ item.description }}
                @if (item.note) {
                  <div class="note">{{ item.note }}</div>
                }
              </td>
            </ng-container>
            <ng-container matColumnDef="category">
              <th mat-header-cell *matHeaderCellDef>Kategorie</th>
              <td mat-cell *matCellDef="let item">{{ label(item) }}</td>
            </ng-container>
            <ng-container matColumnDef="amount">
              <th mat-header-cell *matHeaderCellDef class="amount">Betrag</th>
              <td
                mat-cell
                *matCellDef="let item"
                class="amount"
                [class.income]="item.type === 'INCOME'"
                [class.expense]="item.type === 'EXPENSE'"
              >
                {{ item.type === 'EXPENSE' ? '−' : '+' }}{{ item.amount | currency: 'EUR' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let item" class="actions">
                <button
                  matIconButton
                  type="button"
                  matTooltip="Bearbeiten"
                  [attr.aria-label]="'Eintrag ' + item.description + ' bearbeiten'"
                  (click)="edit.emit(item)"
                >
                  <mat-icon svgIcon="edit" />
                </button>
                <button
                  matIconButton
                  type="button"
                  matTooltip="Löschen"
                  [attr.aria-label]="'Eintrag ' + item.description + ' löschen'"
                  (click)="remove.emit(item)"
                >
                  <mat-icon svgIcon="delete" />
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="columns"></tr>
            <tr mat-row *matRowDef="let row; columns: columns"></tr>
          </table>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    table {
      width: 100%;
      background: transparent;
    }
    .amount {
      text-align: right;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }
    .income {
      color: var(--app-income-color);
    }
    .expense {
      color: var(--mat-sys-error);
    }
    .actions {
      width: 96px;
      white-space: nowrap;
    }
    .note {
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-small);
      white-space: pre-line;
      overflow-wrap: anywhere;
    }
    .empty {
      color: var(--mat-sys-on-surface-variant);
      padding: 16px 0 0;
    }
  `,
})
export class BudgetItemTable {
  readonly items = input.required<BudgetItem[]>();
  readonly edit = output<BudgetItem>();
  readonly remove = output<BudgetItem>();

  protected readonly columns = ['type', 'description', 'category', 'amount', 'actions'];

  /**
   * Every reload of the overview brings new objects for the same items; tracking them by id keeps
   * the rows of unchanged items instead of building all rows anew, and keeps the focus in place.
   */
  protected readonly trackById = (_index: number, item: BudgetItem): string => item.id;

  protected label(item: BudgetItem): string {
    return categoryLabel(item.category);
  }
}
