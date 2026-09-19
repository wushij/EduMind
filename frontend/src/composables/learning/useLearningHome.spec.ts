import { describe, it, expect } from 'vitest';
import { resolveCourseRoute } from '@/utils/learning/course-route';

describe('learning home utilities', () => {
  it('resolveCourseRoute builds course tab paths', () => {
    expect(resolveCourseRoute(102, 'resources')).toBe('/course/102/resources');
    expect(resolveCourseRoute(102, 'ai')).toBe('/course/102/ai');
  });
});
