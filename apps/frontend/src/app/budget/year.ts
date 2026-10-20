import { Year } from './budget.model';

/** The first and last year a household plan can exist for (HP-1). */
export const FIRST_YEAR: Year = 1900;
export const LAST_YEAR: Year = 2200;

/** The current year in the browser's time zone, limited to the years a plan can exist for. */
export function currentYear(): Year {
  return Math.min(Math.max(new Date().getFullYear(), FIRST_YEAR), LAST_YEAR);
}

/** The year in an address, e.g. "2026"; `null` if it is no whole number a plan can exist for (HP-1). */
export function parseYear(value: string | undefined): Year | null {
  if (value === undefined || !/^\d{4}$/.test(value)) {
    return null;
  }
  const year = Number(value);
  return year >= FIRST_YEAR && year <= LAST_YEAR ? year : null;
}
