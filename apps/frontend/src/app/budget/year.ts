import { Year } from './budget.model';

/** The current year in the browser's time zone. */
export function currentYear(): Year {
  return new Date().getFullYear();
}
