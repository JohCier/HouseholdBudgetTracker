import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Year } from '../../shared/year';
import { currentYear } from '../../shared/year';
import { toMessage } from '../../shared/http-error';
import { NetWorthApi } from '../data-access/net-worth-api';
import {
  changeAmountLabel,
  changeDescription,
  changeFullLabel,
  changePercentageLabel,
  changeTone,
} from '../net-worth-change';
import { NetWorthHistoryEntry } from '../net-worth.model';
import { copiedNetWorthMessage, openCopyNetWorthDialog } from './copy-net-worth-dialog';
import { NetWorthChart } from './net-worth-chart';

/**
 * The card "Vermögen" of the yearly overview (VS-18 to VS-20): the net worth of the newest year with
 * its change, the chart of its course and a table of all years with entries, the newest first. A row
 * opens the page "Vermögen" of its year. Always shows the stock at the end of the year, whatever
 * the period chosen for the plans (VS-19). Loads its figures itself, as they have nothing to do with
 * the plans (VS-4).
 */
@Component({
  selector: 'app-net-worth-card',
  imports: [CurrencyPipe, MatButtonModule, MatCardModule, MatIconModule, NetWorthChart, RouterLink],
  template: `
    <mat-card appearance="outlined" class="card">
      <div class="head">
        <div>
          <h2>Vermögen</h2>
          <p class="subtitle">Nettovermögen (Vermögen − Schulden) am Jahresende</p>
        </div>
        @if (latest(); as entry) {
          <div class="hero">
            <span class="value" [class.overindebted]="entry.overindebted">
              {{ entry.netWorth | currency: 'EUR' }}
            </span>
            @if (entry.change; as change) {
              <span [class]="tone(entry)">{{ full(entry) }} zum Vorjahr</span>
            }
          </div>
        }
      </div>

      @if (error(); as message) {
        <p class="load-error" role="alert">Vermögen konnte nicht geladen werden: {{ message }}</p>
      } @else if (years(); as years) {
        @if (years.length === 0) {
          <div class="empty">
            <p>Noch kein Vermögen erfasst.</p>
            <a matButton="filled" [routerLink]="['/vermoegen', currentYear]">Vermögen erfassen</a>
          </div>
        } @else {
          <app-net-worth-chart class="chart" [years]="years" />
          <div class="table">
            <table>
              <thead>
                <tr>
                  <th scope="col">Jahr</th>
                  <th scope="col" class="amount wide">Vermögen</th>
                  <th scope="col" class="amount wide">Schulden</th>
                  <th scope="col" class="amount">Nettovermögen</th>
                  <th scope="col" class="amount">Veränderung</th>
                  <th scope="col" class="actions"><span class="visually-hidden">Aktionen</span></th>
                </tr>
              </thead>
              <tbody>
                @for (entry of years; track entry.year) {
                  <tr (click)="open(entry.year)" [attr.data-year]="entry.year">
                    <th scope="row">
                      <a
                        [routerLink]="['/vermoegen', entry.year]"
                        (click)="$event.stopPropagation()"
                        >{{ entry.year }}</a
                      >
                    </th>
                    <td class="amount wide">{{ entry.totalAssets | currency: 'EUR' }}</td>
                    <td class="amount wide">{{ entry.totalLiabilities | currency: 'EUR' }}</td>
                    <td class="amount">
                      <span class="value" [class.overindebted]="entry.overindebted">
                        {{ entry.netWorth | currency: 'EUR' }}
                      </span>
                      @if (entry.overindebted) {
                        <span class="small overindebted">Überschuldet</span>
                      }
                      <span class="small narrow">
                        {{ entry.totalAssets | currency: 'EUR' }} −
                        {{ entry.totalLiabilities | currency: 'EUR' }}
                      </span>
                    </td>
                    <td
                      class="amount change"
                      [class]="tone(entry)"
                      [attr.aria-label]="description(entry)"
                    >
                      @if (entry.change) {
                        <span class="value">{{ amount(entry) }}</span>
                        <span class="small">{{ percentage(entry) }}</span>
                      } @else {
                        –
                      }
                    </td>
                    <td class="actions">
                      <button
                        matIconButton
                        type="button"
                        [attr.aria-label]="'Vermögen ' + entry.year + ' übernehmen'"
                        title="Vermögen übernehmen"
                        (click)="$event.stopPropagation(); copy(entry.year)"
                      >
                        <mat-icon svgIcon="content_copy" />
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      }
    </mat-card>
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
    }
    .head {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      padding: 16px 16px 8px;
    }
    h2 {
      margin: 0;
      font: var(--mat-sys-title-large);
    }
    .subtitle {
      margin: 4px 0 0;
      color: var(--mat-sys-on-surface-variant);
    }
    .hero {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
    }
    .hero .value {
      font: var(--mat-sys-headline-medium);
      font-variant-numeric: tabular-nums;
    }
    .chart {
      padding: 0 16px;
    }
    .empty {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
      padding: 8px 16px 16px;
    }
    .empty p,
    .load-error {
      margin: 0;
    }
    .load-error {
      padding: 8px 16px 16px;
      color: var(--mat-sys-error);
    }
    .table {
      overflow-x: auto;
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th,
    td {
      padding: 12px 16px;
      text-align: left;
      vertical-align: top;
    }
    thead th {
      font: var(--mat-sys-label-large);
      color: var(--mat-sys-on-surface-variant);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    tbody tr {
      cursor: pointer;
    }
    tbody tr:not(:last-child) {
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    tbody tr:hover {
      background: var(--mat-sys-surface-container-high);
    }
    tbody th a {
      font: var(--mat-sys-title-medium);
      color: var(--mat-sys-primary);
      text-decoration: none;
    }
    .amount {
      text-align: right;
      font-variant-numeric: tabular-nums;
    }
    .value,
    .small {
      display: block;
    }
    .small {
      font: var(--mat-sys-body-small);
    }
    .narrow {
      display: none;
      color: var(--mat-sys-on-surface-variant);
    }
    .actions {
      width: 56px;
      padding: 4px 8px;
      vertical-align: middle;
    }
    .good {
      color: var(--app-income-color);
    }
    .bad,
    .overindebted {
      color: var(--mat-sys-error);
    }
    .neutral {
      color: var(--mat-sys-on-surface-variant);
    }
    .visually-hidden {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
    /* Vermögen and Schulden move under the net worth, so the table fits on a phone (VS-24) */
    @media (max-width: 600px) {
      .hero {
        align-items: flex-start;
      }
      th,
      td {
        padding: 10px 8px;
      }
      .wide,
      .actions {
        display: none;
      }
      .narrow {
        display: block;
      }
    }
  `,
})
export class NetWorthCard implements OnInit {
  private readonly api = inject(NetWorthApi);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly currentYear = currentYear();
  /** `undefined` until loaded, newest first. */
  protected readonly years = signal<NetWorthHistoryEntry[] | undefined>(undefined);
  protected readonly error = signal<string | null>(null);
  protected readonly latest = computed(() => this.years()?.[0] ?? null);

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    try {
      this.years.set((await firstValueFrom(this.api.getHistory())).years);
      this.error.set(null);
    } catch (error) {
      this.error.set(toMessage(error));
    }
  }

  protected amount(entry: NetWorthHistoryEntry): string {
    return entry.change ? changeAmountLabel(entry.change) : '–';
  }

  protected percentage(entry: NetWorthHistoryEntry): string {
    return entry.change ? changePercentageLabel(entry.change) : '–';
  }

  protected full(entry: NetWorthHistoryEntry): string {
    return entry.change ? changeFullLabel(entry.change) : '';
  }

  protected tone(entry: NetWorthHistoryEntry): string {
    return changeTone(entry.change ?? null);
  }

  protected description(entry: NetWorthHistoryEntry): string {
    return changeDescription(entry.change ?? null);
  }

  protected open(year: Year): Promise<boolean> {
    return this.router.navigate(['/vermoegen', year]);
  }

  /** Takes over the year into another one and opens that one (VS-17). */
  protected copy(sourceYear: Year): void {
    openCopyNetWorthDialog(this.dialog, { sourceYear }).subscribe(async (copied) => {
      if (copied) {
        await this.open(copied.targetYear);
        this.snackBar.open(copiedNetWorthMessage(copied), undefined, { duration: 3000 });
      }
    });
  }
}
