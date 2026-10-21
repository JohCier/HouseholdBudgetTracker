import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, input, linkedSignal, model } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import {
  CategoryBreakdown,
  CategoryItems,
  ExpenseCategory,
  ItemGroup,
  PieSelection,
  RULE_SELECTION,
  categoryLabel,
  isSameSubcategory,
  missedTargetLabel,
  targetLabel,
} from '../budget.model';

/** One slice of the pie and its line in the legend. */
interface PieEntry {
  key: string;
  label: string;
  /** Amount per month shown in the legend (MJ-14, TK-8) */
  monthly: string;
  /** Size of the slice relative to the others */
  size: number;
  /** Share shown in the legend: of the income (TD-5) or of the selection (TK-8) */
  percentage: number;
  /** What the percentage refers to, for screen readers (TD-9, TK-12) */
  percentUnit: string;
  /** Category class that colors the slice (TD-2); `null` when `fill` does */
  cssClass: string | null;
  /** Shade of the category color (TK-10); `null` when `cssClass` does */
  fill: string | null;
  /** What clicking the slice selects (TK-4); `null` if nothing */
  target: PieSelection | null;
}

interface Slice {
  entry: PieEntry;
  /** SVG path of the slice; `null` when the slice is the whole circle. */
  path: string | null;
}

interface SelectionOption {
  key: string;
  label: string;
  /** Name for screen readers: a group with its category, e.g. "Lebensunterhalt › Auto" (TK-3) */
  fullLabel: string;
  selection: PieSelection;
  nested: boolean;
}

const RADIUS = 80;

const CATEGORY_COLORS: Record<ExpenseCategory, string> = {
  LEBENSUNTERHALT: 'var(--app-lebensunterhalt-color)',
  FREIZEIT_SPASS: 'var(--app-freizeit-spass-color)',
  SPAREN_INVESTIEREN: 'var(--app-sparen-investieren-color)',
};

/** Color of the slice "nicht verplant" (TK-9, TK-10). */
const UNPLANNED_FILL = 'var(--mat-sys-outline)';

/** Lightest shade of a category color, in percent of the color mixed into the surface (TK-10). */
const LIGHTEST_SHADE = 40;

/**
 * Pie chart of the plan (TD-2 to TD-9, TK-1 to TK-14). By default it shows the 50/30/20 breakdown;
 * slice sizes follow the category totals, savings including the surplus (TD-3). A selection
 * narrows it to the subcategory groups of one category or the items of one group, sized by their
 * amounts per month (TK-6). All amounts and shares come from the overview (TD-8, TK-8); the pie
 * only divides the circle in proportion to them.
 */
