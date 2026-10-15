import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterOutlet } from '@angular/router';
import { AuthStore } from './auth/data-access/auth-store';

@Component({
  selector: 'app-root',
  imports: [MatButtonModule, MatToolbarModule, RouterOutlet],
  template: `
    <mat-toolbar>
      <span>Haushaltsbuch</span>
      @if (auth.user(); as user) {
        <span class="user">{{ user.email }}</span>
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
      overflow: hidden;
      text-overflow: ellipsis;
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
