import { CurrencyPipe, DatePipe } from '@angular/common';
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
    DatePipe,
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
          <p class="empty">Keine Einträge in diesem Monat.</p>
        } @else {
          <table mat-table [dataSource]="items()">
            <ng-container matColumnDef="bookingDate">
              <th mat-header-cell *matHeaderCellDef>Datum</th>
              <td mat-cell *matCellDef="let item" class="date">
                {{ item.bookingDate | date: 'dd.MM.' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef>Art</th>
              <td mat-cell *matCellDef="let item">
                {{ item.type === 'INCOME' ? 'Einnahme' : 'Ausgabe' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="description">
              <th mat-header-cell *matHeaderCellDef>Beschreibung</th>
              <td mat-cell *matCellDef="let item">{{ item.description }}</td>
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
    .date {
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }
    .actions {
      width: 48px;
    }
    .empty {
      color: var(--mat-sys-on-surface-variant);
      padding: 16px 0 0;
    }
  `,
})
export class BudgetItemTable {
  readonly items = input.required<BudgetItem[]>();
  readonly remove = output<BudgetItem>();

  protected readonly columns = [
    'bookingDate',
    'type',
    'description',
    'category',
    'amount',
    'actions',
  ];

  protected label(item: BudgetItem): string {
    return categoryLabel(item.category);
  }
}
