import { describe, it, expect } from 'vitest';
import { useSummaryStudio } from './useSummaryStudio';

describe('useSummaryStudio', () => {
  it('initializes default generation state', () => {
    const studio = useSummaryStudio();
    expect(studio.sourceType.value).toBe('DOCUMENT');
    expect(studio.mode.value).toBe('OVERVIEW');
    expect(studio.title.value).toBe('');
    expect(studio.content.value).toBe('');
    expect(studio.generating.value).toBe(false);
    expect(studio.hasResult.value).toBe(false);
    expect(studio.records.value).toEqual([]);
  });

  it('exposes core handlers', () => {
    const studio = useSummaryStudio();
    expect(typeof studio.handleGenerate).toBe('function');
    expect(typeof studio.handleStop).toBe('function');
    expect(typeof studio.openRecord).toBe('function');
    expect(typeof studio.exportMarkdown).toBe('function');
  });
});
