import { describe, it, expect } from 'vitest';
import {
  COMMON_FUNCTIONS,
  DEFAULT_TODO_ITEMS,
  ABILITY_LEGEND,
  LATEST_NEWS,
  TEACHING_ACTIVITY_DATA,
  buildLineChartOption,
  buildPieChartOption
} from './useDashboard';

describe('dashboard static data', () => {
  it('defines eight common function shortcuts', () => {
    expect(COMMON_FUNCTIONS).toHaveLength(8);
    expect(COMMON_FUNCTIONS[0].route).toBe('/ai/marketplace');
  });

  it('defines five default todo items', () => {
    expect(DEFAULT_TODO_ITEMS).toHaveLength(5);
    expect(DEFAULT_TODO_ITEMS[0].title).toBe('待批改作业');
  });

  it('defines ability legend and latest news collections', () => {
    expect(ABILITY_LEGEND).toHaveLength(4);
    expect(LATEST_NEWS).toHaveLength(5);
  });
});

describe('chart option builders', () => {
  it('builds line chart option with weekly activity data', () => {
    const option = buildLineChartOption();
    const series = option.series as Array<{ data?: number[] }>;

    expect(series[0].data).toEqual(TEACHING_ACTIVITY_DATA);
  });

  it('builds pie chart option with four ability segments', () => {
    const option = buildPieChartOption();
    const series = option.series as Array<{ data?: Array<{ name: string }> }>;

    expect(series[0].data).toHaveLength(4);
    expect(series[0].data?.[0].name).toBe('优秀');
  });
});
