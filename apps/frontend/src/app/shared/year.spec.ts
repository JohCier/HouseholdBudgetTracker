import { currentYear, defaultDateIn, isoDate, parseYear } from './year';

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

describe('parseYear', () => {
  // HP-1, JU-11
  it('reads a year a plan can exist for from the address', () => {
    expect(parseYear('2026')).toBe(2026);
    expect(parseYear('1900')).toBe(1900);
    expect(parseYear('2200')).toBe(2200);
  });

  it('rejects anything else', () => {
    expect(parseYear(undefined)).toBeNull();
    expect(parseYear('1899')).toBeNull();
    expect(parseYear('2201')).toBeNull();
    expect(parseYear('20a6')).toBeNull();
    expect(parseYear('2026.5')).toBeNull();
    expect(parseYear('')).toBeNull();
  });
});

// docs/specs/einmalposten.md
describe('defaultDateIn', () => {
  // EP-11
  it('is today if today lies in the year shown', () => {
    expect(defaultDateIn(2026, new Date(2026, 9, 29))).toBe('2026-10-29');
  });

  // EP-11
  it('is 1 January of the year shown otherwise', () => {
    expect(defaultDateIn(2027, new Date(2026, 9, 29))).toBe('2027-01-01');
    expect(defaultDateIn(1999, new Date(2026, 9, 29))).toBe('1999-01-01');
  });

  it('writes days in the format of the API, in local time', () => {
    expect(isoDate(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05');
  });
});
