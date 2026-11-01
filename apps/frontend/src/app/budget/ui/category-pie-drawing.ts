/** Order, shades and SVG paths of the slices of the category pie. */
import { PieSort } from '../budget.model';

/** Radius of the pie in SVG units. */
export const RADIUS = 80;

/** Lightest shade of a category color, in percent of the color mixed into the surface (TK-10). */
const LIGHTEST_SHADE = 40;

const NAME_COLLATOR = new Intl.Collator('de', { sensitivity: 'base' });

/**
 * The slices in the chosen order (TS-1 to TS-3); the shades follow it (TS-4). Ties keep the order
 * of the entries, since the sort is stable.
 */
export function sorted<T>(
  values: readonly T[],
  sort: PieSort,
  monthly: (value: T) => string,
  name: (value: T) => string,
): T[] {
  if (sort.key === 'entries') {
    return [...values];
  }
  const direction = sort.descending ? -1 : 1;
  const compare =
    sort.key === 'amount'
      ? (a: T, b: T) => Number(monthly(a)) - Number(monthly(b))
      : (a: T, b: T) => NAME_COLLATOR.compare(name(a), name(b));
  return [...values].sort((a, b) => direction * compare(a, b));
}

/** Shades of a color from full strength down to the lightest, mixed into the surface (TK-10). */
export function shades(color: string, count: number): string[] {
  return Array.from({ length: count }, (_, index) => {
    const strength =
      count <= 1 ? 100 : Math.round(100 - ((100 - LIGHTEST_SHADE) * index) / (count - 1));
    return strength === 100
      ? color
      : `color-mix(in srgb, ${color} ${strength}%, var(--mat-sys-surface))`;
  });
}

/** Path of a pie slice between two fractions of the full circle, starting at twelve o'clock. */
export function arc(start: number, end: number): string {
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
