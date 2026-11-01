import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { NetWorthApi } from '../data-access/net-worth-api';
import { NetWorthOverview } from '../net-worth.model';
import { NetWorthCopy } from './copy-net-worth-dialog';

function overview(assets: number, liabilities: number): NetWorthOverview {
  return {
    assets: Array.from({ length: assets }),
    liabilities: Array.from({ length: liabilities }),
  } as unknown as NetWorthOverview;
}

// VS-15 to VS-17; the dialog itself is covered by the tests of CopyPlanDialog
describe('NetWorthCopy', () => {
  let api: { getOverview: ReturnType<typeof vi.fn>; copy: ReturnType<typeof vi.fn> };
  let kind: NetWorthCopy;

  beforeEach(() => {
    api = { getOverview: vi.fn(), copy: vi.fn(() => of(overview(1, 0))) };
    TestBed.configureTestingModule({ providers: [{ provide: NetWorthApi, useValue: api }] });
    kind = TestBed.inject(NetWorthCopy);
  });

  it('counts assets and liabilities and copies by year', async () => {
    api.getOverview.mockReturnValue(of(overview(2, 1)));

    const content = await firstValueFrom(kind.lookUp(2026));
    expect(content?.count).toBe(3);

    await firstValueFrom(content!.copyTo(2027, true));
    expect(api.copy).toHaveBeenCalledWith(2026, { targetYear: 2027, overwrite: true });
  });

  // VS-7
  it('treats a year without assets and liabilities as not recorded', async () => {
    api.getOverview.mockReturnValue(of(overview(0, 0)));

    expect(await firstValueFrom(kind.lookUp(2026))).toBeNull();
  });

  // VS-16
  it('names one or more entries', () => {
    expect(kind.takenOver(1)).toBe('1 Wert wird mit den Beträgen übernommen.');
    expect(kind.takenOver(3)).toBe('3 Werte werden mit den Beträgen übernommen.');
    expect(kind.overwriteWarning(2027, 1)).toContain(
      'Für 2027 ist bereits Vermögen mit 1 Wert erfasst.',
    );
    expect(kind.overwriteWarning(2027, 2)).toContain('mit 2 Werten erfasst.');
  });
});
