import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { CategoryBreakdown, categoryLabel } from '../budget.model';

/** Visualizes actual spend per category against its 50/30/20 target share of income. */
@Component({
  selector: 'app-category-breakdown',
  imports: [CurrencyPipe, DecimalPipe, MatCardModule, MatProgressBarModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>50/30/20-Regel</mat-card-title>
        <mat-card-subtitle>Anteil der Ausgaben am Einkommen je Kategorie</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        @for (breakdown of breakdowns(); track breakdown.category) {
          <div class="row" [class.exceeded]="!breakdown.withinTarget">
            <div class="row-header">
              <span class="name">{{ label(breakdown) }}</span>
              <span class="total">{{ breakdown.total | currency: 'EUR' }}</span>
            </div>
            <mat-progress-bar
              mode="determinate"
              [value]="progress(breakdown)"
              [attr.aria-label]="label(breakdown)"
            />
            <span class="ratio">
              {{ breakdown.actualPercentageOfIncome | number: '1.0-2' }} % von
              {{ breakdown.targetPercentageOfIncome }} %
              @if (!breakdown.withinTarget) {
                · Ziel überschritten
              }
            </span>
          </div>
        }
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './category-breakdown.scss',
})
export class CategoryBreakdownCard {
  readonly breakdowns = input.required<CategoryBreakdown[]>();

  protected label(breakdown: CategoryBreakdown): string {
    return categoryLabel(breakdown.category);
  }

  /** Fill level relative to the category's target, capped at 100 %. */
  protected progress(breakdown: CategoryBreakdown): number {
    if (breakdown.targetPercentageOfIncome <= 0) {
      return 0;
    }
    return Math.min(100, (breakdown.actualPercentageOfIncome / breakdown.targetPercentageOfIncome) * 100);
  }
}
