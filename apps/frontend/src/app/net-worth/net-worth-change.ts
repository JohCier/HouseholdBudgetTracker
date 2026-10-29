/**
 * How the change of the net worth from the year before is shown (VS-12, VS-13). The backend works
 * out the change; this only formats and classifies it, like `budget/year-change.ts`.
 */
import { changeLabel } from '../budget/year-change';
import { NetWorthChange } from './net-worth.model';

const EURO = new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' });

/** E.g. "13.900,00 €", "−3.000,00 €". */
export function euro(value: string | number): string {
  return EURO.format(Number(value)).replace('-', '−');
}

/** E.g. "+3.400,00 €", "−2.200,00 €", "±0,00 €" (VS-12, like JU-7). */
export function changeAmountLabel(change: NetWorthChange): string {
  const amount = Number(change.amount);
  const sign = amount > 0 ? '+' : amount < 0 ? '−' : '±';
  return `${sign}${EURO.format(Math.abs(amount))}`;
}

/** E.g. "+32,38 %"; "–" if the previous net worth was zero (VS-13). */
export function changePercentageLabel(change: NetWorthChange): string {
  return changeLabel(change.percentage);
}

/** E.g. "+3.400,00 € (+32,38 %)". */
export function changeFullLabel(change: NetWorthChange): string {
  return `${changeAmountLabel(change)} (${changePercentageLabel(change)})`;
}

/** Green for growth, red for loss, neutral without change (VS-13, like JU-8). */
export function changeTone(change: NetWorthChange | null): 'good' | 'bad' | 'neutral' {
  const amount = change === null ? 0 : Number(change.amount);
  return amount > 0 ? 'good' : amount < 0 ? 'bad' : 'neutral';
}

/** What a change means in words, as the colour alone does not tell (VS-22, like JU-8). */
export function changeDescription(change: NetWorthChange | null): string {
  if (change === null) {
    return 'kein Vergleich mit dem Vorjahr';
  }
  const tone = changeTone(change);
  const meaning = tone === 'good' ? 'Wachstum' : tone === 'bad' ? 'Verlust' : 'unverändert';
  return `${meaning} zum Vorjahr: ${changeFullLabel(change)}`;
}
