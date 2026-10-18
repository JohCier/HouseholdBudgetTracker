import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Amounts are stored in cents, so more than two decimal places are rejected. */
export function maxTwoDecimals(control: AbstractControl<number | null>): ValidationErrors | null {
  const value = control.value;
  return value === null || /^\d+(\.\d{1,2})?$/.test(String(value)) ? null : { decimals: true };
}
