import { describe, it, expect, vi } from 'vitest';
import { buildRandomCourseCode } from './useCourseCreate';

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
