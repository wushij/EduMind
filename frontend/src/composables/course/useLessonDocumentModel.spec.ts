import { describe, it, expect } from 'vitest';
import {
  blocksToStudio,
  studioToBlocks,
  createDefaultStudioDocument
} from './useLessonDocumentModel';

describe('useLessonDocumentModel', () => {
  it('returns default template for empty blocks', () => {
    const studio = blocksToStudio({ version: 1, blocks: [] });
    expect(studio.mainMarkdown).toBe('');
    expect(studio.objectiveCallout.title).toBe('学习目标');
    expect(studio.objectiveCallout.body).toBe('');
    expect(studio.extraBlocks).toEqual([]);
  });

  it('round-trips objective and main markdown', () => {
    const studio = {
      ...createDefaultStudioDocument(),
      mainMarkdown: '## 核心\n\n正文内容',
      objectiveCallout: { title: '目标', body: '掌握 JVM' }
    };
    const doc = studioToBlocks(studio);
    expect(doc.blocks[0].type).toBe('callout');
    expect(doc.blocks[1].type).toBe('markdown');
    const back = blocksToStudio(doc);
    expect(back.mainMarkdown).toBe('## 核心\n\n正文内容');
    expect(back.objectiveCallout.body).toBe('掌握 JVM');
  });

  it('maps knowledgePoints block to ids only, not extraBlocks', () => {
    const doc = {
      version: 1,
      blocks: [
        { type: 'markdown' as const, body: '正文' },
        { type: 'knowledgePoints' as const, knowledgePointIds: [1, 2] }
      ]
    };
    const studio = blocksToStudio(doc);
    expect(studio.knowledgePointIds).toEqual([1, 2]);
    expect(studio.extraBlocks.some(b => b.type === 'knowledgePoints')).toBe(false);
    const back = studioToBlocks(studio);
    expect(back.blocks.some(b => b.type === 'knowledgePoints' && b.knowledgePointIds?.join() === '1,2')).toBe(
      true
    );
  });

  it('uses first markdown block only as main', () => {
    const doc = {
      version: 1,
      blocks: [
        { type: 'markdown' as const, body: 'first' },
        { type: 'markdown' as const, body: 'second' }
      ]
    };
    const studio = blocksToStudio(doc);
    expect(studio.mainMarkdown).toBe('first');
    expect(studio.extraBlocks.filter(b => b.type === 'markdown').length).toBe(1);
  });
});
