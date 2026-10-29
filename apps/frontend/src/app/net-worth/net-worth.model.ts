/**
 * Client-side representation of the net worth REST contract (Vermögen und Schulden). The types come
 * from @hbt/shared-types, generated from packages/shared-types/openapi/plans.json. Monetary values
 * are decimal strings (e.g. "3200.00"); liabilities have positive amounts as well (VS-1).
 */
import type { PlansSchemas as Schemas } from '@hbt/shared-types';

export type NetWorthEntryType = Schemas['NetWorthEntryType'];
export type NetWorthEntry = Schemas['NetWorthEntryResponse'];
export type NetWorthEntryRequest = Schemas['NetWorthEntryRequest'];
export type NetWorthChange = NonNullable<Schemas['NetWorthChangeResponse']>;
export type NetWorthOverview = Schemas['NetWorthOverviewResponse'];
export type NetWorthHistory = Schemas['NetWorthHistoryResponse'];
export type NetWorthHistoryEntry = Schemas['NetWorthHistoryEntryResponse'];
export type CopyNetWorthRequest = Schemas['CopyNetWorthRequest'];

/** The two lists of the page "Vermögen" with their headings (VS-8). */
export const NET_WORTH_ENTRY_TYPES: readonly {
  value: NetWorthEntryType;
  label: string;
  singular: string;
}[] = [
  { value: 'ASSET', label: 'Vermögen', singular: 'Vermögenswert' },
  { value: 'LIABILITY', label: 'Schulden', singular: 'Schuld' },
];

/** Whether the overview of a year has a statement, i.e. at least one asset or liability (VS-7). */
export function isRecorded(overview: NetWorthOverview): boolean {
  return overview.assets.length + overview.liabilities.length > 0;
}

/** How many assets and liabilities the overview of a year has. */
export function entryCount(overview: NetWorthOverview): number {
  return overview.assets.length + overview.liabilities.length;
}
