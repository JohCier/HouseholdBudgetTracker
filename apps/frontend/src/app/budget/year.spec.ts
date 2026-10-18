import { currentYear } from './year';

describe('currentYear', () => {
  afterEach(() => vi.useRealTimers());

  it('is the year of the local date', () => {
    // New Year's Eve in local time, already the next year in UTC for time zones west of UTC.
    vi.useFakeTimers({ now: new Date(2026, 11, 31, 23, 30) });

    expect(currentYear()).toBe(2026);
  });

  // HP-1
  it('stays within the years a plan can exist for', () => {
    vi.useFakeTimers({ now: new Date(2300, 0, 1) });
    expect(currentYear()).toBe(2200);

    vi.useFakeTimers({ now: new Date(1850, 0, 1) });
    expect(currentYear()).toBe(1900);
  });
});
