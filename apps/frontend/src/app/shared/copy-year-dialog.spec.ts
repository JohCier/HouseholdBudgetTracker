import { suggestedSourceYear, suggestedTargetYear } from './copy-year-dialog';

describe('suggested years', () => {
  // PU-2
  it('suggests the following year as target, the year before for 2200', () => {
    expect(suggestedTargetYear(2026)).toBe(2027);
    expect(suggestedTargetYear(2200)).toBe(2199);
  });

  // PU-13
  it('suggests the newest plan before the target year as source, otherwise the oldest after it', () => {
    expect(suggestedSourceYear(2027, [2030, 2025, 2024])).toBe(2025);
    expect(suggestedSourceYear(2020, [2030, 2025])).toBe(2025);
    expect(suggestedSourceYear(2020, [])).toBeNull();
  });
});
