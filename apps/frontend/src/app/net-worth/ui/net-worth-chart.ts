import {
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { Year } from '../../shared/year';
import { changeFullLabel, euro } from '../net-worth-change';
import { NetWorthHistoryEntry } from '../net-worth.model';

/** Below this width the chart gets compact labels and a minimum width per year (VS-24). */
const COMPACT_WIDTH = 600;
/** The widest the figures of a year get; longer lines wrap. */
const TOOLTIP_WIDTH = 240;
/** On a phone at most this many years show at once; older ones are a swipe away (VS-24). */
const MAX_COMPACT_YEARS = 8;

interface Column {
  year: Year;
  x: number;
  entry: NetWorthHistoryEntry | null;
}

interface Bar {
  year: Year;
  path: string;
  gain: boolean;
}

/**
 * The course of the net worth over the years (VS-20 to VS-24), drawn as SVG: a line of the net worth
 * with a point per year, a bar per year for its growth (green, upwards) or loss (red, downwards) from
 * the year before, and an axis in euros with a zero line. A year without entries keeps its place on
 * the axis, but interrupts the line and has neither point nor bar (VS-21). Pointing at or tapping a
 * year shows its figures (VS-22). The table of the card holds all figures as text, so colour is
 * never the only information.
 */
@Component({
  selector: 'app-net-worth-chart',
  template: `
    <div class="legend" aria-hidden="true">
      <span><i class="swatch-line"></i>Nettovermögen</span>
      <span><i class="swatch-bar gain"></i>Wachstum zum Vorjahr</span>
      <span><i class="swatch-bar loss"></i>Verlust zum Vorjahr</span>
    </div>
    <div class="scroll" #scroll>
      <div class="plot" [style.width.px]="layout().width">
        <svg
          [attr.viewBox]="'0 0 ' + layout().width + ' ' + layout().height"
          [attr.width]="layout().width"
          [attr.height]="layout().height"
          role="img"
          [attr.aria-label]="description()"
        >
          @for (tick of layout().ticks; track tick.value) {
            <line
              [class]="tick.value === 0 ? 'zero' : 'grid'"
              [attr.x1]="layout().left"
              [attr.x2]="layout().width - layout().right"
              [attr.y1]="tick.y"
              [attr.y2]="tick.y"
            />
            <text class="axis" [attr.x]="layout().left - 8" [attr.y]="tick.y + 4" text-anchor="end">
              {{ tick.label }}
            </text>
          }
          @if (selected(); as year) {
            @if (columnOf(year); as column) {
              <rect
                class="highlight"
                [attr.x]="column.x - layout().step / 2"
                [attr.y]="layout().top"
                [attr.width]="layout().step"
                [attr.height]="layout().height - layout().top - layout().bottom"
                rx="4"
              />
            }
          }
          @for (bar of layout().bars; track bar.year) {
            <path [class]="bar.gain ? 'bar gain' : 'bar loss'" [attr.d]="bar.path" />
          }
          @for (line of layout().lines; track $index) {
            <polyline class="line" [attr.points]="line" />
          }
          @for (column of layout().columns; track column.year) {
            @if (column.entry; as entry) {
              <circle
                class="point"
                [attr.cx]="column.x"
                [attr.cy]="layout().y(entry.netWorth)"
                [attr.r]="selected() === column.year ? 6 : 4.5"
              />
            }
            <text
              class="axis"
              [attr.x]="column.x"
              [attr.y]="layout().height - 8"
              text-anchor="middle"
            >
              {{
                layout().compact
                  ? '’' + (column.year % 100).toString().padStart(2, '0')
                  : column.year
              }}
            </text>
          }
          @if (latest(); as entry) {
            <text
              class="label"
              [attr.x]="columnOf(entry.year)?.x"
              [attr.y]="layout().y(entry.netWorth) - 12"
              text-anchor="middle"
            >
              {{ short(entry.netWorth) }}
            </text>
          }
          @for (column of layout().columns; track column.year) {
            @if (column.entry; as entry) {
              <rect
                class="hit"
                tabindex="0"
                [attr.x]="column.x - layout().step / 2"
                [attr.y]="layout().top"
                [attr.width]="layout().step"
                [attr.height]="layout().height - layout().top - layout().bottom"
                [attr.aria-label]="tooltipText(entry)"
                [attr.data-year]="column.year"
                (pointerenter)="selected.set(column.year)"
                (pointerleave)="selected.set(null)"
                (focus)="selected.set(column.year)"
                (blur)="selected.set(null)"
                (click)="selected.set(column.year)"
              />
            }
          }
        </svg>
        @if (selectedEntry(); as entry) {
          <div
            class="tooltip"
            role="status"
            [style.left.px]="tooltipLeft()"
            [style.top.px]="tooltipTop()"
          >
            <b>{{ entry.year }}</b>
            <span>Nettovermögen {{ euroOf(entry.netWorth) }}</span>
            <span>{{
              entry.change ? changeOf(entry) + ' zum Vorjahr' : 'kein Vergleich mit dem Vorjahr'
            }}</span>
          </div>
        }
      </div>
    </div>
    @if (years().length === 1) {
      <p class="hint">Ab zwei Jahren zeigt das Diagramm den Verlauf.</p>
    }
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
    }
    .legend {
      display: flex;
      flex-wrap: wrap;
      gap: 6px 16px;
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
    }
    .legend span {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .swatch-line {
      position: relative;
      width: 18px;
      height: 2px;
      background: var(--mat-sys-primary);
    }
    .swatch-line::after {
      content: '';
      position: absolute;
      left: 5px;
      top: -3px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--mat-sys-primary);
    }
    .swatch-bar {
      width: 10px;
      height: 12px;
      border-radius: 2px;
    }
    .gain {
      background: var(--app-gain-color);
      fill: var(--app-gain-color);
    }
    .loss {
      background: var(--app-loss-color);
      fill: var(--app-loss-color);
    }
    /* Older years are reached by swiping within the chart, never by scrolling the page (VS-24) */
    .scroll {
      overflow-x: auto;
      overscroll-behavior-x: contain;
    }
    .plot {
      position: relative;
      min-width: 100%;
    }
    svg {
      display: block;
      overflow: visible;
    }
    text {
      font:
        12px Roboto,
        sans-serif;
      fill: var(--mat-sys-on-surface-variant);
    }
    text.label {
      font-weight: 500;
      fill: var(--mat-sys-on-surface);
    }
    .grid {
      stroke: var(--mat-sys-outline-variant);
    }
    .zero {
      stroke: var(--mat-sys-outline);
    }
    .line {
      fill: none;
      stroke: var(--mat-sys-primary);
      stroke-width: 2;
      stroke-linejoin: round;
    }
    .point {
      fill: var(--mat-sys-primary);
      stroke: var(--mat-sys-surface);
      stroke-width: 2;
    }
    .highlight {
      fill: var(--mat-sys-on-surface);
      opacity: 0.06;
    }
    .hit {
      fill: transparent;
      cursor: pointer;
      outline: none;
    }
    .hit:focus-visible {
      stroke: var(--mat-sys-primary);
      stroke-width: 2;
    }
    .tooltip {
      position: absolute;
      display: flex;
      flex-direction: column;
      gap: 2px;
      padding: 8px 12px;
      border-radius: 8px;
      background: var(--mat-sys-inverse-surface);
      color: var(--mat-sys-inverse-on-surface);
      font: var(--mat-sys-body-small);
      box-sizing: border-box;
      max-width: 240px;
      pointer-events: none;
      box-shadow: var(--mat-sys-level2);
    }
    .tooltip b {
      font: var(--mat-sys-label-large);
    }
    .hint {
      margin: 8px 0 0;
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class NetWorthChart {
  /** The years with entries, in any order, as the history lists them. */
  readonly years = input.required<readonly NetWorthHistoryEntry[]>();

  private readonly injector = inject(Injector);
  private readonly scroll = viewChild.required<ElementRef<HTMLElement>>('scroll');
  private readonly width = signal(0);
  protected readonly selected = signal<Year | null>(null);

  private readonly ascending = computed(() => [...this.years()].sort((a, b) => a.year - b.year));
  protected readonly latest = computed(() => this.ascending().at(-1) ?? null);
  protected readonly selectedEntry = computed(() => {
    const year = this.selected();
    return this.ascending().find((entry) => entry.year === year) ?? null;
  });

  protected readonly layout = computed(() => layoutOf(this.ascending(), this.width()));

  protected readonly description = computed(() => {
    const entries = this.ascending();
    if (entries.length === 0) {
      return 'Verlauf des Nettovermögens';
    }
    return `Verlauf des Nettovermögens von ${entries[0].year} bis ${entries.at(-1)!.year}`;
  });

  constructor() {
    // The width of the scroll area, which is that of the card without its padding
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      const scroll = this.scroll().nativeElement;
      this.width.set(scroll.clientWidth);
      if (typeof ResizeObserver !== 'undefined') {
        const observer = new ResizeObserver(() => this.width.set(scroll.clientWidth));
        observer.observe(scroll);
        destroyRef.onDestroy(() => observer.disconnect());
      }
    });
    // On a phone the newest years show first; older ones are a swipe away (VS-24)
    effect(() => {
      this.layout();
      const scroll = this.scroll().nativeElement;
      afterNextRender(() => (scroll.scrollLeft = scroll.scrollWidth), { injector: this.injector });
    });
  }

  protected columnOf(year: Year): Column | undefined {
    return this.layout().columns.find((column) => column.year === year);
  }

  protected short(value: string): string {
    return euro(Math.round(Number(value))).replace(/,00\s/, ' ');
  }

  protected euroOf(value: string): string {
    return euro(value);
  }

  protected changeOf(entry: NetWorthHistoryEntry): string {
    return entry.change ? changeFullLabel(entry.change) : '';
  }

  protected tooltipText(entry: NetWorthHistoryEntry): string {
    const change = entry.change ? ` · ${changeFullLabel(entry.change)}` : '';
    return `${entry.year}: ${euro(entry.netWorth)}${change}`;
  }

  /** Left of the point if there is room, otherwise right of it, but always within the chart. */
  protected tooltipLeft(): number {
    const entry = this.selectedEntry();
    const column = entry ? this.columnOf(entry.year) : undefined;
    if (!column) {
      return 0;
    }
    const left = column.x - TOOLTIP_WIDTH - 12 >= 0 ? column.x - TOOLTIP_WIDTH - 12 : column.x + 12;
    return Math.max(0, Math.min(left, this.layout().width - TOOLTIP_WIDTH));
  }

  protected tooltipTop(): number {
    const entry = this.selectedEntry();
    return entry ? Math.max(0, this.layout().y(entry.netWorth) - 88) : 0;
  }
}

/** Where everything goes for the given years and the width available. */
function layoutOf(entries: NetWorthHistoryEntry[], available: number) {
  const compact = available > 0 && available < COMPACT_WIDTH;
  const height = compact ? 220 : 280;
  const left = compact ? 60 : 72;
  // Room for the label of the newest net worth, centred over its point
  const right = 32;
  const top = 24;
  const bottom = 28;
  const first = entries[0]?.year ?? 0;
  const count = entries.length === 0 ? 0 : entries.at(-1)!.year - first + 1;
  const plotWidth = Math.max(available - left - right, 0);
  const step = count === 0 ? 0 : plotWidth / (compact ? Math.min(count, MAX_COMPACT_YEARS) : count);
  const width = Math.max(available, left + right + step * count);

  const values = entries.flatMap((entry) => [
    Number(entry.netWorth),
    entry.change ? Number(entry.change.amount) : 0,
  ]);
  const ticks = niceTicks(Math.min(0, ...values), Math.max(0, ...values));
  const min = ticks[0];
  const max = ticks.at(-1)!;
  const y = (value: string | number) =>
    top + ((max - Number(value)) / (max - min || 1)) * (height - top - bottom);

  const byYear = new Map(entries.map((entry) => [entry.year, entry]));
  const columns: Column[] = Array.from({ length: count }, (_, index) => ({
    year: first + index,
    x: left + step * (index + 0.5),
    entry: byYear.get(first + index) ?? null,
  }));

  // The line is interrupted wherever a year has no entries (VS-21)
  const lines: string[] = [];
  let points: string[] = [];
  for (const column of columns) {
    if (column.entry) {
      points.push(`${column.x},${y(column.entry.netWorth)}`);
    } else if (points.length > 0) {
      lines.push(points.join(' '));
      points = [];
    }
  }
  if (points.length > 0) {
    lines.push(points.join(' '));
  }

  const barWidth = Math.min(compact ? 18 : 28, step * 0.4);
  const bars: Bar[] = columns.flatMap((column) => {
    const change = column.entry?.change;
    if (!change || Number(change.amount) === 0) {
      return [];
    }
    const amount = Number(change.amount);
    return [
      {
        year: column.year,
        path: barPath(column.x - barWidth / 2, barWidth, y(0), y(amount)),
        gain: amount > 0,
      },
    ];
  });

  return {
    compact,
    width,
    height,
    left,
    right,
    top,
    bottom,
    step,
    y,
    columns,
    lines,
    bars,
    ticks: ticks.map((value) => ({ value, y: y(value), label: axisLabel(value, compact) })),
  };
}

/** A bar from the zero line to the value, rounded only at its end away from the zero line. */
function barPath(x: number, width: number, zero: number, end: number): string {
  const radius = Math.min(4, Math.abs(end - zero), width / 2);
  const direction = end < zero ? 1 : -1;
  return [
    `M${x},${zero}`,
    `V${end + direction * radius}`,
    `Q${x},${end} ${x + radius},${end}`,
    `H${x + width - radius}`,
    `Q${x + width},${end} ${x + width},${end + direction * radius}`,
    `V${zero}`,
    'Z',
  ].join(' ');
}

/** Round values from at most `min` to at least `max`, about five of them, always including zero. */
export function niceTicks(min: number, max: number): number[] {
  if (min === max) {
    return max === 0 ? [0, 1000] : niceTicks(Math.min(0, min), Math.max(0, max));
  }
  const rough = (max - min) / 4;
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const step = [1, 2, 2.5, 5, 10]
    .map((factor) => factor * magnitude)
    .find((candidate) => candidate >= rough)!;
  const ticks: number[] = [];
  for (
    let value = Math.floor(min / step) * step;
    value <= Math.ceil(max / step) * step + step / 2;
    value += step
  ) {
    ticks.push(Math.round(value * 100) / 100);
  }
  return ticks;
}

/** E.g. "15.000 €", compact "15 Tsd. €". */
function axisLabel(value: number, compact: boolean): string {
  if (compact && Math.abs(value) >= 1000) {
    const thousands = new Intl.NumberFormat('de-DE', { maximumFractionDigits: 1 }).format(
      Math.abs(value) / 1000,
    );
    return `${value < 0 ? '−' : ''}${thousands} Tsd. €`;
  }
  return euro(value).replace(/,00\s/, ' ');
}
