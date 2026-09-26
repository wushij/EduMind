import { describe, it, expect } from 'vitest';
import {
  buildRecommendedQuestionMaterial,
  mapChapterTree,
  resolveActiveCourse
} from './useCourseAIWorkspace';
import type { Chapter } from '@/types/course/chapter';
import type { Course } from '@/types/course/course';

describe('mapChapterTree', () => {
  it('maps nested chapters with child sections', () => {
    const nodes: Chapter[] = [
      {
        id: 1,
        title: 'Chapter 1',
        children: [
          { id: 11, title: 'Section 1.1', children: [] },
          { id: 12, title: 'Section 1.2', children: [] }
        ]
      }
    ];

    const result = mapChapterTree(nodes);
    expect(result).toHaveLength(1);
    expect(result[0].sections).toHaveLength(2);
    expect(result[0].expanded).toBe(true);
  });

  it('uses chapter itself as section when no children', () => {
    const nodes: Chapter[] = [{ id: 2, title: 'Standalone', children: [] }];
    const result = mapChapterTree(nodes, false);
    expect(result[0].sections).toEqual([{ id: 2, title: 'Standalone' }]);
    expect(result[0].expanded).toBe(false);
  });

  it('returns empty array for empty input', () => {
    expect(mapChapterTree([])).toEqual([]);
  });
});

describe('resolveActiveCourse', () => {
  const courses = [
    { id: 1, title: 'Course A' } as Course,
    { id: 2, title: 'Course B' } as Course
  ];

  it('returns null for empty course list', () => {
    expect(resolveActiveCourse([])).toBeNull();
  });

  it('prefers stored course id when present in list', () => {
    expect(resolveActiveCourse(courses, 2)?.id).toBe(2);
  });

  it('falls back to first course when stored id is missing', () => {
    expect(resolveActiveCourse(courses, 99)?.id).toBe(1);
  });
});

describe('buildRecommendedQuestionMaterial', () => {
  it('carries course + chapter structure + active section so questions anchor on real content', () => {
    const chapters = mapChapterTree([
      {
        id: 1,
        title: '第一章 函数与极限',
        children: [{ id: 11, title: '1.1 数列与函数极限计算', children: [] }]
      }
    ] as Chapter[]);

    const material = buildRecommendedQuestionMaterial('高等数学', chapters, '1.1 数列与函数极限计算');

    expect(material).toContain('课程：高等数学');
    expect(material).toContain('函数与极限');
    expect(material).toContain('数列与函数极限计算');
    expect(material).toContain('当前正在学习的知识点：数列与函数极限计算');
    expect(material).toContain('【提问规范】');
  });

  it('still yields usable material before chapters are loaded', () => {
    const material = buildRecommendedQuestionMaterial('高等数学', []);

    expect(material).toContain('课程：高等数学');
    expect(material).not.toContain('章节结构');
  });
});
