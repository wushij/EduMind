import { describe, expect, it } from 'vitest';
import { buildLessonToc } from './lesson-toc';

describe('buildLessonToc', () => {
  it('parses headings after same normalize as render (glued ##)', () => {
    const items = buildLessonToc([
      {
        type: 'markdown',
        body: '##三、核心概念\n\n###3.1 JDK\n\n正文'
      }
    ]);
    expect(items.map(i => i.title)).toEqual(['三、核心概念', '3.1 JDK']);
  });

  it('collects objective, markdown headings in order', () => {
    const items = buildLessonToc([
      {
        type: 'callout',
        variant: 'objective',
        title: '学习目标',
        body: '- a'
      },
      {
        type: 'markdown',
        body: '## 一、JDK 体系\n\n### 1.1 组成\n\n正文'
      }
    ]);
    expect(items.map(i => i.title)).toEqual(['学习目标', '一、JDK 体系', '1.1 组成']);
    expect(items[1].level).toBe(2);
    expect(items[2].level).toBe(3);
  });
});
