import { yearOf } from './year';

describe('year helpers', () => {
  it('extracts the year of an ISO date', () => {
    expect(yearOf('2026-10-04')).toBe(2026);
    expect(yearOf('2027-01-01')).toBe(2027);
  });
});
