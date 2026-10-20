/**
 * How the yearly overview shows a change from the previous year (JU-5 to JU-8). The backend
 * works out the change; this only formats and classifies it.
 */

/** What a change in a column means for the user: more income or balance is good, more expenses bad. */
export type ChangeKind = 'income' | 'expense' | 'balance';

/** Whether a change is good, bad or neither for the user (JU-8). */
export type ChangeTone = 'good' | 'bad' | 'neutral';

const PERCENT = new Intl.NumberFormat('de-DE', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** E.g. "+3,00 %", "−3,00 %", "±0,00 %"; "–" without a change (JU-6, JU-7). */
export function changeLabel(change: number | null): string {
  if (change === null) {
    return '–';
  }
  const sign = change > 0 ? '+' : change < 0 ? '−' : '±';
  return `${sign}${PERCENT.format(Math.abs(change))} %`;
}

/** Green for more income or balance and for less expenses, red the other way round (JU-8). */
export function changeTone(kind: ChangeKind, change: number | null): ChangeTone {
  if (change === null || change === 0) {
    return 'neutral';
  }
  const increaseIsGood = kind !== 'expense';
  return change > 0 === increaseIsGood ? 'good' : 'bad';
}

/** What a change means in words, for screen readers, as the colour alone does not tell (JU-8). */
export function changeDescription(change: number | null): string {
  if (change === null) {
    return 'keine Veränderung zum Vorjahr bekannt';
  }
  if (change === 0) {
    return 'unverändert zum Vorjahr';
  }
  return `${changeLabel(change)} ${change > 0 ? 'mehr' : 'weniger'} als im Vorjahr`;
}
