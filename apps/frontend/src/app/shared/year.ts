/** A calendar year, e.g. 2026. */
export type Year = number;

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

/** A day as the API sends it, e.g. "2026-03-15", in the browser's time zone. */
export function isoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** The day a new one-time item starts with: today if it lies in the year shown, otherwise 1 January of it (EP-11). */
export function defaultDateIn(year: Year, today: Date = new Date()): string {
  return today.getFullYear() === year ? isoDate(today) : `${year}-01-01`;
}
