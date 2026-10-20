import { Component, computed, input } from '@angular/core';
import { ChangeKind, changeDescription, changeLabel, changeTone } from '../year-change';

/** The change of a figure from the previous year, coloured by what it means for the user (JU-7, JU-8). */
@Component({
  selector: 'app-year-change',
  template: `<span
    [class]="tone()"
    [attr.title]="description()"
    [attr.aria-label]="description()"
    >{{ label() }}</span
  >`,
  styles: `
    :host {
      display: block;
      font: var(--mat-sys-body-small);
    }
    .neutral {
      color: var(--mat-sys-on-surface-variant);
    }
    .good {
      color: var(--app-income-color);
    }
    .bad {
      color: var(--mat-sys-error);
    }
  `,
})
export class YearChange {
  readonly kind = input.required<ChangeKind>();
  /** In percent; `null` without a change (JU-6). */
  readonly change = input.required<number | null>();

  protected readonly label = computed(() => changeLabel(this.change()));
  protected readonly tone = computed(() => changeTone(this.kind(), this.change()));
  protected readonly description = computed(() => changeDescription(this.change()));
}
