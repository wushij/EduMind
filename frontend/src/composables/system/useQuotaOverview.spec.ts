import { describe, it, expect } from 'vitest';
import { computeQuotaUsagePercent } from './useQuotaOverview';

describe('computeQuotaUsagePercent', () => {
  it('returns 0 when quota is unlimited', () => {
    expect(computeQuotaUsagePercent(5000, 10000, true)).toBe(0);
  });

  it('computes rounded usage percentage', () => {
    expect(computeQuotaUsagePercent(2500, 10000)).toBe(25);
  });

  it('caps percentage at 100', () => {
    expect(computeQuotaUsagePercent(15000, 10000)).toBe(100);
  });
});