@Component({
  selector: 'app-category-pie',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
  ],
  template: `
    <div class="selection">
      <mat-form-field class="selection-field" subscriptSizing="dynamic">
        <mat-label>Ausschnitt</mat-label>
        <mat-select [value]="shownKey()" (selectionChange)="selectKey($event.value)">
          <mat-select-trigger>{{ shownLabel() }}</mat-select-trigger>
          @for (option of options(); track option.key) {
            <mat-option
              [value]="option.key"
              [class.nested-option]="option.nested"
              [attr.aria-label]="option.fullLabel"
            >
              {{ option.label }}
            </mat-option>
          }
        </mat-select>
      </mat-form-field>
      @if (shown().level !== 'rule') {
        <button mat-button type="button" class="back" (click)="back()">
          <mat-icon svgIcon="arrow_back" />
          Zurück
        </button>
      }
    </div>
    <svg viewBox="-100 -100 200 200" class="pie" role="group" aria-label="Tortendiagramm">
      @for (slice of slices(); track slice.entry.key) {
        @let e = slice.entry;
        @let description =
          e.label +
          ': ' +
          (e.monthly | currency: 'EUR') +
          ' im Monat, ' +
          (e.percentage | number: '1.0-2') +
          ' ' +
          e.percentUnit;
        @let ariaLabel =
          e.target ? description + '; ' + targetLabel(e.target) + ' anzeigen' : description;
        @if (slice.path; as path) {
          <path
            class="slice"
            [class]="e.cssClass"
            [class.selectable]="e.target"
            [style.fill]="e.fill"
            [attr.d]="path"
            [attr.role]="e.target ? 'button' : 'img'"
            [attr.tabindex]="e.target ? 0 : null"
            [attr.aria-label]="ariaLabel"
            [class.highlighted]="highlighted() === e.key"
            (click)="open(e)"
            (keydown.enter)="open(e)"
            (mouseenter)="highlight(e)"
            (mouseleave)="unhighlight(e)"
            (focus)="highlight(e)"
            (blur)="unhighlight(e)"
          >
            <title>{{ description }}</title>
          </path>
        } @else {
          <circle
            class="slice"
            [class]="e.cssClass"
            [class.selectable]="e.target"
            [style.fill]="e.fill"
            [attr.r]="radius"
            [attr.role]="e.target ? 'button' : 'img'"
            [attr.tabindex]="e.target ? 0 : null"
            [attr.aria-label]="ariaLabel"
            [class.highlighted]="highlighted() === e.key"
            (click)="open(e)"
            (keydown.enter)="open(e)"
            (mouseenter)="highlight(e)"
            (mouseleave)="unhighlight(e)"
            (focus)="highlight(e)"
            (blur)="unhighlight(e)"
          >
            <title>{{ description }}</title>
          </circle>
        }
      } @empty {
        <circle class="empty" [attr.r]="radius" />
      }
    </svg>
    @if (slices().length === 0) {
      <p class="empty-hint">{{ emptyHint() }}</p>
    }
    @let s = shown();
    @if (s.level === 'rule') {
      <ul class="legend">
        @for (breakdown of breakdowns(); track breakdown.category) {
          <li
            class="legend-item"
            [class.missed]="!breakdown.targetMet"
            [class.highlighted]="highlighted() === breakdown.category"
          >
            <span class="swatch" [class]="breakdown.category"></span>
            <div class="legend-text">
              <div class="legend-header">
                <span class="name">{{ label(breakdown.category) }}</span>
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
    } @else {
      @let breakdown = breakdownOf(s.category);
      <div
        class="selection-summary"
        [class.missed]="s.level === 'category' && !breakdown?.targetMet"
      >
        <div class="legend-header">
          <span class="name">{{ shownLabel() }}</span>
          <span class="total">{{ shownMonthlyTotal() | currency: 'EUR' }}</span>
        </div>
        @if (s.level === 'category' && breakdown) {
          <span class="ratio">
            {{ breakdown.actualPercentageOfIncome | number: '1.0-2' }} % des Einkommens von
            {{ target(breakdown) }} %
            @if (!breakdown.targetMet) {
              · {{ missed(breakdown) }}
            }
          </span>
        }
      </div>
      <ul class="legend">
        @for (entry of entries(); track entry.key) {
          <li class="legend-item" [class.highlighted]="highlighted() === entry.key">
            <span class="swatch" [style.background-color]="entry.fill"></span>
            <div class="legend-text">
              <div class="legend-header">
                <span class="name">{{ entry.label }}</span>
                <span class="total">
                  {{ entry.monthly | currency: 'EUR' }} · {{ entry.percentage | number: '1.0-2' }} %
                </span>
              </div>
            </div>
          </li>
        }
      </ul>
    }
  `,
  styleUrl: './category-pie.scss',
})
export class CategoryPieChart {
  readonly breakdowns = input.required<CategoryBreakdown[]>();
  /** The expenses grouped by category and subcategory, as the entries show them (UK-10, UK-11). */
  readonly expenseCategories = input<CategoryItems[]>([]);
  /** Selected part of the plan; the parent keeps it, so it survives switching the year (TK-5). */
  readonly selection = model<PieSelection>(RULE_SELECTION);

  protected readonly radius = RADIUS;

