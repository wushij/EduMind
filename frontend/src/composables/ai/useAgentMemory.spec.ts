import { describe, it, expect } from 'vitest';
import type { MemoryItemVO } from '@/types/ai/memory';
import {
  filterMemoryItems,
  getMemoryTypeLabel,
  getMemoryTypeTagType
} from './useAgentMemory';

const sampleItems: MemoryItemVO[] = [
  {
    id: 1,
    memoryType: 'PREFERENCE',
    summary: '偏好图解法',
    memoryValue: '喜欢图解',
    memoryKey: 'style'
  },
  {
    id: 2,
    memoryType: 'EPISODIC',
    summary: '导数薄弱',
    memoryValue: '链式法则易错',
    memoryKey: 'weak'
  }
];

describe('filterMemoryItems', () => {
  it('returns all items when filters are empty', () => {
    expect(filterMemoryItems(sampleItems, '')).toHaveLength(2);
  });

  it('filters by keyword across summary and value', () => {
    expect(filterMemoryItems(sampleItems, '导数')).toHaveLength(1);
  });

  it('filters by memory type', () => {
    expect(filterMemoryItems(sampleItems, '', 'PREFERENCE')).toHaveLength(1);
  });
});

describe('memory type helpers', () => {
  it('maps memory type labels', () => {
    expect(getMemoryTypeLabel('PREFERENCE')).toBe('学习偏好');
    expect(getMemoryTypeLabel('UNKNOWN')).toBe('记忆');
  });

  it('maps memory type tag types', () => {
    expect(getMemoryTypeTagType('PROFILE')).toBe('danger');
    expect(getMemoryTypeTagType('UNKNOWN')).toBe('info');
  });
});
