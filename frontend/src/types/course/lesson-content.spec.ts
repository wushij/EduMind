import { describe, it, expect } from 'vitest';
import { parseLessonContent, serializeLessonContent } from '@/types/course/lesson-content';

describe('lesson-content', () => {
  it('parses empty json safely', () => {
    const doc = parseLessonContent(null);
    expect(doc.version).toBe(1);
    expect(doc.blocks).toEqual([]);
  });

  it('round-trips content document', () => {
    const doc = {
      version: 1,
      blocks: [{ type: 'markdown' as const, body: 'hello' }]
    };
    const json = serializeLessonContent(doc);
    const parsed = parseLessonContent(json);
    expect(parsed.blocks[0].type).toBe('markdown');
  });
});
