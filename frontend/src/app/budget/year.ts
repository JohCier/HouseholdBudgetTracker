import { Year } from './budget.model';

/** Today's date in the browser's time zone as ISO string, e.g. "2026-10-04". */
export function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export function yearOf(isoDate: string): Year {
  return Number(isoDate.slice(0, 4));
}

export function currentYear(): Year {
  return yearOf(today());
}
