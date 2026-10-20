import { changeDescription, changeLabel, changeTone } from './year-change';

// docs/specs/jahresuebersicht.md
describe('year change', () => {
  // JU-7
  it('shows the change with sign and two decimal places', () => {
    expect(changeLabel(3)).toBe('+3,00 %');
    expect(changeLabel(-3)).toBe('−3,00 %');
    expect(changeLabel(0)).toBe('±0,00 %');
    expect(changeLabel(1234.5)).toBe('+1.234,50 %');
  });

  // JU-6
  it('shows a dash without a change', () => {
    expect(changeLabel(null)).toBe('–');
  });

  // JU-8
  it('rates more income and balance as good and more expenses as bad', () => {
    expect(changeTone('income', 3)).toBe('good');
    expect(changeTone('income', -3)).toBe('bad');
    expect(changeTone('balance', 33)).toBe('good');
    expect(changeTone('balance', -1)).toBe('bad');
    expect(changeTone('expense', -3)).toBe('good');
    expect(changeTone('expense', 3)).toBe('bad');
    expect(changeTone('expense', 0)).toBe('neutral');
    expect(changeTone('income', null)).toBe('neutral');
  });

  it('describes the change in words', () => {
    expect(changeDescription(3)).toBe('+3,00 % mehr als im Vorjahr');
    expect(changeDescription(-3)).toBe('−3,00 % weniger als im Vorjahr');
    expect(changeDescription(0)).toBe('unverändert zum Vorjahr');
    expect(changeDescription(null)).toBe('keine Veränderung zum Vorjahr bekannt');
  });
});
