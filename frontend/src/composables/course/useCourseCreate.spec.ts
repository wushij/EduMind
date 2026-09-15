import { describe, it, expect, vi } from 'vitest';
import {
  buildRandomCourseCode,
  getSyllabusTemplateChapters,
  SYLLABUS_TEMPLATES
} from './useCourseCreate';

describe('buildRandomCourseCode', () => {
  it('generates code with expected prefix and numeric suffix', () => {
    const code = buildRandomCourseCode(0);
    expect(code).toMatch(/^(CS|AI|SE|DATA|EE|MATH)2026-\d{4}$/);
  });

  it('uses deterministic letter for fixed random value', () => {
    expect(buildRandomCourseCode(0)).toBe('CS2026-1000');
    expect(buildRandomCourseCode(0.99)).toBe('MATH2026-9910');
  });

  it('calls Math.random when no argument provided', () => {
    const randomSpy = vi.spyOn(Math, 'random').mockReturnValue(0.5);
    const code = buildRandomCourseCode();
    expect(randomSpy).toHaveBeenCalled();
    expect(code).toMatch(/2026-\d{4}$/);
    randomSpy.mockRestore();
  });
});

describe('getSyllabusTemplateChapters', () => {
  it('returns core template with 6 chapters', () => {
    const chapters = getSyllabusTemplateChapters('core');
    expect(chapters).toHaveLength(6);
    expect(chapters[0]).toContain('课程导论');
    expect(chapters).toEqual([...SYLLABUS_TEMPLATES.core]);
  });

  it('returns practical template with 4 stages', () => {
    const chapters = getSyllabusTemplateChapters('practical');
    expect(chapters).toHaveLength(4);
    expect(chapters[0]).toContain('第一阶段');
  });

  it('returns general template with 4 chapters', () => {
    const chapters = getSyllabusTemplateChapters('general');
    expect(chapters).toHaveLength(4);
    expect(chapters[3]).toContain('未来技术趋势');
  });

  it('returns a new array copy each time', () => {
    const first = getSyllabusTemplateChapters('core');
    first.push('extra');
    expect(getSyllabusTemplateChapters('core')).toHaveLength(6);
  });
});
