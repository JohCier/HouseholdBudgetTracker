import {
  changeAmountLabel,
  changeDescription,
  changeFullLabel,
  changePercentageLabel,
  changeTone,
  euro,
} from './net-worth-change';

/** The currency format separates the euro sign with a no-break space. */
const plain = (text: string) => text.replace(/\u00a0/g, ' ');

// docs/specs/vermoegen.md
describe('net worth change', () => {
  // VS-12
  it('shows the change in euros and percent with sign', () => {
    expect(plain(changeAmountLabel({ amount: '3400.00', percentage: 32.38 }))).toBe('+3.400,00 €');
    expect(plain(changeAmountLabel({ amount: '-2200.00', percentage: -36.67 }))).toBe(
      '−2.200,00 €',
    );
    expect(plain(changeAmountLabel({ amount: '0.00', percentage: 0 }))).toBe('±0,00 €');
    expect(plain(changeFullLabel({ amount: '3400.00', percentage: 32.38 }))).toBe(
      '+3.400,00 € (+32,38 %)',
    );
  });

  // VS-13
  it('shows a dash instead of percent after a net worth of zero', () => {
    expect(changePercentageLabel({ amount: '1000.00', percentage: null })).toBe('–');
  });

  // VS-13
  it('rates growth as good and loss as bad', () => {
    expect(changeTone({ amount: '1.00', percentage: 1 })).toBe('good');
    expect(changeTone({ amount: '-1.00', percentage: -1 })).toBe('bad');
    expect(changeTone({ amount: '0.00', percentage: 0 })).toBe('neutral');
    expect(changeTone(null)).toBe('neutral');
  });

  // VS-22
  it('describes the change in words', () => {
    expect(plain(changeDescription({ amount: '-2200.00', percentage: -36.67 }))).toBe(
      'Verlust zum Vorjahr: −2.200,00 € (−36,67 %)',
    );
    expect(plain(changeDescription(null))).toBe('kein Vergleich mit dem Vorjahr');
  });

  it('formats negative euros with a minus sign', () => {
    expect(plain(euro('-3000.00'))).toBe('−3.000,00 €');
  });
});
