import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '../auth.model';
import { AuthStore } from '../data-access/auth-store';
import { passwordsMatch } from './passwords-match';

/** Lets the signed-in user replace their password, proving it is them with the current one (PA-1, PA-8). */
@Component({
  selector: 'app-change-password-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <h1 matCardTitle>Passwort ändern</h1>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="submit()">
          @if (error(); as error) {
            <p class="error" role="alert">{{ error }}</p>
          }

          <mat-form-field>
            <mat-label>Aktuelles Passwort</mat-label>
            <input
              matInput
              type="password"
              formControlName="currentPassword"
              autocomplete="current-password"
            />
            @if (form.controls.currentPassword.invalid) {
              <mat-error>Bitte das aktuelle Passwort angeben.</mat-error>
            }
          </mat-form-field>

          <mat-form-field>
            <mat-label>Neues Passwort</mat-label>
            <input
              matInput
              type="password"
              formControlName="password"
              autocomplete="new-password"
              [attr.maxlength]="maxLength"
            />
            <mat-hint>Mindestens {{ minLength }} Zeichen</mat-hint>
            @if (form.controls.password.invalid) {
              <mat-error>Mindestens {{ minLength }} Zeichen.</mat-error>
            }
          </mat-form-field>

          <mat-form-field>
            <mat-label>Neues Passwort wiederholen</mat-label>
            <input
              matInput
              type="password"
              formControlName="passwordRepeat"
              autocomplete="new-password"
            />
            @if (form.controls.passwordRepeat.invalid) {
              <mat-error>Bitte das neue Passwort wiederholen.</mat-error>
            }
          </mat-form-field>
          @if (form.hasError('passwordMismatch') && form.controls.passwordRepeat.touched) {
            <p class="error" role="alert">Die Passwörter stimmen nicht überein.</p>
          }

          <div class="actions">
            <a matButton routerLink="/">Abbrechen</a>
            <button matButton="filled" type="submit" [disabled]="submitting()">
              Passwort ändern
            </button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './auth-card.scss',
  styles: `
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
  `,
})
export class ChangePasswordPage {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly minLength = PASSWORD_MIN_LENGTH;
  protected readonly maxLength = PASSWORD_MAX_LENGTH;
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group(
    {
      currentPassword: this.fb.control('', Validators.required),
      password: this.fb.control('', [
        Validators.required,
        Validators.minLength(PASSWORD_MIN_LENGTH),
        Validators.maxLength(PASSWORD_MAX_LENGTH),
      ]),
      passwordRepeat: this.fb.control('', Validators.required),
    },
    { validators: passwordsMatch },
  );

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, password } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.auth.changePassword({ currentPassword, newPassword: password });
      this.snackBar.open('Passwort geändert.', undefined, { duration: 4000 });
      await this.router.navigateByUrl('/');
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.submitting.set(false);
    }
  }
}
