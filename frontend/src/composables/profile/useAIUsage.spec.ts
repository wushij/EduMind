import { describe, it, expect } from 'vitest';
import {
  formatUsageNumber,
  formatUsageDateTime,
  calcTodayUsagePercent,
  resolveQuotaPillClass,
  AI_USAGE_CHANGED_EVENT,
  AI_USAGE_REFRESH_INTERVAL_MS
} from './useAIUsage';

describe('formatUsageNumber', () => {
  it('formats numbers with zh-CN locale grouping', () => {
    expect(formatUsageNumber(1842000)).toBe('1,842,000');
  });
});

describe('formatUsageDateTime', () => {
  it('replaces T separator and trims seconds precision', () => {
    expect(formatUsageDateTime('2026-09-15T10:24:30')).toBe('2026-09-15 10:24:30');
  });

  it('returns dash for empty value', () => {
    expect(formatUsageDateTime('')).toBe('-');
  });
});

describe('calcTodayUsagePercent', () => {
  it('calculates capped usage percentage', () => {
    expect(calcTodayUsagePercent(36800, 100000)).toBe(37);
    expect(calcTodayUsagePercent(150000, 100000)).toBe(100);
  });

  it('returns 0 when daily limit is missing', () => {
    expect(calcTodayUsagePercent(100, 0)).toBe(0);
  });
});

describe('resolveQuotaPillClass', () => {
  it('maps remaining percent to status classes', () => {
    expect(resolveQuotaPillClass(80)).toBe('is-success');
    expect(resolveQuotaPillClass(35)).toBe('is-warning');
    expect(resolveQuotaPillClass(10)).toBe('is-danger');
  });
});

describe('AI usage constants', () => {
  it('exposes refresh event name and interval', () => {
    expect(AI_USAGE_CHANGED_EVENT).toBe('edumind:ai-usage-changed');
    expect(AI_USAGE_REFRESH_INTERVAL_MS).toBe(15000);
  });
});
