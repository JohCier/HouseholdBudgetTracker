import { AbstractControl, ValidationErrors } from '@angular/forms';

/** The repeated password has to match the password (form controls `password` and `passwordRepeat`). */
export function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const { password, passwordRepeat } = group.value as { password: string; passwordRepeat: string };
  return password === passwordRepeat ? null : { passwordMismatch: true };
}
