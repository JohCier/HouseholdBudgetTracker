import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Year } from '../../budget/budget.model';
import { currentYear } from '../../budget/year';
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
  templateUrl: './net-worth-card.html',
  styleUrl: './net-worth-card.scss',
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
