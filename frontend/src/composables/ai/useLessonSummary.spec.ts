import { describe, it, expect } from 'vitest';
import { useLessonSummary } from './useLessonSummary';

describe('useLessonSummary', () => {
  it('initializes empty form state', () => {
    const { documentId, content, loading, result } = useLessonSummary();
    expect(documentId.value).toBeUndefined();
    expect(content.value).toBe('');
    expect(loading.value).toBe(false);
    expect(result.value).toBe('');
  });

  it('exposes summarize handler', () => {
    const { handleSummarize } = useLessonSummary();
    expect(typeof handleSummarize).toBe('function');
  });
});
