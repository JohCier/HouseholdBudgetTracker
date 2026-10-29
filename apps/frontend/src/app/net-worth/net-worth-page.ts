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
  template: `
    @if (store.loading()) {
      <mat-progress-bar mode="indeterminate" class="loading" />
    }

    <a matButton routerLink="/" class="back">
      <mat-icon svgIcon="arrow_back" />
      Alle Jahre
    </a>

    <div class="toolbar">
      <nav class="year" aria-label="Jahr wählen">
        <button
          matIconButton
          type="button"
          aria-label="Vorheriges Jahr"
          [disabled]="!store.hasPreviousYear()"
          (click)="showYear(store.year() - 1)"
        >
          <mat-icon svgIcon="chevron_left" />
        </button>
        <h1>Vermögen {{ store.year() }}</h1>
        <button
          matIconButton
          type="button"
          aria-label="Nächstes Jahr"
          [disabled]="!store.hasNextYear()"
          (click)="showYear(store.year() + 1)"
        >
          <mat-icon svgIcon="chevron_right" />
        </button>
      </nav>
      @if (store.overview() && !store.empty()) {
        <button matButton type="button" (click)="copy()">
          <mat-icon svgIcon="content_copy" />
          Vermögen übernehmen
        </button>
      }
    </div>

    @if (store.loadError(); as error) {
      <div class="load-error" role="alert">
        <span>Daten konnten nicht geladen werden: {{ error }}</span>
        <button matButton type="button" (click)="store.load()">Erneut versuchen</button>
      </div>
    }

    @if (store.empty()) {
      <mat-card appearance="outlined" class="empty">
        <mat-card-content>
          <p>Für {{ store.year() }} ist noch kein Vermögen erfasst.</p>
          <div class="actions">
            <button matButton="filled" type="button" (click)="add('ASSET')">Wert erfassen</button>
            @if (otherYears().length > 0) {
              <button matButton="outlined" type="button" (click)="copyHere()">
                <mat-icon svgIcon="content_copy" />
                Aus anderem Jahr übernehmen
              </button>
            }
          </div>
        </mat-card-content>
      </mat-card>
    } @else if (store.overview(); as overview) {
      <mat-card appearance="outlined" class="sums" aria-label="Summen">
        <div>
          <span class="label">Vermögen</span>
          <span class="value">{{ overview.totalAssets | currency: 'EUR' }}</span>
        </div>
        <div>
          <span class="label">Schulden</span>
          <span class="value">{{ overview.totalLiabilities | currency: 'EUR' }}</span>
        </div>
        <div class="net-worth">
          <span class="label">Nettovermögen</span>
          <span class="value" [class.overindebted]="overview.overindebted">
            {{ overview.netWorth | currency: 'EUR' }}
          </span>
          @if (overview.overindebted) {
            <span class="overindebted">Überschuldet</span>
          }
        </div>
        <div class="change" [attr.aria-label]="changeText(overview)">
          <span class="label">Zum Vorjahr ({{ overview.year - 1 }})</span>
          @if (overview.change; as change) {
            <span class="value" [class]="tone(overview)">{{ amountLabel(overview) }}</span>
            <span class="percentage" [class]="tone(overview)">{{ percentageLabel(overview) }}</span>
          } @else {
            <span class="value none">–</span>
          }
        </div>
      </mat-card>

      <div class="lists">
        @for (type of types; track type.value) {
          <mat-card appearance="outlined" class="list" [attr.data-type]="type.value">
            <h2>{{ type.label }}</h2>
            <ul>
              @for (entry of entriesOf(overview, type.value); track entry.id) {
                <li class="entry">
                  <span class="name">{{ entry.name }}</span>
                  <span class="amount">{{ entry.amount | currency: 'EUR' }}</span>
                  <button
                    matIconButton
                    type="button"
                    [attr.aria-label]="entry.name + ' bearbeiten'"
                    title="Bearbeiten"
                    (click)="edit(entry)"
                  >
                    <mat-icon svgIcon="edit" />
                  </button>
                  <button
                    matIconButton
                    type="button"
                    class="remove"
                    [attr.aria-label]="entry.name + ' löschen'"
                    title="Löschen"
                    (click)="remove(entry)"
                  >
                    <mat-icon svgIcon="delete" />
                  </button>
                </li>
              }
              <li class="add">
                <button matButton type="button" (click)="add(type.value)">
                  <mat-icon svgIcon="add" />
                  Wert erfassen
                </button>
              </li>
              <li class="sum">
                <span class="name">Summe {{ type.label }}</span>
                <span class="amount">{{ sumOf(overview, type.value) | currency: 'EUR' }}</span>
              </li>
            </ul>
          </mat-card>
        }
      </div>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    .loading {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 10;
    }
    .back {
      align-self: flex-start;
      margin-bottom: -16px;
    }
    .toolbar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }
    .year {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .year h1 {
      margin: 0;
      font: var(--mat-sys-headline-small);
    }
    .load-error {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 12px 16px;
      border-radius: 12px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    .empty mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
      padding-top: 16px;
    }
    .empty p {
      margin: 0;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .sums {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
    .sums > div {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 16px;
      min-width: 0;
    }
    .sums > div + div {
      border-left: 1px solid var(--mat-sys-outline-variant);
    }
    .sums .label {
      font: var(--mat-sys-label-large);
      color: var(--mat-sys-on-surface-variant);
    }
    .sums .value {
      font: var(--mat-sys-title-large);
      font-variant-numeric: tabular-nums;
    }
    .net-worth .value {
      font-weight: 500;
    }
    .percentage,
    span.overindebted:not(.value) {
      font: var(--mat-sys-body-small);
    }
    .overindebted,
    .bad {
      color: var(--mat-sys-error);
    }
    .good {
      color: var(--app-income-color);
    }
    .neutral,
    .none {
      color: var(--mat-sys-on-surface-variant);
    }
    .lists {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 24px;
      align-items: start;
    }
    .list h2 {
      margin: 0;
      padding: 16px 16px 8px;
      font: var(--mat-sys-title-large);
    }
    .list h2::after {
      content: '';
      display: block;
      width: 32px;
      height: 4px;
      margin-top: 6px;
      border-radius: 2px;
      background: var(--app-gain-color);
    }
    .list[data-type='LIABILITY'] h2::after {
      background: var(--app-loss-color);
    }
    ul {
      margin: 0;
      padding: 0;
      list-style: none;
    }
    li {
      display: flex;
      align-items: center;
      gap: 4px;
      min-height: 48px;
      padding: 4px 8px 4px 16px;
      border-top: 1px solid var(--mat-sys-outline-variant);
    }
    li .name {
      flex: 1;
      min-width: 0;
      overflow-wrap: anywhere;
      font: var(--mat-sys-body-large);
    }
    li .amount {
      font: var(--mat-sys-body-large);
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }
    li.add {
      padding-left: 4px;
    }
    li.sum {
      /* The amount lines up with those of the entries above, left of their two buttons */
      padding-right: 96px;
      border-radius: 0 0 12px 12px;
      background: var(--mat-sys-surface-container);
    }
    li.sum .name,
    li.sum .amount {
      font: var(--mat-sys-title-medium);
    }
    .remove {
      --mat-icon-button-icon-color: var(--mat-sys-error);
      --mat-icon-button-state-layer-color: var(--mat-sys-error);
    }
    @media (max-width: 860px) {
      .lists {
        grid-template-columns: minmax(0, 1fr);
      }
    }
    @media (max-width: 600px) {
      .sums {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
      .sums > div:nth-child(3) {
        border-left: none;
      }
      .sums > div:nth-child(n + 3) {
        border-top: 1px solid var(--mat-sys-outline-variant);
      }
    }
  `,
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
