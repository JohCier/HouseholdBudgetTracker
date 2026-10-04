import { Month } from './budget.model';

/** Today's date in the browser's time zone as ISO string, e.g. "2026-10-04". */
export function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export function monthOf(isoDate: string): Month {
  return isoDate.slice(0, 7);
}

export function currentMonth(): Month {
  return monthOf(today());
}

export function addMonths(month: Month, delta: number): Month {
  const [year, monthIndex] = month.split('-').map(Number);
  const date = new Date(year, monthIndex - 1 + delta, 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

/** First day of the month, usable with Angular's DatePipe. */
export function monthStart(month: Month): Date {
  const [year, monthIndex] = month.split('-').map(Number);
  return new Date(year, monthIndex - 1, 1);
}
