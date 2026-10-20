import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, input, model } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  BreakdownView,
  CategoryBreakdown,
  categoryLabel,
  missedTargetLabel,
  targetLabel,
} from '../budget.model';
import { CategoryPieChart } from './category-pie';

/**
 * Visualizes actual spend per category against its 50/30/20 target share of income.
 * Living costs and leisure have a ceiling, savings a floor. Income that no expense uses up
 * counts towards savings and is shown there as unplanned (AW-10). A toggle switches between
 * the bars and a pie chart (TD-1). Both show the amounts per month (MJ-14).
 */
@Component({
  selector: 'app-category-breakdown',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    MatButtonToggleModule,
    MatCardModule,
    MatProgressBarModule,
    CategoryPieChart,
  ],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>50/30/20-Regel</mat-card-title>
        <mat-card-subtitle>Beträge je Monat, Anteil am Einkommen je Kategorie</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <mat-button-toggle-group
          class="view-toggle"
          hideSingleSelectionIndicator
          aria-label="Darstellung"
          [value]="view()"
          (change)="view.set($event.value)"
        >
          <mat-button-toggle value="bars">Balken</mat-button-toggle>
          <mat-button-toggle value="pie">Torte</mat-button-toggle>
        </mat-button-toggle-group>
        @if (view() === 'pie') {
          <app-category-pie [breakdowns]="breakdowns()" />
        } @else {
          @for (breakdown of breakdowns(); track breakdown.category) {
            <div class="row" [class.missed]="!breakdown.targetMet">
              <div class="row-header">
                <span class="name">{{ label(breakdown) }}</span>
                <span class="total">{{ breakdown.monthlyTotal | currency: 'EUR' }}</span>
              </div>
              @if (hasUnplanned(breakdown)) {
                <span class="unplanned">
                  davon nicht verplant: {{ breakdown.monthlyUnplanned | currency: 'EUR' }}
                </span>
              }
              <mat-progress-bar
                mode="determinate"
                [value]="progress(breakdown)"
                [attr.aria-label]="label(breakdown)"
              />
              <span class="ratio">
                {{ breakdown.actualPercentageOfIncome | number: '1.0-2' }} % von
                {{ target(breakdown) }} %
                @if (!breakdown.targetMet) {
                  · {{ missed(breakdown) }}
                }
              </span>
            </div>
          }
        }
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './category-breakdown.scss',
})
export class CategoryBreakdownCard {
  readonly breakdowns = input.required<CategoryBreakdown[]>();
  /** Selected view; the parent keeps it, so it survives switching the year (TD-1). */
  readonly view = model<BreakdownView>('bars');

  protected label(breakdown: CategoryBreakdown): string {
    return categoryLabel(breakdown.category);
  }

  protected target(breakdown: CategoryBreakdown): string {
    return targetLabel(breakdown);
  }

  protected missed(breakdown: CategoryBreakdown): string {
    return missedTargetLabel(breakdown);
  }

  protected hasUnplanned(breakdown: CategoryBreakdown): boolean {
    return Number(breakdown.unplanned) > 0;
  }

  /** Fill level relative to the category's target, capped at 100 %. */
  protected progress(breakdown: CategoryBreakdown): number {
    if (breakdown.targetPercentageOfIncome <= 0) {
      return 0;
    }
    return Math.min(
      100,
      (breakdown.actualPercentageOfIncome / breakdown.targetPercentageOfIncome) * 100,
    );
  }
}
