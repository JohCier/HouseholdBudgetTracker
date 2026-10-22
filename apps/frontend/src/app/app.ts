import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthStore } from './auth/data-access/auth-store';

@Component({
  selector: 'app-root',
  imports: [MatButtonModule, MatIconModule, MatToolbarModule, RouterLink, RouterOutlet],
  template: `
    <mat-toolbar>
      <span>Haushaltsbuch</span>
      @if (auth.user(); as user) {
        <span class="user">{{ user.email }}</span>
        <a matButton routerLink="/passwort" aria-label="Passwort ändern">
          <mat-icon svgIcon="key" />
          <span class="label">Passwort ändern</span>
        </a>
        <button matButton type="button" (click)="logout()">Abmelden</button>
      }
    </mat-toolbar>
    <main>
      <router-outlet />
    </main>
  `,
  styles: `
    mat-toolbar {
      gap: 8px;
      background: var(--mat-sys-surface-container);
    }
    .user {
      margin-left: auto;
      font: var(--mat-sys-body-medium);
      color: var(--mat-sys-on-surface-variant);
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    mat-toolbar a,
    mat-toolbar button {
      flex-shrink: 0;
    }
    /* Only the key icon on narrow screens, so the toolbar fits without scrolling (PA-8). */
    @media (max-width: 600px) {
      .label {
        display: none;
      }
      mat-toolbar {
        gap: 4px;
      }
      mat-toolbar a {
        min-width: 40px;
        padding-inline: 8px;
      }
      mat-toolbar a mat-icon {
        margin: 0;
      }
    }
    main {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px 16px;
    }
  `,
})
export class App {
  protected readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    try {
      await this.auth.logout();
    } finally {
      await this.router.navigateByUrl('/login');
    }
  }
}
