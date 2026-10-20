import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { CategoryBreakdown, categoryLabel, missedTargetLabel, targetLabel } from '../budget.model';

interface Slice {
  breakdown: CategoryBreakdown;
  /** SVG path of the slice; `null` when the slice is the whole circle. */
  path: string | null;
}

const RADIUS = 80;

/**
 * Pie chart of the 50/30/20 breakdown (TD-2 to TD-9). Slice sizes follow the category totals,
 * savings including the surplus (TD-3); without a deficit they add up to the income. The legend
 * shows the amounts per month (MJ-14) and the shares and target hints as computed by the domain
 * (TD-8, AW-9).
 */
@Component({
  selector: 'app-category-pie',
  imports: [CurrencyPipe, DecimalPipe],
  template: `
    <svg viewBox="-100 -100 200 200" class="pie" role="group" aria-label="Tortendiagramm">
      @for (slice of slices(); track slice.breakdown.category) {
        @let b = slice.breakdown;
        @let description =
          label(b) +
          ': ' +
          (b.monthlyTotal | currency: 'EUR') +
          ' im Monat, ' +
          (b.actualPercentageOfIncome | number: '1.0-2') +
          ' % des Einkommens';
        @if (slice.path; as path) {
          <path
            class="slice"
            [class]="b.category"
            [attr.d]="path"
            role="img"
            [attr.aria-label]="description"
          >
            <title>{{ description }}</title>
          </path>
        } @else {
          <circle
            class="slice"
            [class]="b.category"
            [attr.r]="radius"
            role="img"
            [attr.aria-label]="description"
          >
            <title>{{ description }}</title>
          </circle>
        }
      } @empty {
        <circle class="empty" [attr.r]="radius" />
      }
    </svg>
    @if (slices().length === 0) {
      <p class="empty-hint">Noch keine Ausgaben oder Einnahmen</p>
    }
    <ul class="legend">
      @for (breakdown of breakdowns(); track breakdown.category) {
        <li class="legend-item" [class.missed]="!breakdown.targetMet">
          <span class="swatch" [class]="breakdown.category"></span>
          <div class="legend-text">
            <div class="legend-header">
              <span class="name">{{ label(breakdown) }}</span>
              <span class="total">{{ breakdown.monthlyTotal | currency: 'EUR' }}</span>
            </div>
            @if (hasUnplanned(breakdown)) {
              <span class="unplanned">
                davon nicht verplant: {{ breakdown.monthlyUnplanned | currency: 'EUR' }}
              </span>
            }
            <span class="ratio">
              {{ breakdown.actualPercentageOfIncome | number: '1.0-2' }} % von
              {{ target(breakdown) }} %
              @if (!breakdown.targetMet) {
                · {{ missed(breakdown) }}
              }
            </span>
          </div>
        </li>
      }
    </ul>
  `,
  styleUrl: './category-pie.scss',
})
export class CategoryPieChart {
  readonly breakdowns = input.required<CategoryBreakdown[]>();

  protected readonly radius = RADIUS;

  /** Slices of the categories with a total above zero, clockwise from twelve o'clock (TD-6). */
  protected readonly slices = computed<Slice[]>(() => {
    const filled = this.breakdowns().filter((b) => Number(b.total) > 0);
    const sum = filled.reduce((acc, b) => acc + Number(b.total), 0);
    let start = 0;
    return filled.map((breakdown) => {
      const fraction = Number(breakdown.total) / sum;
      const end = start + fraction;
      const path = fraction >= 1 ? null : arc(start, end);
      start = end;
      return { breakdown, path };
    });
  });

  protected label(breakdown: CategoryBreakdown): string {
    return categoryLabel(breakdown.category);
  }

  protected hasUnplanned(breakdown: CategoryBreakdown): boolean {
    return Number(breakdown.unplanned) > 0;
  }

  protected target(breakdown: CategoryBreakdown): string {
    return targetLabel(breakdown);
  }

  protected missed(breakdown: CategoryBreakdown): string {
    return missedTargetLabel(breakdown);
  }
}

/** Path of a pie slice between two fractions of the full circle, starting at twelve o'clock. */
function arc(start: number, end: number): string {
  const [x1, y1] = point(start);
  const [x2, y2] = point(end);
  const largeArc = end - start > 0.5 ? 1 : 0;
  return `M 0 0 L ${x1} ${y1} A ${RADIUS} ${RADIUS} 0 ${largeArc} 1 ${x2} ${y2} Z`;
}

function point(fraction: number): [number, number] {
  const angle = fraction * 2 * Math.PI;
  return [round(RADIUS * Math.sin(angle)), round(-RADIUS * Math.cos(angle))];
}

function round(value: number): number {
  return Math.round(value * 1000) / 1000;
}
