import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { BudgetDashboard } from './budget/budget-dashboard';

@Component({
  selector: 'app-root',
  imports: [MatToolbarModule, BudgetDashboard],
  template: `
    <mat-toolbar>
      <span>Haushaltsbuch</span>
    </mat-toolbar>
    <main>
      <app-budget-dashboard />
    </main>
  `,
  styles: `
    mat-toolbar {
      background: var(--mat-sys-surface-container);
    }
    main {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px 16px;
    }
  `,
})
export class App {}
