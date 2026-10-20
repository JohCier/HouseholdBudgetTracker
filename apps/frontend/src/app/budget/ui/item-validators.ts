import { AbstractControl, ValidationErrors } from '@angular/forms';

/** The longest note an item may have (PN-2). */
export const NOTE_MAX_LENGTH = 1000;

/** The note to send for the text typed in: none if it is blank (PN-3). */
export function noteOf(text: string): { note?: string } {
  const note = text.trim();
  return note === '' ? {} : { note };
}

/** The longest subcategory an item may have (UK-2). */
export const SUBCATEGORY_MAX_LENGTH = 50;

/** The subcategory to send for the text typed in: none if it is blank, so the item goes to "Sonstiges" (UK-1, UK-2). */
export function subcategoryOf(text: string): { subcategory?: string } {
  const subcategory = text.trim();
  return subcategory === '' ? {} : { subcategory };
}

/**
 * Amounts are stored in cents, so more than two decimal places are rejected. The sign is left to
 * `Validators.min`, so a negative amount gets the "positive amount" message (PO-4).
 */
export function maxTwoDecimals(control: AbstractControl<number | null>): ValidationErrors | null {
  const value = control.value;
  return value === null || /^-?\d+(\.\d{1,2})?$/.test(String(value)) ? null : { decimals: true };
}