  /** The selection as shown: a group that no longer has items falls back to its category (TK-5). */
  protected readonly shown = computed<PieSelection>(() => {
    const selection = this.selection();
    if (selection.level === 'group' && !this.groupOf(selection.category, selection.subcategory)) {
      return { level: 'category', category: selection.category };
    }
    return selection;
  });

  /** The rule, each category and below it its groups in the order of the entries (TK-2). */
  protected readonly options = computed<SelectionOption[]>(() => {
    const options: SelectionOption[] = [option(RULE_SELECTION, '50/30/20-Regel', false)];
    for (const breakdown of this.breakdowns()) {
      const category = breakdown.category;
      options.push(option({ level: 'category', category }, categoryLabel(category), false));
      for (const group of this.categoryItemsOf(category)?.groups ?? []) {
        options.push(
          option(
            { level: 'group', category, subcategory: group.subcategory },
            group.subcategory,
            true,
          ),
        );
      }
    }
    return options;
  });

  protected readonly shownKey = computed(() => selectionKey(this.shown()));

  protected readonly shownLabel = computed(() => selectionLabel(this.shown()));

  protected readonly shownMonthlyTotal = computed(() => {
    const shown = this.shown();
    if (shown.level === 'group') {
      return this.groupOf(shown.category, shown.subcategory)?.monthlyTotal ?? '0.00';
    }
    if (shown.level === 'category') {
      return this.breakdownOf(shown.category)?.monthlyTotal ?? '0.00';
    }
    return '0.00';
  });

  protected readonly entries = computed<PieEntry[]>(() => {
    const shown = this.shown();
    switch (shown.level) {
      case 'rule':
        return this.ruleEntries();
      case 'category':
        return this.categoryEntries(shown.category);
      case 'group':
        return this.groupEntries(shown.category, shown.subcategory);
    }
  });

  /** Slices of the entries with an amount above zero, clockwise from twelve o'clock (TD-6, TK-6). */
  protected readonly slices = computed<Slice[]>(() => {
    const filled = this.entries().filter((e) => e.size > 0);
    const sum = filled.reduce((acc, e) => acc + e.size, 0);
    let start = 0;
    return filled.map((entry) => {
      const fraction = entry.size / sum;
      const end = start + fraction;
      const path = fraction >= 1 ? null : arc(start, end);
      start = end;
      return { entry, path };
    });
  });

  /**
   * Key of the slice under the mouse or with the focus; its line in the legend lights up (TK-14).
   * Choosing another selection clears it, so a tapped slice does not stay lit on touch screens.
   */
  protected readonly highlighted = linkedSignal<string, string | null>({
    source: this.shownKey,
    computation: () => null,
  });

  protected readonly emptyHint = computed(() => {
    const shown = this.shown();
    return shown.level === 'rule'
      ? 'Noch keine Ausgaben oder Einnahmen'
      : `Keine Ausgaben in ${selectionLabel(shown)}`;
  });

  protected selectKey(key: string): void {
    const option = this.options().find((o) => o.key === key);
    if (option) {
      this.selection.set(option.selection);
    }
  }

  /** One level up: from a group to its category, from a category to the rule (TK-4). */
  protected back(): void {
    const shown = this.shown();
    this.selection.set(
      shown.level === 'group' ? { level: 'category', category: shown.category } : RULE_SELECTION,
    );
  }

  /** Selects the next level down, if the slice leads to one (TK-4). */
  protected open(entry: PieEntry): void {
    if (entry.target) {
      this.selection.set(entry.target);
    }
  }

  protected highlight(entry: PieEntry): void {
    this.highlighted.set(entry.key);
  }

  protected unhighlight(entry: PieEntry): void {
    if (this.highlighted() === entry.key) {
      this.highlighted.set(null);
    }
  }

  protected targetLabel(selection: PieSelection): string {
    return selectionLabel(selection);
  }

  protected label(category: ExpenseCategory): string {
    return categoryLabel(category);
  }

