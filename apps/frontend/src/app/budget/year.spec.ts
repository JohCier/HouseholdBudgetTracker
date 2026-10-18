import { currentYear } from './year';

describe('currentYear', () => {
  afterEach(() => vi.useRealTimers());

  it('is the year of the local date', () => {
    // New Year's Eve in local time, already the next year in UTC for time zones west of UTC.
    vi.useFakeTimers({ now: new Date(2026, 11, 31, 23, 30) });

    expect(currentYear()).toBe(2026);
  });
});
