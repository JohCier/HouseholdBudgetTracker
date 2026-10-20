import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { OTHER_SUBCATEGORY } from '../budget.model';
import { SUBCATEGORY_MAX_LENGTH } from './item-validators';

/**
 * Field for the optional subcategory of an item. It suggests the given subcategories, filtered
 * by what has been typed, and takes any other name as well (UK-6). Left empty, the item goes to
 * "Sonstiges" (UK-1).
 */
@Component({
  selector: 'app-subcategory-field',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule],
  template: `
    <mat-form-field>
      <mat-label>Unterkategorie (optional)</mat-label>
      <input
        matInput
        name="subcategory"
        [formControl]="control()"
        [matAutocomplete]="autocomplete"
        autocomplete="off"
      />
      <mat-autocomplete #autocomplete="matAutocomplete">
        @for (suggestion of filtered(); track suggestion) {
          <mat-option [value]="suggestion">{{ suggestion }}</mat-option>
        }
      </mat-autocomplete>
      @if (control().invalid) {
        <mat-error>Höchstens {{ maxLength }} Zeichen.</mat-error>
      } @else {
        <mat-hint>Leer lassen für „{{ other }}"</mat-hint>
      }
    </mat-form-field>
  `,
  styles: `
    mat-form-field {
      width: 100%;
    }
  `,
})
export class SubcategoryField implements OnInit {
  readonly control = input.required<FormControl<string>>();
  readonly suggestions = input.required<readonly string[]>();

  protected readonly maxLength = SUBCATEGORY_MAX_LENGTH;
  protected readonly other = OTHER_SUBCATEGORY;

  private readonly destroyRef = inject(DestroyRef);
  private readonly typed = signal('');

  /** The suggestions containing what has been typed, ignoring case (UK-6). */
  protected readonly filtered = computed(() => {
    const typed = this.typed().trim().toLocaleLowerCase('de');
    return this.suggestions().filter((suggestion) =>
      suggestion.toLocaleLowerCase('de').includes(typed),
    );
  });

  ngOnInit(): void {
    const control = this.control();
    this.typed.set(control.value);
    const subscription = control.valueChanges.subscribe((value) => this.typed.set(value ?? ''));
    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }
}
