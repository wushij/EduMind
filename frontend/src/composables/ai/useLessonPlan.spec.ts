import { describe, it, expect } from 'vitest';
import { renderLessonPlanMarkdown } from './useLessonPlan';

describe('renderLessonPlanMarkdown', () => {
  it('converts markdown headings to html tags', () => {
    const html = renderLessonPlanMarkdown('## 教学目标\n### 知识点');
    expect(html).toContain('<h2>教学目标</h2>');
    expect(html).toContain('<h3>知识点</h3>');
  });

  it('replaces newlines with br tags', () => {
    expect(renderLessonPlanMarkdown('第一行\n第二行')).toContain('<br/>');
  });

  it('returns empty string for empty input', () => {
    expect(renderLessonPlanMarkdown('')).toBe('');
  });
});
