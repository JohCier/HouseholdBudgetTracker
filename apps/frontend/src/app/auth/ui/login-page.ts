import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../data-access/auth-store';

@Component({
  selector: 'app-login-page',
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
        <h1 matCardTitle>Anmelden</h1>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="submit()">
          @if (error(); as error) {
            <p class="error" role="alert">{{ error }}</p>
          }

          <mat-form-field>
            <mat-label>E-Mail-Adresse</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="username" />
            @if (form.controls.email.invalid) {
              <mat-error>Bitte eine gültige E-Mail-Adresse angeben.</mat-error>
            }
          </mat-form-field>

          <mat-form-field>
            <mat-label>Passwort</mat-label>
            <input
              matInput
              type="password"
              formControlName="password"
              autocomplete="current-password"
            />
            @if (form.controls.password.invalid) {
              <mat-error>Bitte das Passwort angeben.</mat-error>
            }
          </mat-form-field>

          <button matButton="filled" type="submit" [disabled]="submitting()">Anmelden</button>
        </form>
        <p class="switch">Noch kein Konto? <a routerLink="/register">Jetzt registrieren</a></p>
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './auth-card.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    email: this.fb.control('', [Validators.required, Validators.email]),
    password: this.fb.control('', Validators.required),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.auth.login(this.form.getRawValue());
      await this.router.navigateByUrl('/');
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.submitting.set(false);
    }
  }
}
