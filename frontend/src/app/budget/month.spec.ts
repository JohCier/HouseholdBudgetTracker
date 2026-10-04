import { addMonths, monthOf } from './month';

describe('month helpers', () => {
  it('adds months across year boundaries', () => {
    expect(addMonths('2026-12', 1)).toBe('2027-01');
    expect(addMonths('2026-01', -1)).toBe('2025-12');
    expect(addMonths('2026-10', 0)).toBe('2026-10');
  });

  it('extracts the month of an ISO date', () => {
    expect(monthOf('2026-10-04')).toBe('2026-10');
  });
});
