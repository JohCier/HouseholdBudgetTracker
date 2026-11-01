import { CurrencyPipe } from '@angular/common';
import { Component, Injector, effect, inject, input, signal, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Year } from '../budget/budget.model';
import { parseYear } from '../budget/year';
import { NetWorthApi } from './data-access/net-worth-api';
import { NetWorthStore } from './data-access/net-worth-store';
import {
  changeAmountLabel,
  changeDescription,
  changePercentageLabel,
  changeTone,
} from './net-worth-change';
import {
  NET_WORTH_ENTRY_TYPES,
  NetWorthEntry,
  NetWorthEntryType,
  NetWorthOverview,
} from './net-worth.model';
import {
  CopiedNetWorth,
  CopyNetWorthDialogData,
  copiedNetWorthMessage,
  openCopyNetWorthDialog,
} from './ui/copy-net-worth-dialog';
import { openNetWorthEntryDialog } from './ui/net-worth-entry-dialog';
import { openRemoveNetWorthEntryDialog } from './ui/remove-net-worth-entry-dialog';

/**
 * The page "Vermögen <Jahr>" under `/vermoegen/<Jahr>` (VS-8 to VS-11): the sums of the year with the
 * change from the year before, and the lists of assets and liabilities, in which the user records,
 * changes and removes entries. The arrows change the year, "Alle Jahre" leads back to the yearly
 * overview (VS-9). It has no link to the household plan of the year (VS-4).
 */
@Component({
  selector: 'app-net-worth-page',
  imports: [
    CurrencyPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    RouterLink,
  ],
  providers: [NetWorthStore],
  templateUrl: './net-worth-page.html',
  styleUrl: './net-worth-page.scss',
})
export class NetWorthPage {
  /** The year from the address, `/vermoegen/2026` (VS-9). */
  readonly year = input<string>();
  protected readonly store = inject(NetWorthStore);
  private readonly api = inject(NetWorthApi);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly injector = inject(Injector);

  protected readonly types = NET_WORTH_ENTRY_TYPES;
  /** The other years with assets or liabilities, looked up for a year without any (VS-10). */
  protected readonly otherYears = signal<Year[]>([]);

  constructor() {
    // An address without a valid year leads back to the yearly overview (VS-9)
    effect(() => {
      const year = parseYear(this.year());
      untracked(() => {
        if (year === null) {
          void this.router.navigateByUrl('/');
        } else {
          void this.store.selectYear(year);
        }
      });
    });
    effect(() => {
      if (this.store.empty()) {
        untracked(() => void this.lookUpOtherYears(this.store.year()));
      } else {
        this.otherYears.set([]);
      }
    });
  }

  protected entriesOf(overview: NetWorthOverview, type: NetWorthEntryType): NetWorthEntry[] {
    return type === 'ASSET' ? overview.assets : overview.liabilities;
  }

  protected sumOf(overview: NetWorthOverview, type: NetWorthEntryType): string {
    return type === 'ASSET' ? overview.totalAssets : overview.totalLiabilities;
  }

  protected amountLabel(overview: NetWorthOverview): string {
    return overview.change ? changeAmountLabel(overview.change) : '–';
  }

  protected percentageLabel(overview: NetWorthOverview): string {
    return overview.change ? changePercentageLabel(overview.change) : '–';
  }

  protected tone(overview: NetWorthOverview): string {
    return changeTone(overview.change ?? null);
  }

  protected changeText(overview: NetWorthOverview): string {
    return changeDescription(overview.change ?? null);
  }

  protected showYear(year: Year): Promise<boolean> {
    return this.router.navigate(['/vermoegen', year]);
  }

  /** Records an entry in the list it was started from (VS-11). */
  protected add(type: NetWorthEntryType): void {
    openNetWorthEntryDialog(this.dialog, this.injector, { type }).subscribe();
  }

  protected edit(entry: NetWorthEntry): void {
    openNetWorthEntryDialog(this.dialog, this.injector, { entry }).subscribe();
  }

  /** Removes an entry after asking (VS-5). */
  protected remove(entry: NetWorthEntry): void {
    openRemoveNetWorthEntryDialog(this.dialog, entry).subscribe(async (confirmed) => {
      if (!confirmed) {
        return;
      }
      try {
        await this.store.removeEntry(entry.id);
      } catch (error) {
        this.snackBar.open((error as Error).message, 'OK', { duration: 6000 });
      }
    });
  }

  /** Takes over the year shown into another one, which then shows (VS-15, VS-17). */
  protected copy(): void {
    this.openCopyDialog({ sourceYear: this.store.year() });
  }

  /** Takes over another year into the year shown, which has no entries yet (VS-10). */
  protected copyHere(): void {
    this.openCopyDialog({ targetYear: this.store.year(), sourceYears: this.otherYears() });
  }

  private openCopyDialog(data: CopyNetWorthDialogData): void {
    openCopyNetWorthDialog(this.dialog, data).subscribe((copied) => {
      if (copied) {
        void this.showCopied(copied);
      }
    });
  }

  private async showCopied(copied: CopiedNetWorth): Promise<void> {
    if (copied.targetYear === this.store.year()) {
      await this.store.load();
    } else {
      await this.showYear(copied.targetYear);
    }
    this.snackBar.open(copiedNetWorthMessage(copied), undefined, { duration: 3000 });
  }

  private async lookUpOtherYears(year: Year): Promise<void> {
    try {
      const history = await firstValueFrom(this.api.getHistory());
      if (this.store.year() === year && this.store.empty()) {
        this.otherYears.set(
          history.years.map((entry) => entry.year).filter((other) => other !== year),
        );
      }
    } catch {
      // Without the other years the user can still record entries one by one
      this.otherYears.set([]);
    }
  }
}