  protected breakdownOf(category: ExpenseCategory): CategoryBreakdown | undefined {
    return this.breakdowns().find((b) => b.category === category);
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

  /** One slice per category, sized by its total including the surplus (TD-3). */
  private ruleEntries(): PieEntry[] {
    return this.breakdowns().map((b) => ({
      key: b.category,
      label: categoryLabel(b.category),
      monthly: b.monthlyTotal,
      size: Number(b.total),
      percentage: b.actualPercentageOfIncome,
      percentUnit: '% des Einkommens',
      cssClass: b.category,
      fill: null,
      target: { level: 'category', category: b.category },
    }));
  }

  /** One slice per group, and one for the unplanned income of savings (TK-6, TK-9). */
  private categoryEntries(category: ExpenseCategory): PieEntry[] {
    const groups = this.categoryItemsOf(category)?.groups ?? [];
    const fills = shades(CATEGORY_COLORS[category], groups.length);
    const entries = groups.map<PieEntry>((group, index) =>
      entry(
        `group:${group.subcategory}`,
        group.subcategory,
        group.monthlyTotal,
        group.percentageOfSection,
        fills[index],
        { level: 'group', category, subcategory: group.subcategory },
      ),
    );
    const breakdown = this.breakdownOf(category);
    if (breakdown && Number(breakdown.monthlyUnplanned) > 0) {
      entries.push(
        entry(
          'unplanned',
          'nicht verplant',
          breakdown.monthlyUnplanned,
          breakdown.unplannedPercentageOfTotal,
          UNPLANNED_FILL,
          null,
        ),
      );
    }
    return entries;
  }

  /** One slice per item of the group, sized by its amount per month (TK-6). */
  private groupEntries(category: ExpenseCategory, subcategory: string): PieEntry[] {
    const items = this.groupOf(category, subcategory)?.items ?? [];
    const fills = shades(CATEGORY_COLORS[category], items.length);
    return items.map((item, index) =>
      entry(
        `item:${item.id}`,
        item.description,
        item.monthlyAmount,
        item.percentageOfGroup ?? 0,
        fills[index],
        null,
      ),
    );
  }

  private categoryItemsOf(category: ExpenseCategory): CategoryItems | undefined {
    return this.expenseCategories().find((c) => c.category === category);
  }

  private groupOf(category: ExpenseCategory, subcategory: string): ItemGroup | undefined {
    return this.categoryItemsOf(category)?.groups.find((g) =>
      isSameSubcategory(g.subcategory, subcategory),
    );
  }
}

function entry(
  key: string,
  label: string,
  monthly: string,
  percentage: number,
  fill: string,
  target: PieSelection | null,
): PieEntry {
  return {
    key,
    label,
    monthly,
    size: Number(monthly),
    percentage,
    percentUnit: '%',
    cssClass: null,
    fill,
    target,
  };
}

function option(selection: PieSelection, label: string, nested: boolean): SelectionOption {
  return {
    key: selectionKey(selection),
    label,
    fullLabel: selectionLabel(selection),
    selection,
    nested,
  };
}

function selectionKey(selection: PieSelection): string {
  switch (selection.level) {
    case 'rule':
      return 'rule';
    case 'category':
      return selection.category;
    case 'group':
      return `${selection.category}/${selection.subcategory.toLocaleLowerCase('de')}`;
  }
}

function selectionLabel(selection: PieSelection): string {
  switch (selection.level) {
    case 'rule':
      return '50/30/20-Regel';
    case 'category':
      return categoryLabel(selection.category);
    case 'group':
      return `${categoryLabel(selection.category)} › ${selection.subcategory}`;
  }
}

/** Shades of a color from full strength down to the lightest, mixed into the surface (TK-10). */
function shades(color: string, count: number): string[] {
  return Array.from({ length: count }, (_, index) => {
    const strength =
      count <= 1 ? 100 : Math.round(100 - ((100 - LIGHTEST_SHADE) * index) / (count - 1));
    return strength === 100
      ? color
      : `color-mix(in srgb, ${color} ${strength}%, var(--mat-sys-surface))`;
  });
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
