import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { NetWorthHistoryEntry } from '../net-worth.model';
import { NetWorthCard } from './net-worth-card';

const entry = (
  year: number,
  netWorth: string,
  change: NetWorthHistoryEntry['change'] = null,
): NetWorthHistoryEntry => ({
  year,
  totalAssets: '21700.00',
  totalLiabilities: '7800.00',
  netWorth,
  overindebted: Number(netWorth) < 0,
  change,
});

const plain = (text: string) => text.replace(/\s+/g, ' ').trim();

// docs/specs/vermoegen.md
describe('NetWorthCard', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<NetWorthCard>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function render(years: NetWorthHistoryEntry[]): Promise<HTMLElement> {
    fixture = TestBed.createComponent(NetWorthCard);
    fixture.detectChanges();
    http.expectOne('/api/net-worth/years').flush({ years });
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  // VS-18, VS-12, VS-13
  it('lists the years, the newest first, with net worth and change', async () => {
    const element = await render([
      entry(2026, '13900.00', { amount: '3400.00', percentage: 32.38 }),
      entry(2025, '10500.00'),
    ]);

    expect([...element.querySelectorAll('tbody th')].map((th) => th.textContent!.trim())).toEqual([
      '2026',
      '2025',
    ]);
    // Amounts of the currency pipe use the default locale of the tests, the change is always German
    const hero = plain(element.querySelector('.hero')!.textContent!);
    expect(hero).toContain('13,900.00');
    expect(hero).toContain('+3.400,00 € (+32,38 %) zum Vorjahr');
    const change = element.querySelector('tr[data-year="2026"] .change')!;
    expect(plain(change.textContent!)).toBe('+3.400,00 €+32,38 %');
    expect(change.classList).toContain('good');
    expect(plain(element.querySelector('tr[data-year="2025"] .change')!.textContent!)).toBe('–');
  });

  // VS-6
  it('marks a negative net worth as overindebted', async () => {
    const element = await render([entry(2026, '-3000.00')]);

    expect(element.querySelector('tr[data-year="2026"]')!.textContent).toContain('Überschuldet');
  });

  // VS-18
  it('opens the page of a year by clicking its row', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const element = await render([entry(2026, '13900.00')]);

    element.querySelector<HTMLElement>('tr[data-year="2026"]')!.click();

    expect(navigate).toHaveBeenCalledWith(['/vermoegen', 2026]);
  });

  // VS-19
  it('offers to record the net worth while there is none', async () => {
    const element = await render([]);

    expect(element.textContent).toContain('Noch kein Vermögen erfasst.');
    expect(element.querySelector('a')!.textContent!.trim()).toBe('Vermögen erfassen');
    expect(element.querySelector('app-net-worth-chart')).toBeNull();
  });
});
